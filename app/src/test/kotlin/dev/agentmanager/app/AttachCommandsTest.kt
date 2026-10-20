package dev.agentmanager.app

import dev.agentmanager.commandpalette.api.CommandGroup
import dev.agentmanager.missioncontrol.ui.SessionActionsState
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** The pure projection of `canAttach` and the dialogs onto the attach command (FR-K2, AM-505). */
class AttachCommandsTest {
    private val none = SessionActionsState(canStop = false, canRespawn = false)
    private val attachable = SessionActionsState(canStop = true, canRespawn = false, canAttach = true)
    private var attached = 0

    private fun commands(
        actions: SessionActionsState = attachable,
        dialogOpen: Boolean = false,
    ) = attachCommands(dialogOpen, actions) { attached++ }

    @Test
    fun `attach is offered only when canAttach`() {
        commands(none).shouldBeEmpty()

        val offered = commands(attachable).single()

        offered.id.value shouldBe "sessions.attach"
        offered.title shouldBe "Attach sesji w terminalu"
        offered.group shouldBe CommandGroup.SESSION
        offered.keywords shouldBe listOf("attach", "terminal", "podłącz")
    }

    @Test
    fun `attach is bound to Enter`() {
        commands().single().shortcut shouldBe AppShortcuts.ATTACH_SESSION
    }

    @Test
    fun `no attach while a projects dialog is open`() {
        commands(attachable, dialogOpen = true).shouldBeEmpty()
    }

    @Test
    fun `no attach while a mission control dialog is open`() {
        commands(attachable.copy(dialogOpen = true)).shouldBeEmpty()
    }

    @Test
    fun `attach runs the attacher`() =
        runTest {
            commands().single().run()

            attached shouldBe 1
        }
}
