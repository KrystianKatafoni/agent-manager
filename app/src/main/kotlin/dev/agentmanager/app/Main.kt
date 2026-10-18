package dev.agentmanager.app

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.agentmanager.core.domain.info

/** Entry point: builds the dependency graph once, then opens the main window. */
fun main() {
    val core = productionCoreDeps()
    core.logger.info("app start")
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = Strings["app.title"],
            state = rememberWindowState(size = DpSize(width = 1280.dp, height = 800.dp)),
        ) {
            AgentManagerApp()
        }
    }
}
