package dev.agentmanager.app.terminalspike

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import java.awt.BorderLayout
import java.awt.KeyEventDispatcher
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JComponent
import javax.swing.JFrame

private const val CONTROL_WIDTH = 260
private const val DETACHED_WIDTH = 900
private const val DETACHED_HEIGHT = 600
private const val PALE_YELLOW = 0xFFFFFFE0

/** Variant B of F3: sees Command-K before any focused Swing component does. */
internal fun metaKDispatcher(onPalette: () -> Unit): KeyEventDispatcher =
    KeyEventDispatcher { event ->
        val hit = event.id == KeyEvent.KEY_PRESSED && event.isMetaDown && event.keyCode == KeyEvent.VK_K
        if (hit) onPalette()
        hit
    }

@Composable
private fun SpikeButton(
    label: String,
    onClick: () -> Unit,
) {
    BasicText(label, Modifier.background(Color.LightGray).clickable(onClick = onClick).padding(8.dp))
}

@Composable
internal fun ControlColumn(
    lastKey: String,
    capture: Boolean,
    actions: SpikeActions,
) {
    var text by remember { mutableStateOf("") }
    Column(Modifier.width(CONTROL_WIDTH.dp).fillMaxHeight().padding(8.dp), Arrangement.spacedBy(8.dp)) {
        BasicText("F2: Compose text field")
        BasicTextField(text, { text = it }, Modifier.background(Color(PALE_YELLOW)).padding(6.dp).controlFieldWidth())
        BasicText("Last key seen by Compose: $lastKey")
        SpikeButton("Palette (Cmd-K mock)", actions.onPalette)
        SpikeButton("Hide/show terminal", actions.onToggleTerminal)
        SpikeButton("Full width", actions.onFullWidth)
        SpikeButton("Open in separate window", actions.onDetach)
        SpikeButton("Capture app shortcuts: $capture") { actions.onCapture(!capture) }
    }
}

private fun Modifier.controlFieldWidth(): Modifier = this.width(CONTROL_WIDTH.dp - 16.dp)

/** Callbacks of the control column, grouped to keep signatures short. */
internal class SpikeActions(
    val onCapture: (Boolean) -> Unit,
    val onPalette: () -> Unit,
    val onToggleTerminal: () -> Unit,
    val onFullWidth: () -> Unit,
    val onDetach: () -> Unit,
)

@Composable
internal fun TerminalArea(
    terminal: JComponent,
    shown: Boolean,
    paletteOpen: Boolean,
    onPaletteClosed: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        if (shown) SwingPanel(factory = { terminal }, modifier = Modifier.fillMaxSize())
        if (paletteOpen) Palette(onPaletteClosed)
    }
}

/** F4 and F6: Esc closes the mock palette and hands the focus back to the terminal. */
@Composable
private fun Palette(onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Box(Modifier.fillMaxSize(), Alignment.TopCenter) {
        Column(Modifier.width(360.dp).background(Color.White).padding(12.dp)) {
            BasicText("Command palette (mock). Esc closes.")
            BasicTextField(
                query,
                { query = it },
                Modifier.focusRequester(focus).onPreviewKeyEvent { event ->
                    val esc = event.type == KeyEventType.KeyDown && event.key == Key.Escape
                    if (esc) onClose()
                    esc
                },
            )
        }
    }
}

/** B1 (plan B): the same widget in its own Swing window while the Compose panel is gone. */
@Composable
internal fun DetachedFrame(
    detached: Boolean,
    terminal: JComponent,
    onClosed: () -> Unit,
) {
    DisposableEffect(detached) {
        val frame = if (detached) detachedFrame(terminal, onClosed) else null
        onDispose { frame?.dispose() }
    }
}

private fun detachedFrame(
    terminal: JComponent,
    onClosed: () -> Unit,
): JFrame =
    JFrame("AM-501 terminal (separate window)").apply {
        defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        add(terminal, BorderLayout.CENTER)
        setSize(DETACHED_WIDTH, DETACHED_HEIGHT)
        addWindowListener(
            object : WindowAdapter() {
                override fun windowClosed(e: WindowEvent) = onClosed()
            },
        )
        isVisible = true
    }
