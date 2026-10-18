package dev.agentmanager.projects.ui

import dev.agentmanager.agentruntime.api.RuntimePhase
import dev.agentmanager.agentruntime.api.RuntimeSession
import dev.agentmanager.agentruntime.api.aRuntimeSession
import dev.agentmanager.core.domain.SessionId
import dev.agentmanager.core.testing.aSessionId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * Sessions stopped by "stop sessions and remove" are WORKING and BLOCKED, the same rule as the inspector
 * count in `projects:impl` (AM-304 plan, decision 1; AM-303 question 2).
 */
class ActiveSessionsTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("listings")
    fun `only working and blocked sessions are active, in list order`(
        case: String,
        sessions: List<RuntimeSession>,
        expected: List<SessionId>,
    ) {
        withClue(case) { ActiveSessions.of(sessions) shouldBe expected }
    }

    companion object {
        private fun inPhase(phase: RuntimePhase): RuntimeSession =
            aRuntimeSession(id = aSessionId(phase.name.lowercase()), phase = phase)

        @JvmStatic
        fun listings(): List<Arguments> =
            listOf(
                arguments(
                    "one session in every phase",
                    RuntimePhase.entries.map(::inPhase),
                    listOf(aSessionId("working"), aSessionId("blocked")),
                ),
                arguments(
                    "blocked before working keeps the order",
                    listOf(inPhase(RuntimePhase.BLOCKED), inPhase(RuntimePhase.DONE), inPhase(RuntimePhase.WORKING)),
                    listOf(aSessionId("blocked"), aSessionId("working")),
                ),
                arguments(
                    "only ended sessions",
                    listOf(RuntimePhase.DONE, RuntimePhase.FAILED, RuntimePhase.STOPPED).map(::inPhase),
                    emptyList<SessionId>(),
                ),
                arguments("no sessions", emptyList<RuntimeSession>(), emptyList<SessionId>()),
            )
    }
}
