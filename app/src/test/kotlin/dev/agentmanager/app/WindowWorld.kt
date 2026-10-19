package dev.agentmanager.app

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.waitUntilExactlyOneExists
import dev.agentmanager.commandpalette.api.FakeCommandRegistry
import dev.agentmanager.commandpalette.ui.CommandPaletteController
import dev.agentmanager.core.domain.NoOpLogger
import dev.agentmanager.core.testing.TestClock
import dev.agentmanager.missioncontrol.ui.MissionControlController
import dev.agentmanager.projects.api.FakeProjectInspector
import dev.agentmanager.projects.api.FakeProjectsApi
import dev.agentmanager.projects.ui.DirectoryPicker
import dev.agentmanager.projects.ui.ProjectsController
import dev.agentmanager.sessions.api.FakeSessions
import dev.agentmanager.sessions.api.SessionsApi
import kotlin.time.Duration.Companion.hours

/**
 * The window of the app on fakes: sidebar, Mission Control and the palette over [AppCommands], registered in a
 * [FakeCommandRegistry] the way `Main.kt` registers it in the real one. `MenuBar` is not part of it
 * (it does not work in `runComposeUiTest`), so tests open the palette with [palette].
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showWindow(projects: FakeProjectsApi): CommandPaletteController {
    val built = mutableListOf<CommandPaletteController>()
    setContent {
        val scope = rememberCoroutineScope()
        val sessions = remember { FakeSessions() }
        val controller =
            remember {
                ProjectsController(
                    projects = projects,
                    inspector = FakeProjectInspector(projects = projects),
                    sessions = SessionsApi(monitor = sessions, commands = sessions),
                    picker = DirectoryPicker { null },
                    scope = scope,
                    logger = NoOpLogger,
                )
            }
        val missionControl =
            remember { MissionControlController(sessions, projects, controller.selection, TestClock(), scope, 1.hours) }
        val palette =
            remember {
                val registry = FakeCommandRegistry(scope)
                registry.register(AppCommands(projects, controller, scope))
                CommandPaletteController(registry, scope, NoOpLogger).also { built += it }
            }
        AgentManagerApp(projects = controller, missionControl = missionControl, palette = palette)
    }
    waitUntil { built.isNotEmpty() }
    return built.single()
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.await(tag: String): SemanticsNodeInteraction {
    waitUntilExactlyOneExists(hasTestTag(tag))
    return onNodeWithTag(tag)
}
