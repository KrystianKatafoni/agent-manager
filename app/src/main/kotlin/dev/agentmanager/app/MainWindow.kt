package dev.agentmanager.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.FrameWindowScope
import dev.agentmanager.commandpalette.api.CommandRegistry
import dev.agentmanager.commandpalette.ui.CommandPaletteController
import dev.agentmanager.missioncontrol.ui.MissionControlController
import dev.agentmanager.projects.api.ProjectInspector
import dev.agentmanager.projects.api.ProjectsApi
import dev.agentmanager.projects.ui.ProjectsController
import dev.agentmanager.sessions.api.SessionsApi

/** The content and menu of the main window; its controllers live as long as the window (AM-408). */
@Composable
internal fun FrameWindowScope.MainWindow(
    core: CoreDeps,
    projects: ProjectsApi,
    sessions: SessionsApi,
    inspector: ProjectInspector,
    registry: CommandRegistry,
) {
    val scope = rememberCoroutineScope()
    val controller =
        remember {
            val picker = AwtDirectoryPicker(window, core.dispatchers.main)
            ProjectsController(projects, inspector, sessions, picker, scope, core.logger)
        }
    val missionControl =
        remember { MissionControlController(sessions.monitor, projects, controller.selection, core.clock, scope) }
    val palette = remember { CommandPaletteController(registry, scope, core.logger) }
    DisposableEffect(Unit) {
        val registration = registry.register(AppCommands(projects, controller, scope))
        onDispose { registration.close() }
    }
    AppMenu(controller, palette)
    AgentManagerApp(projects = controller, missionControl = missionControl, palette = palette)
}
