package dev.agentmanager.terminal.impl

import com.jediterm.terminal.ui.TerminalActionPresentation
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import javax.swing.KeyStroke

/** JediTerm widget settings: Option sends Escape (docs/06), no copy on selection, 13 pt font, Cmd-K left free. */
internal class TerminalWidgetSettings : DefaultSettingsProvider() {
    override fun altSendsEscape(): Boolean = true

    override fun copyOnSelect(): Boolean = false

    override fun useAntialiasing(): Boolean = true

    override fun getTerminalFontSize(): Float = FONT_SIZE

    override fun emulateX11CopyPaste(): Boolean = false

    /** No shortcut, so Cmd-K stays with the app's palette (ADR 0012 decision 3). */
    override fun getClearBufferActionPresentation(): TerminalActionPresentation =
        TerminalActionPresentation("Clear Buffer", emptyList<KeyStroke>())

    private companion object {
        const val FONT_SIZE = 13f
    }
}
