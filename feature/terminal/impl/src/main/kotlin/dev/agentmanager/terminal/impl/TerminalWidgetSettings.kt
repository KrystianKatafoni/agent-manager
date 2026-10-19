package dev.agentmanager.terminal.impl

import com.jediterm.terminal.ui.settings.DefaultSettingsProvider

/** JediTerm widget settings: Option sends Escape (docs/06), no copy on selection, 13 pt font. */
internal class TerminalWidgetSettings : DefaultSettingsProvider() {
    override fun altSendsEscape(): Boolean = true

    override fun copyOnSelect(): Boolean = false

    override fun useAntialiasing(): Boolean = true

    override fun getTerminalFontSize(): Float = FONT_SIZE

    override fun emulateX11CopyPaste(): Boolean = false

    private companion object {
        const val FONT_SIZE = 13f
    }
}
