@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.agentmanager.sessions.impl

import dev.agentmanager.agentruntime.api.A_PROJECT_DIR
import dev.agentmanager.agentruntime.api.RuntimeError
import dev.agentmanager.agentruntime.api.RuntimePhase
import dev.agentmanager.agentruntime.api.RuntimeSession
import dev.agentmanager.agentruntime.api.aRuntimeSession
import dev.agentmanager.core.domain.Outcome
import dev.agentmanager.core.domain.ProjectId
import dev.agentmanager.core.domain.SessionId
import dev.agentmanager.core.testing.T0
import dev.agentmanager.core.testing.TestClock
import dev.agentmanager.sessions.api.RuntimeProblem
import dev.agentmanager.sessions.api.SessionEvent
import dev.agentmanager.sessions.api.SessionState
import dev.agentmanager.sessions.api.SessionsSnapshot
import dev.agentmanager.sessions.api.SnapshotStatus
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

/** The single owner of the published snapshot and events (AM-403 plan, `SessionsState`). */
class SessionsStateTest {
    private val clock = TestClock()
    private val projects: Map<ProjectId, Path> = mapOf(ProjectId("agent-manager") to A_PROJECT_DIR)

    @Test
    fun `starts with the initial snapshot`() {
        SessionsState(clock).snapshot.value shouldBe SessionsSnapshot.Initial
    }

    @Test
    fun `publishes the snapshot before the events`() =
        runTest {
            val state = SessionsState(clock)
            state.publish(ok(aRuntimeSession()), projects)
            val seen =
                mutableListOf<Pair<SessionEvent, SessionsSnapshot>>().also { seen ->
                    backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        state.events.collect { seen += it to state.snapshot.value }
                    }
                }

            clock.advanceBy(2.seconds)
            val reduction = state.publish(ok(aRuntimeSession(phase = RuntimePhase.DONE)), projects)
            runCurrent()

            val (event, snapshotAtEvent) = seen.single()
            event.shouldBeInstanceOf<SessionEvent.TurnFinished>()
            snapshotAtEvent.sessions shouldContain event.session
            state.snapshot.value shouldBe reduction.snapshot
            reduction.events shouldBe listOf(event)
        }

    @Test
    fun `failed listing publishes no events`() =
        runTest {
            val state = SessionsState(clock)
            state.publish(ok(aRuntimeSession()), projects)
            val events = record(state.events)

            clock.advanceBy(2.seconds)
            val reduction = state.publish(Outcome.Err(RuntimeError.Timeout), projects)
            runCurrent()

            events.shouldBeEmpty()
            reduction.events.shouldBeEmpty()
            state.snapshot.value.status shouldBe
                SnapshotStatus.Stale(lastFreshAt = T0, problem = RuntimeProblem.TIMEOUT)
            state.snapshot.value.sessions
                .single()
                .state shouldBe SessionState.Working
        }

    @Test
    fun `events are not replayed to late collectors`() =
        runTest {
            val state = SessionsState(clock)
            state.publish(ok(aRuntimeSession()), projects)
            clock.advanceBy(2.seconds)
            state.publish(ok(aRuntimeSession(phase = RuntimePhase.FAILED)), projects)

            val late = record(state.events)
            runCurrent()

            late.shouldBeEmpty()
            state.snapshot.value.sessions
                .single()
                .state shouldBe SessionState.Failed
        }

    @Test
    fun `every event of one listing reaches a collector`() =
        runTest {
            val state = SessionsState(clock)
            val ids = (1..10).map { "aaaa%04d".format(Locale.ROOT, it) }
            state.publish(ok(*ids.map { runtimeSession(it) }.toTypedArray()), projects)
            val events = record(state.events)

            clock.advanceBy(2.seconds)
            state.publish(ok(*ids.map { runtimeSession(it, RuntimePhase.DONE) }.toTypedArray()), projects)
            runCurrent()

            events shouldHaveSize ids.size
        }

    @Test
    fun `fresh time comes from the clock`() =
        runTest {
            val state = SessionsState(clock)
            clock.advanceBy(5.seconds)

            state.publish(ok(aRuntimeSession()), projects)

            state.snapshot.value.status shouldBe SnapshotStatus.Fresh(T0.plusSeconds(5))
        }

    private fun ok(vararg sessions: RuntimeSession): Outcome<List<RuntimeSession>, RuntimeError> =
        Outcome.Ok(sessions.toList())

    private fun runtimeSession(
        id: String,
        phase: RuntimePhase = RuntimePhase.WORKING,
    ): RuntimeSession = aRuntimeSession(id = SessionId(id), phase = phase)
}
