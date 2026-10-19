@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.agentmanager.missioncontrol.ui

import dev.agentmanager.core.domain.SessionId
import dev.agentmanager.core.testing.RecordingLogger
import dev.agentmanager.core.testing.T0
import dev.agentmanager.core.testing.TestClock
import dev.agentmanager.projects.api.A_PROJECT_DIR
import dev.agentmanager.projects.api.FakeProjectsApi
import dev.agentmanager.projects.api.aProject
import dev.agentmanager.sessions.api.FakeSessions
import dev.agentmanager.sessions.api.RuntimeProblem
import dev.agentmanager.sessions.api.SessionCommands
import dev.agentmanager.sessions.api.SessionError
import dev.agentmanager.sessions.api.SessionMonitor
import dev.agentmanager.sessions.api.SessionState
import dev.agentmanager.sessions.api.SessionsApi
import dev.agentmanager.sessions.api.SessionsWorld
import dev.agentmanager.sessions.api.WorldProject
import dev.agentmanager.sessions.api.WorldSession
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * TogglePreview, the open preview and `actions.canPreview` / `previewOpen` (FR-K2, AM-411). Sessions start at
 * T0 + their index, so rows are newest first. Time moves only by `advanceTimeBy`; the tick is one hour.
 */
internal class SessionPreviewControllerTest {
    private val project = aProject(displayName = "Agent Manager")
    private val log = RecordingLogger()
    private val a = SessionId("a")
    private val b = SessionId("b")

    private fun world(vararg sessions: WorldSession): FakeSessions =
        FakeSessions(
            SessionsWorld(
                projects = listOf(WorldProject(project.id, project.dir)),
                sessions = sessions.toList(),
                outputs = sessions.associate { it.id to "out-${it.id.value}" },
            ),
            TestClock(),
        )

    private fun session(
        id: String,
        index: Long,
        state: SessionState = SessionState.Working,
    ): WorldSession =
        WorldSession(
            id = SessionId(id),
            cwd = A_PROJECT_DIR,
            state = state,
            name = "name-$id",
            startedAt = T0.plusSeconds(index),
        )

    private fun TestScope.start(
        fake: FakeSessions,
        monitor: SessionMonitor = fake,
        commands: SessionCommands = fake,
    ): MissionControlController =
        MissionControlController(
            SessionsApi(monitor = monitor, commands = commands),
            FakeProjectsApi(initial = listOf(project)),
            MutableStateFlow(project.id),
            TestClock(),
            backgroundScope,
            log,
            1.hours,
        ).also { runCurrent() }

    private fun TestScope.select(
        controller: MissionControlController,
        id: SessionId,
    ) {
        controller.onEvent(MissionControlEvent.SelectSession(id))
        runCurrent()
    }

    private fun TestScope.send(
        controller: MissionControlController,
        intent: MissionControlIntent,
    ) {
        controller.onIntent(intent)
        runCurrent()
    }

    private fun TestScope.pass(duration: Duration) {
        advanceTimeBy(duration)
        runCurrent()
    }

    private fun MissionControlController.preview(): PreviewUi? = state.value.preview

    private fun text(id: String): PreviewUi = PreviewUi(SessionId(id), "name-$id", PreviewContent.Text("out-$id"))

    @Test
    fun `toggle without a selection opens nothing`() =
        runTest {
            val fake = world(session("a", 1))
            val controller = start(fake)

            send(controller, MissionControlIntent.TogglePreview)
            pass(10.seconds)

            controller.preview().shouldBeNull()
            controller.actions.value.previewOpen shouldBe false
            fake.outputReads.shouldBeEmpty()
        }

    @Test
    fun `toggle opens the preview of the selected session`() =
        runTest {
            val fake = world(session("a", 1), session("b", 2))
            val controller = start(fake)
            select(controller, a)

            send(controller, MissionControlIntent.TogglePreview)

            controller.preview() shouldBe text("a")
            controller.actions.value.previewOpen shouldBe true
            fake.outputReads shouldBe listOf(a)
            fake.calls.shouldBeEmpty()
        }

    @Test
    fun `the preview shows loading until the first read ends`() =
        runTest {
            val fake = world(session("a", 1))
            val gated = GatedOutput(fake)
            val controller = start(fake, gated)
            select(controller, a)
            gated.hold()

            send(controller, MissionControlIntent.TogglePreview)

            controller.preview() shouldBe PreviewUi(a, "name-a", PreviewContent.Loading)

            gated.release()
            runCurrent()

            controller.preview() shouldBe text("a")
        }

    @Test
    fun `an empty output shows empty`() =
        runTest {
            val fake = world(session("a", 1))
            fake.setOutput(a, "  \n")
            val controller = start(fake)
            select(controller, a)

            send(controller, MissionControlIntent.TogglePreview)

            controller.preview() shouldBe PreviewUi(a, "name-a", PreviewContent.Empty)
        }

