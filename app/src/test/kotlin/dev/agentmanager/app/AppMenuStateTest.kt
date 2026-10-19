package dev.agentmanager.app

import dev.agentmanager.projects.ui.ProjectsMenuState
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

/** Which window-menu items are enabled: dialog x palette x selection (AM-408, decision 2). */
internal class AppMenuStateTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    fun `menu items follow the controller and the palette`(
        @Suppress("UNUSED_PARAMETER") name: String,
        menu: ProjectsMenuState,
        paletteOpen: Boolean,
        expected: AppMenuState,
    ) {
        AppMenuState.of(menu, paletteOpen) shouldBe expected
    }

    companion object {
        private val none = ProjectsMenuState(canAdd = true, canRemove = false, canRename = false, dialogOpen = false)
        private val selected = ProjectsMenuState(canAdd = true, canRemove = true, canRename = true, dialogOpen = false)
        private val dialog = ProjectsMenuState(canAdd = false, canRemove = false, canRename = false, dialogOpen = true)
        private val everything = AppMenuState(add = true, remove = true, rename = true, palette = true)
        private val nothing = AppMenuState(add = false, remove = false, rename = false, palette = false)

        @JvmStatic
        fun cases(): List<Arguments> =
            listOf(
                arguments("selection, no dialog, palette closed", selected, false, everything),
                arguments(
                    "no selection, no dialog, palette closed",
                    none,
                    false,
                    AppMenuState(add = true, remove = false, rename = false, palette = true),
                ),
                arguments("selection, palette open", selected, true, nothing),
                arguments("no selection, palette open", none, true, nothing),
                arguments("dialog open, palette closed", dialog, false, nothing),
                arguments("dialog open and palette open", dialog, true, nothing),
            )
    }
}
