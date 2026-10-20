package dev.agentmanager.terminal.ui

import dev.agentmanager.core.domain.ProjectId

/** Semantics tags of the terminal area for UI tests. A pane frame exposes `selected` = it is the active pane. */
internal object TerminalTags {
    const val AREA = "terminal.area"
    const val ERROR = "terminal.error"
    const val ERROR_DISMISS = "terminal.error.dismiss"

    fun group(project: ProjectId): String = "terminal.group.${project.value}"

    /** The "+" at the end of a group: opens a shell in that project. */
    fun groupAdd(project: ProjectId): String = "terminal.group.${project.value}.add"

    fun tab(tab: TabId): String = "terminal.tab.${tab.value}"

    fun tabClose(tab: TabId): String = "terminal.tab.${tab.value}.close"

    /** The `exit N` marker of an exited tab. */
    fun tabExit(tab: TabId): String = "terminal.tab.${tab.value}.exit"

    fun pane(pane: PaneId): String = "terminal.pane.${pane.value}"
}