    @Test
    fun `toggle again closes it`() =
        runTest {
            val fake = world(session("a", 1))
            val controller = start(fake)
            select(controller, a)
            send(controller, MissionControlIntent.TogglePreview)

            send(controller, MissionControlIntent.TogglePreview)

            controller.preview().shouldBeNull()
            controller.actions.value.previewOpen shouldBe false
            controller.actions.value.canPreview shouldBe true
        }

    @Test
    fun `the close button toggles the preview too`() =
        runTest {
            val fake = world(session("a", 1))
            val controller = start(fake)
            select(controller, a)
            send(controller, MissionControlIntent.TogglePreview)

            controller.onEvent(MissionControlEvent.TogglePreview)
            runCurrent()

            controller.preview().shouldBeNull()
        }

    @Test
    fun `next session loads the output of the new session`() =
        runTest {
            val fake = world(session("a", 1), session("b", 2))
            val controller = start(fake)
            select(controller, b)
            send(controller, MissionControlIntent.TogglePreview)
            controller.preview() shouldBe text("b")

            send(controller, MissionControlIntent.NextSession)

            controller.preview() shouldBe text("a")
            fake.outputReads shouldBe listOf(b, a)
        }

    @Test
    fun `the open preview reads again every 3 seconds and keeps the old text meanwhile`() =
        runTest {
            val fake = world(session("a", 1))
            val gated = GatedOutput(fake)
            val controller = start(fake, gated)
            select(controller, a)
            send(controller, MissionControlIntent.TogglePreview)
            fake.setOutput(a, "newer")

            pass(2900.milliseconds)
            controller.preview() shouldBe text("a")
            gated.started shouldBe listOf(a)

            gated.hold()
            pass(100.milliseconds)
            gated.started shouldBe listOf(a, a)
            // The second read is still running: the old text stays, no loading flicker.
            controller.preview() shouldBe text("a")

            gated.release()
            runCurrent()
            controller.preview() shouldBe PreviewUi(a, "name-a", PreviewContent.Text("newer"))

            pass(3.seconds)
            gated.started shouldBe listOf(a, a, a)
        }

    @Test
    fun `a failed reread replaces the text`() =
        runTest {
            val fake = world(session("a", 1))
            val controller = start(fake)
            select(controller, a)
            send(controller, MissionControlIntent.TogglePreview)
            fake.failNextOutput(SessionError.Runtime(RuntimeProblem.TIMEOUT))

            pass(3.seconds)

            controller.preview() shouldBe
                PreviewUi(a, "name-a", PreviewContent.Failed(UiText("action.reason.runtime.TIMEOUT")))

            pass(3.seconds)

            controller.preview() shouldBe text("a")
        }

    @Test
    fun `closing stops the rereads`() =
        runTest {
            val fake = world(session("a", 1))
            val controller = start(fake)
            select(controller, a)
            send(controller, MissionControlIntent.TogglePreview)
            send(controller, MissionControlIntent.TogglePreview)

            pass(30.seconds)

            fake.outputReads shouldBe listOf(a)
        }

    @Test
    fun `losing the selected session closes the preview`() =
        runTest {
            val fake = world(session("a", 1, SessionState.Done))
            val controller = start(fake)
            select(controller, a)
            send(controller, MissionControlIntent.TogglePreview)

            fake.remove(a)
            runCurrent()

            controller.preview().shouldBeNull()
            controller.actions.value.previewOpen shouldBe false
            pass(30.seconds)
            fake.outputReads shouldBe listOf(a)
        }

    @Test
    fun `hiding the selected session by the filter closes the preview`() =
        runTest {
            val fake = world(session("a", 1))
            val controller = start(fake)
            select(controller, a)
            send(controller, MissionControlIntent.TogglePreview)

            controller.onEvent(MissionControlEvent.SelectFilter(SessionFilter.FINISHED))
            runCurrent()

            controller.preview().shouldBeNull()
            controller.actions.value.previewOpen shouldBe false
        }

    @Test
    fun `a slow read of the previous session is dropped`() =
        runTest {
            val fake = world(session("a", 1), session("b", 2))
            val gated = GatedOutput(fake)
            val controller = start(fake, gated)
            select(controller, b)
            gated.hold()
            send(controller, MissionControlIntent.TogglePreview)
            controller.preview() shouldBe PreviewUi(b, "name-b", PreviewContent.Loading)

            send(controller, MissionControlIntent.NextSession)
            controller.preview() shouldBe PreviewUi(a, "name-a", PreviewContent.Loading)
            gated.release()
            runCurrent()

            controller.preview() shouldBe text("a")
            gated.started shouldBe listOf(b, a)
        }

