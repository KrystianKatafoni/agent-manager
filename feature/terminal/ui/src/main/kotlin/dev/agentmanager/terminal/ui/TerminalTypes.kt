package dev.agentmanager.terminal.ui

import dev.agentmanager.core.domain.ProjectId
import dev.agentmanager.terminal.api.TerminalSession
import java.nio.file.Path

/** The project a tab belongs to; tabs are grouped by [id] and labelled with [name]. [dir]: where shells start. */
public data class TerminalProject(
    val id: ProjectId,
    val name: String,
    val dir: Path,
)

/** How [TerminalController.split] places the new pane: ⌘D [SIDE_BY_SIDE], ⌘⇧D [STACKED]. */
public enum class SplitAxis { SIDE_BY_SIDE, STACKED }

/** A key press seen while a terminal has the focus; [keyCode] is a `java.awt.event.KeyEvent.VK_*` code. */
public data class TerminalKey(
    val keyCode: Int,
    val meta: Boolean,
    val shift: Boolean,
    val alt: Boolean,
    val ctrl: Boolean,
)

/** What the terminal can do now; `app` enables menu items and palette commands from it. */
public data class TerminalMenuState(
    val hasTabs: Boolean,
    val canSplit: Boolean,
    val canSwitchPane: Boolean,
    val maximized: Boolean,
)

@JvmInline
internal value class TabId(
    val value: Int,
)

@JvmInline
internal value class PaneId(
    val value: Int,
)

internal enum class TabKind { SHELL, COMMAND }

internal data class TerminalTab(
    val id: TabId,
    val project: TerminalProject,
    val label: String,
    val kind: TabKind,
    val session: TerminalSession,
    val exitCode: Int?,
)

internal enum class OpenFailure { WORK_DIR_MISSING, START_FAILED }

internal data class TerminalUiState(
    val tabs: List<TerminalTab> = emptyList(),
    val layout: PaneTree? = null,
    val active: PaneId? = null,
    val maximized: Boolean = false,
    val error: OpenFailure? = null,
)

/** Events of [TerminalState.reduce]. */
internal sealed interface TerminalEvent {
    /** Events handled by [TerminalTabs]. */
    sealed interface OfTab : TerminalEvent

    /** Events handled by [PaneLayout]. */
    sealed interface OfPane : TerminalEvent

    data class Opened(
        val tab: TerminalTab,
    ) : OfTab

    data class Select(
        val tab: TabId,
    ) : OfTab

    data class Close(
        val tab: TabId,
    ) : OfTab

    data class Exited(
        val tab: TabId,
        val code: Int,
    ) : OfTab

    data class SplitOpened(
        val tab: TerminalTab,
        val axis: SplitAxis,
    ) : OfPane

    data class FocusPane(
        val pane: PaneId,
    ) : OfPane

    data object FocusNext : OfPane

    data object FocusPrevious : OfPane

    data object ToggleMaximized : OfPane

    data class Failed(
        val failure: OpenFailure,
    ) : TerminalEvent

    data object DismissError : TerminalEvent
}

/** A pane command bound to a key (⌘D, ⌘⇧D, ⌘⌥←/→, ⌘⇧↵). */
internal sealed interface PaneCommand {
    data class Split(
        val axis: SplitAxis,
    ) : PaneCommand

    data object FocusNext : PaneCommand

    data object FocusPrevious : PaneCommand

    data object ToggleMaximized : PaneCommand

    /** ⌘F: opens the search bar of the active pane. */
    data object Find : PaneCommand
}
