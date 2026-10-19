package dev.agentmanager.app

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.agentmanager.agentruntime.impl.agentRuntimeModule
import dev.agentmanager.commandpalette.impl.commandPaletteModule
import dev.agentmanager.core.domain.info
import dev.agentmanager.projects.impl.projectInspectorModule
import dev.agentmanager.projects.impl.projectsModule
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
    // The registry only merges lists on Default; commands run in the window scope on Main (see MainWindow).
    val commandsScope = CoroutineScope(SupervisorJob() + core.dispatchers.default)
    val registry = commandPaletteModule(commandsScope, core.logger)
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = Strings["app.title"],
            state = rememberWindowState(size = DpSize(width = 1280.dp, height = 800.dp)),
        ) {
            MainWindow(core, projects, sessions, inspector, registry)
        }
    }
}
