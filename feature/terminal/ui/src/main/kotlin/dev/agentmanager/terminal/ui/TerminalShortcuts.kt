package dev.agentmanager.terminal.ui

import java.awt.event.KeyEvent

/** Pane keys of the terminal (plan AM-503, table 3). */
internal object TerminalShortcuts {
    private data class Binding(
        val keyCode: Int,
        val shift: Boolean,
        val alt: Boolean,
    )

    private val bindings: Map<Binding, PaneCommand> =
        mapOf(
            Binding(KeyEvent.VK_D, shift = false, alt = false) to PaneCommand.Split(SplitAxis.SIDE_BY_SIDE),
            Binding(KeyEvent.VK_D, shift = true, alt = false) to PaneCommand.Split(SplitAxis.STACKED),
            Binding(KeyEvent.VK_RIGHT, shift = false, alt = true) to PaneCommand.FocusNext,
            Binding(KeyEvent.VK_LEFT, shift = false, alt = true) to PaneCommand.FocusPrevious,
            Binding(KeyEvent.VK_ENTER, shift = true, alt = false) to PaneCommand.ToggleMaximized,
        )

    /** The pane command of [key], null for any other key. */
    fun commandOf(key: TerminalKey): PaneCommand? =
        if (key.meta && !key.ctrl) bindings[Binding(key.keyCode, key.shift, key.alt)] else null
}
