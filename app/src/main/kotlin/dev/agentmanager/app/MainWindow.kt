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
    modules: AppModules,
    keys: WindowKeys,
) {
    val projects = modules.projects
    val sessions = modules.sessions
    val registry = modules.registry
    val scope = rememberCoroutineScope()
    val controller =
        remember {
            val picker = AwtDirectoryPicker(window, core.dispatchers.main)
            ProjectsController(projects, modules.inspector, sessions, picker, scope, core.logger)
        }
    val missionControl =
        remember { MissionControlController(sessions, projects, controller.selection, core.clock, scope, core.logger) }
    val palette = remember { CommandPaletteController(registry, scope, core.logger) }
    DisposableEffect(Unit) {
        val registration = registry.register(AppCommands(projects, controller, missionControl, palette, scope))
        onDispose { registration.close() }
    }
    DisposableEffect(Unit) {
        val handle = keys.attach(palette::onKeyEvent)
        onDispose { handle.close() }
    }
    AppMenu(controller, palette, missionControl)
    AgentManagerApp(projects = controller, missionControl = missionControl, palette = palette)
}