    @Test
    fun `the toggle event is ignored while the dialog is open`() =
        runTest {
            val fake = world(session("a", 1, SessionState.Done))
            val controller = start(fake)
            select(controller, a)
            send(controller, MissionControlIntent.RemoveSelected)

            controller.onEvent(MissionControlEvent.TogglePreview)
            runCurrent()

            controller.preview().shouldBeNull()
            fake.outputReads.shouldBeEmpty()

            send(controller, MissionControlIntent.TogglePreview)
            controller.onEvent(MissionControlEvent.Dismiss)
            runCurrent()
            controller.onEvent(MissionControlEvent.TogglePreview)
            runCurrent()
            controller.preview() shouldBe text("a")
            controller.onEvent(MissionControlEvent.TogglePreview)
            runCurrent()
            controller.preview().shouldBeNull()
        }

    @Test
    fun `close and open in one step leaves an open preview with content`() =
        runTest {
            val fake = world(session("a", 1))
            val gated = GatedOutput(fake)
            val controller = start(fake, gated)
            select(controller, a)
            send(controller, MissionControlIntent.TogglePreview)
            gated.hold()

            // No runCurrent between the toggles: the key goes a -> null -> a within one dispatcher step.
            controller.onIntent(MissionControlIntent.TogglePreview)
            controller.onIntent(MissionControlIntent.TogglePreview)
            runCurrent()

            controller.actions.value.previewOpen shouldBe true
            val shown = controller.preview().shouldNotBeNull()
            shown.id shouldBe a
            shown.content.shouldBeInstanceOf<PreviewContent>()
            gated.release()
            runCurrent()
            controller.preview() shouldBe text("a")
        }

    @Test
    fun `toggle is ignored while the dialog is open`() =
        runTest {
            val fake = world(session("a", 1, SessionState.Done))
            val controller = start(fake)
            select(controller, a)
            send(controller, MissionControlIntent.RemoveSelected)
            controller.state.value.dialog
                .shouldBeInstanceOf<SessionDialog.ConfirmRemove>()

            send(controller, MissionControlIntent.TogglePreview)

            controller.preview().shouldBeNull()
            fake.outputReads.shouldBeEmpty()
        }

    @Test
    fun `toggle does not close an open preview while the dialog is open`() =
        runTest {
            val fake = world(session("a", 1, SessionState.Done))
            val controller = start(fake)
            select(controller, a)
            send(controller, MissionControlIntent.TogglePreview)
            send(controller, MissionControlIntent.RemoveSelected)

            send(controller, MissionControlIntent.TogglePreview)

            controller.preview() shouldBe text("a")
        }

    @Test
    fun `actions report canPreview and previewOpen`() =
        runTest {
            val fake = world(session("a", 1), session("b", 2, SessionState.Done))
            val controller = start(fake)
            controller.actions.value.canPreview shouldBe false
            controller.actions.value.previewOpen shouldBe false

            select(controller, a)
            controller.actions.value.canPreview shouldBe true
            controller.actions.value.previewOpen shouldBe false

            send(controller, MissionControlIntent.TogglePreview)
            controller.actions.value.canPreview shouldBe true
            controller.actions.value.previewOpen shouldBe true

            send(controller, MissionControlIntent.TogglePreview)
            controller.onEvent(MissionControlEvent.SelectFilter(SessionFilter.FINISHED))
            runCurrent()
            controller.actions.value.canPreview shouldBe false

            select(controller, b)
            controller.actions.value.canPreview shouldBe true
            send(controller, MissionControlIntent.RemoveSelected)
            controller.actions.value.canPreview shouldBe false
            controller.actions.value.dialogOpen shouldBe true
        }

    @Test
    fun `canPreview stays on while an action runs`() =
        runTest {
            val fake = world(session("a", 1))
            val gated = GatedCommands(fake)
            val controller = start(fake, commands = gated)
            select(controller, a)

            send(controller, MissionControlIntent.StopSelected)

            controller.actions.value shouldBe
                SessionActionsState(canStop = false, canRespawn = false, canPreview = true)
            send(controller, MissionControlIntent.TogglePreview)
            controller.preview() shouldBe text("a")
            controller.actions.value.canPreview shouldBe true
        }

    @Test
    fun `preview logs nothing`() =
        runTest {
            val fake = world(session("a", 1), session("b", 2))
            val controller = start(fake)
            select(controller, a)
            send(controller, MissionControlIntent.TogglePreview)
            fake.failNextOutput(SessionError.Runtime(RuntimeProblem.COMMAND_FAILED))
            pass(3.seconds)
            send(controller, MissionControlIntent.NextSession)
            send(controller, MissionControlIntent.TogglePreview)

            log.entries.shouldBeEmpty()
        }
}
