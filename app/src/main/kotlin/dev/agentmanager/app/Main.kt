package dev.agentmanager.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.agentmanager.agentruntime.impl.agentRuntimeModule
import dev.agentmanager.core.domain.info
import dev.agentmanager.missioncontrol.ui.MissionControlController
import dev.agentmanager.projects.impl.projectInspectorModule
import dev.agentmanager.projects.impl.projectsModule
import dev.agentmanager.projects.ui.ProjectsController
import dev.agentmanager.projects.ui.ProjectsIntent
import dev.agentmanager.sessions.impl.sessionsModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

/**
 * Entry point: builds the dependency graph once, then opens the main window. The project list is read before
 * the window opens: `main` is the one place allowed to block (ENG-C3).
 */
@Suppress("ForbiddenMethodCall")
fun main() {
    // Before the first use of AWT: FileDialog then chooses folders, natively on macOS (AwtDirectoryPicker).
    System.setProperty("apple.awt.fileDialogForDirectories", "true")
    val core = productionCoreDeps()
    core.logger.info("app start")
    val runtime = agentRuntimeModule(core.process, core.clock, core.logger, core.homeDir)
    val projects = runBlocking { projectsModule(core.storage, core.dispatchers.io, core.logger) }
    val inspector = projectInspectorModule(projects, runtime, core.process, core.dispatchers.io, core.logger)
    // Lives as long as the app, not the window (AM-405 risks).
    val sessionsScope = CoroutineScope(SupervisorJob() + core.dispatchers.io)
    val sessions = sessionsModule(runtime, projects, sessionsScope, core.clock, core.logger)
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = Strings["app.title"],
            state = rememberWindowState(size = DpSize(width = 1280.dp, height = 800.dp)),
        ) {
            val scope = rememberCoroutineScope()
            val controller =
                remember {
                    val picker = AwtDirectoryPicker(window, core.dispatchers.main)
                    ProjectsController(projects, inspector, runtime, picker, scope, core.logger)
                }
            val missionControl =
                remember {
                    MissionControlController(sessions.monitor, projects, controller.selection, core.clock, scope)
                }
            ProjectsMenu(controller)
            AgentManagerApp(projects = controller, missionControl = missionControl)
        }
    }
}

/** "Add project…" (⌘⇧O) and "Remove project" (⌘⌫); both disabled while a dialog is open, see `controller.menu`. */
@Composable
private fun FrameWindowScope.ProjectsMenu(controller: ProjectsController) {
    val menu by controller.menu.collectAsState()
    MenuBar {
        Menu(Strings["menu.projects"]) {
            Item(
                text = Strings["menu.addProject"],
                enabled = menu.canAdd,
                shortcut = KeyShortcut(Key.O, meta = true, shift = true),
                onClick = { controller.onIntent(ProjectsIntent.AddRequested) },
            )
            Item(
                text = Strings["menu.removeProject"],
                enabled = menu.canRemove,
                shortcut = KeyShortcut(Key.Backspace, meta = true),
                onClick = { controller.onIntent(ProjectsIntent.RemoveSelectedRequested) },
            )
        }
    }
}
