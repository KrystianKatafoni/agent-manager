package dev.agentmanager.app

import dev.agentmanager.commandpalette.api.Command
import dev.agentmanager.commandpalette.api.CommandGroup
import dev.agentmanager.missioncontrol.ui.MissionControlIntent
import dev.agentmanager.projects.api.A_PROJECT_DIR
import dev.agentmanager.projects.api.Project
import dev.agentmanager.projects.api.aProject
import dev.agentmanager.projects.ui.ProjectsMenuState
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** The pure projection of the project list and menu state onto session and navigation commands (AM-409). */
class SessionCommandsTest {
    private val agent = aProject(id = "project0001", dir = A_PROJECT_DIR, displayName = "Agent Manager")

    private val idle = ProjectsMenuState(canAdd = true, canRemove = true, canRename = true, dialogOpen = false)
    private val dialog = ProjectsMenuState(canAdd = false, canRemove = false, canRename = false, dialogOpen = true)

    private val intents = mutableListOf<MissionControlIntent>()
    private val opened = mutableListOf<CommandGroup>()

    private fun commands(
        list: List<Project> = listOf(agent),
        menu: ProjectsMenuState = idle,
    ): List<Command> = sessionCommands(list, menu, { intents += it }, { opened += it })

    private fun List<Command>.ids(): List<String> = map { it.id.value }

    private fun List<Command>.byId(id: String): Command = single { it.id.value == id }

    @Test
    fun `j and k send next and previous session`() =
        runTest {
            val result = commands()

            result.byId("sessions.next").title shouldBe "Następna sesja"
            result.byId("sessions.next").group shouldBe CommandGroup.SESSION
            result.byId("sessions.next").shortcut shouldBe AppShortcuts.NEXT_SESSION
            result.byId("sessions.previous").title shouldBe "Poprzednia sesja"
            result.byId("sessions.previous").group shouldBe CommandGroup.SESSION
            result.byId("sessions.previous").shortcut shouldBe AppShortcuts.PREVIOUS_SESSION

            result.byId("sessions.next").run()
            result.byId("sessions.previous").run()

            intents shouldBe listOf(MissionControlIntent.NextSession, MissionControlIntent.PreviousSession)
        }

    @Test
    fun `j and k are offered without any project`() {
        commands(list = emptyList()).ids() shouldContain "sessions.next"
        commands(list = emptyList()).ids() shouldContain "sessions.previous"
    }

    @Test
    fun `g p opens the palette limited to go to project`() =
        runTest {
            val switch = commands().byId("projects.switch")

            switch.title shouldBe "Przełącz projekt…"
            switch.keywords shouldBe listOf("switch", "projekt")
            switch.shortcut shouldBe AppShortcuts.SWITCH_PROJECT

            switch.run()

            opened shouldBe listOf(CommandGroup.GO_TO_PROJECT)
            intents.shouldBeEmpty()
        }

    @Test
    fun `switch project is not in the go to project group`() {
        // Otherwise the palette opened by g p would list the command that opened it.
        commands().byId("projects.switch").group shouldBe CommandGroup.PROJECT
    }

    @Test
    fun `switch project needs a project`() {
        commands(list = emptyList()).ids() shouldNotContain "projects.switch"
        commands(list = listOf(agent)).ids() shouldContain "projects.switch"
    }

    @Test
    fun `an open dialog gives no session commands`() {
        commands(menu = dialog).shouldBeEmpty()
    }

    @Test
    fun `command ids are unique and keep clear of project commands`() {
        val ids = commands(listOf(agent, aProject(id = "project0002"))).ids()

        ids.toSet().size shouldBe ids.size
        ids.none { it.startsWith("projects.goto.") || it == "projects.add" } shouldBe true
    }

    @Test
    fun `only the three keyboard commands carry a shortcut`() {
        commands().filter { it.shortcut != null }.ids().toSet() shouldBe
            setOf("sessions.next", "sessions.previous", "projects.switch")
    }
}
