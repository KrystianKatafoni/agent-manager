package dev.agentmanager.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import dev.agentmanager.commandpalette.api.ChordKey
import dev.agentmanager.commandpalette.api.KeyChord
import dev.agentmanager.commandpalette.api.KeyModifier
import dev.agentmanager.commandpalette.api.Shortcut
import dev.agentmanager.commandpalette.api.letterChord
import dev.agentmanager.commandpalette.ui.toKeyShortcut
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** The one source of the key bindings of the window menu and of the palette rows (AM-408, decision 2). */
class AppShortcutsTest {
    @Test
    fun `shortcuts are the documented ones`() {
        AppShortcuts.ADD_PROJECT shouldBe Shortcut.of(letterChord('o', KeyModifier.META, KeyModifier.SHIFT))
        AppShortcuts.REMOVE_PROJECT shouldBe Shortcut.of(KeyChord(ChordKey.Named.BACKSPACE, setOf(KeyModifier.META)))
        AppShortcuts.PALETTE shouldBe Shortcut.of(letterChord('k', KeyModifier.META))
    }

    @Test
    fun `every shortcut has a menu binding`() {
        AppShortcuts.ADD_PROJECT.toKeyShortcut() shouldBe KeyShortcut(Key.O, meta = true, shift = true)
        AppShortcuts.REMOVE_PROJECT.toKeyShortcut() shouldBe KeyShortcut(Key.Backspace, meta = true)
        AppShortcuts.PALETTE.toKeyShortcut() shouldBe KeyShortcut(Key.K, meta = true)
    }
}
