package dev.agentmanager.app.terminalspike

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.agentmanager.app.Slf4jLogger
import dev.agentmanager.core.domain.AppDispatchers
import dev.agentmanager.core.domain.Logger
import dev.agentmanager.core.domain.Outcome
import dev.agentmanager.core.domain.error
import dev.agentmanager.terminal.api.TerminalError
import dev.agentmanager.terminal.api.TerminalSession
import dev.agentmanager.terminal.impl.terminalModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.swing.Swing
import org.slf4j.LoggerFactory
import java.awt.KeyboardFocusManager
import java.nio.file.Path
import javax.swing.JComponent

private val logger: Logger = Slf4jLogger(LoggerFactory.getLogger("terminal-spike"))

/**
 * AM-501 spike window on `terminalModule` (AM-502): JediTerm in a `SwingPanel` next to Compose controls
 * (docs/plans/AM-501.md, manual observations F1-B1). The optional argument is the shell's working directory
 * (default: the home directory). AM-503 removes this window.
 */
fun main(args: Array<String>) {
    val workDir = Path.of(args.firstOrNull() ?: System.getProperty("user.home"))
    val dispatchers = AppDispatchers(main = Dispatchers.Swing, io = Dispatchers.IO, default = Dispatchers.Default)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val terminals = terminalModule(dispatchers, scope, logger, Path.of(System.getenv("SHELL") ?: "/bin/zsh"))
    // Cmd-Q bypasses onCloseRequest; the hook still kills every shell tree.
    Runtime.getRuntime().addShutdownHook(Thread { terminals.close() })
    application {
        val opened by produceState<Outcome<TerminalSession, TerminalError>?>(null) {
            value =
                terminals.openShell(workDir)
        }
        val result = opened ?: return@application
        when (result) {
            is Outcome.Err -> {
                LaunchedEffect(Unit) {
                    logger.error("Terminal spike: cannot open", "reason" to result.error.toString())
                    exitApplication()
                }
            }

            is Outcome.Ok -> {
                Window(
                    onCloseRequest = {
                        terminals.close()
                        exitApplication()
                    },
                    title = "AM-501 terminal spike",
                    state = rememberWindowState(size = DpSize(1100.dp, 700.dp)),
                ) {
                    SpikeContent(result.value.component())
                }
            }
        }
    }
}

@Composable
private fun SpikeContent(terminal: JComponent) {
    var lastKey by remember { mutableStateOf("-") }
    var paletteOpen by remember { mutableStateOf(false) }
    var capture by remember { mutableStateOf(false) }
    var fullWidth by remember { mutableStateOf(false) }
    var terminalShown by remember { mutableStateOf(true) }
    var detached by remember { mutableStateOf(false) }
    val actions =
        SpikeActions(
            onCapture = { capture = it },
            onPalette = { paletteOpen = true },
            onToggleTerminal = { terminalShown = !terminalShown },
            onFullWidth = { fullWidth = !fullWidth },
            onDetach = { detached = true },
        )
    ShortcutCapture(capture) { paletteOpen = true }
    DetachedFrame(detached, terminal) { detached = false }
    val keys =
        Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            val down = event.type == KeyEventType.KeyDown
            if (down) lastKey = event.key.toString()
            val openPalette = down && event.isMetaPressed && event.key == Key.K
            if (openPalette) paletteOpen = true
            openPalette
        }
    Row(keys) {
        if (!fullWidth) ControlColumn(lastKey, capture, actions)
        TerminalArea(terminal, terminalShown && !detached, paletteOpen) {
            paletteOpen = false
            terminal.requestFocusInWindow()
        }
    }
}

@Composable
private fun ShortcutCapture(
    enabled: Boolean,
    onPalette: () -> Unit,
) {
    DisposableEffect(enabled) {
        val dispatcher = metaKDispatcher(onPalette)
        val manager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        if (enabled) manager.addKeyEventDispatcher(dispatcher)
        onDispose { manager.removeKeyEventDispatcher(dispatcher) }
    }
}
