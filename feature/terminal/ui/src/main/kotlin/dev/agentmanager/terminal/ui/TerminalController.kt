package dev.agentmanager.terminal.ui

import dev.agentmanager.core.domain.Logger
import dev.agentmanager.core.domain.Outcome
import dev.agentmanager.core.domain.ShellCommand
import dev.agentmanager.core.domain.info
import dev.agentmanager.core.domain.warn
import dev.agentmanager.terminal.api.TerminalApi
import dev.agentmanager.terminal.api.TerminalError
import dev.agentmanager.terminal.api.TerminalSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Tabs and panes of embedded terminals grouped by project (FR-T2). Opens sessions through [terminals] in [scope]
 * (the window scope, Main dispatcher; not thread-safe). [shellLabel] names shell tabs, e.g. `zsh`. Logs tab and
 * pane ids, kinds and fixed failure categories only, never commands, paths or project names (ENG-C8).
 *
 * The 11 operations are the API of the panel, the menu and the key dispatcher; the logic is in the reducers.
 */
@Suppress("TooManyFunctions")
public class TerminalController(
    private val terminals: TerminalApi,
    private val scope: CoroutineScope,
    private val logger: Logger,
    private val shellLabel: String,
) {
    private val store = MutableStateFlow(TerminalUiState())
    private val menuState = MutableStateFlow(MENU_EMPTY)
    private val exitWatchers = mutableMapOf<TabId, Job>()
    private var lastTab = 0
    private var splitPending = false

    internal val state: StateFlow<TerminalUiState> = store

    /** The search bar of the active pane (AM-504). */
    internal val search = TerminalSearchController(scope, logger) { activeTab() }

    /** What the terminal can do now; `app` enables menu items and palette commands from it. */
    public val menu: StateFlow<TerminalMenuState> = menuState

    /** Opens a login shell in [project]'s dir as a new tab shown in the active pane. Failures show in the panel. */
    public fun openShell(project: TerminalProject) {
        open(project, shellLabel, TabKind.SHELL, axis = null) { terminals.openShell(project.dir) }
    }

    /** Runs [command] (e.g. `attachCommand`) in a new tab labelled [label], shown in the active pane (AM-505). */
    public fun openCommand(
        project: TerminalProject,
        label: String,
        command: ShellCommand,
    ) {
        open(project, label, TabKind.COMMAND, axis = null) { terminals.openCommand(command) }
    }

    /** Closes the tab of the active pane and its session. */
    public fun closeActive() {
        val current = store.value
        val pane = current.active ?: return
        current.layout
            ?.leaves()
            ?.firstOrNull { it.pane == pane }
            ?.let { close(it.tab) }
    }

    /** Opens a shell of the active tab's project in a new pane next to the active one; ignored unless canSplit. */
    public fun split(axis: SplitAxis) {
        val project = activeTab()?.project
        if (project == null || !menuState.value.canSplit) return
        splitPending = true
        publishMenu()
        open(project, shellLabel, TabKind.SHELL, axis) { terminals.openShell(project.dir) }
    }

    /** Activates the next pane in reading order, cyclic. */
    public fun focusNext(): Unit = dispatch(TerminalEvent.FocusNext)

    /** Activates the previous pane in reading order, cyclic. */
    public fun focusPrevious(): Unit = dispatch(TerminalEvent.FocusPrevious)

    /** Shows only the active pane, or brings the layout back. Ignored with one pane. */
    public fun toggleMaximized(): Unit = dispatch(TerminalEvent.ToggleMaximized)

    /** A tab was clicked in the bar: shows it in its pane, or in the active pane when hidden. */
    internal fun select(tab: TabId): Unit = dispatch(TerminalEvent.Select(tab))

    /** The x on a tab: closes that tab and its session. */
    internal fun close(tab: TabId) {
        val closing = store.value.tabs.firstOrNull { it.id == tab } ?: return
        exitWatchers.remove(tab)?.cancel()
        dispatch(TerminalEvent.Close(tab))
        closing.session.close()
        logger.info("terminal tab closed", "tabId" to tab.value)
    }

    /** The user clicked a pane (focus moved into its widget). */
    internal fun focusPane(pane: PaneId): Unit = dispatch(TerminalEvent.FocusPane(pane))

    /** The x on the error bar. */
    internal fun dismissError(): Unit = dispatch(TerminalEvent.DismissError)

    private fun activeTab(): TerminalTab? {
        val current = store.value
        val leaf = current.layout?.leaves()?.firstOrNull { it.pane == current.active }
        return current.tabs.firstOrNull { it.id == leaf?.tab }
    }

    private fun open(
        project: TerminalProject,
        label: String,
        kind: TabKind,
        axis: SplitAxis?,
        start: suspend () -> Outcome<TerminalSession, TerminalError>,
    ) {
        scope.launch {
            try {
                when (val result = start()) {
                    is Outcome.Ok -> opened(newTab(project, label, kind, result.value), axis)
                    is Outcome.Err -> failed(result.error)
                }
            } finally {
                if (axis != null) {
                    splitPending = false
                    publishMenu()
                }
            }
        }
    }

    private fun newTab(
        project: TerminalProject,
        label: String,
        kind: TabKind,
        session: TerminalSession,
    ): TerminalTab = TerminalTab(TabId(++lastTab), project, label, kind, session, exitCode = null)

    private fun opened(
        tab: TerminalTab,
        axis: SplitAxis?,
    ) {
        dispatch(if (axis == null) TerminalEvent.Opened(tab) else TerminalEvent.SplitOpened(tab, axis))
        exitWatchers[tab.id] = scope.launch { exited(tab) }
        logger.info("terminal tab opened", "tabId" to tab.id.value, "kind" to tab.kind.name)
    }

    private suspend fun exited(tab: TerminalTab) {
        val code =
            tab.session.exitCode
                .filterNotNull()
                .first()
        dispatch(TerminalEvent.Exited(tab.id, code))
        exitWatchers.remove(tab.id)
        logger.info("terminal tab exited", "tabId" to tab.id.value, "code" to code)
    }

    private fun failed(error: TerminalError) {
        val failure =
            when (error) {
                TerminalError.WorkDirMissing -> OpenFailure.WORK_DIR_MISSING
                TerminalError.StartFailed -> OpenFailure.START_FAILED
                TerminalError.Closed -> return
            }
        dispatch(TerminalEvent.Failed(failure))
        logger.warn("terminal tab open failed", "reason" to failure.name)
    }

    private fun dispatch(event: TerminalEvent) {
        store.value = TerminalState.reduce(store.value, event)
        publishMenu()
        search.onActiveTabChanged(activeTab())
    }

    private fun publishMenu() {
        val current = store.value
        val panes = current.layout?.leaves()?.size ?: 0
        menuState.value =
            TerminalMenuState(
                hasTabs = current.tabs.isNotEmpty(),
                canSplit = panes in 1 until PaneLayout.MAX_PANES && !splitPending,
                canSwitchPane = panes > 1,
                maximized = current.maximized,
            )
    }

    private companion object {
        val MENU_EMPTY = TerminalMenuState(hasTabs = false, canSplit = false, canSwitchPane = false, maximized = false)
    }
}
