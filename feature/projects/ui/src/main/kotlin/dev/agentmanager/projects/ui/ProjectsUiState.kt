package dev.agentmanager.projects.ui

import dev.agentmanager.core.domain.ProjectId
import dev.agentmanager.projects.api.DirectoryReport
import dev.agentmanager.projects.api.Project

/**
 * What the sidebar shows.
 *
 * @property projects registered projects in list order.
 * @property selected selected project, null = none (the initial state).
 * @property banner fixed text of the start-up storage issue until the user dismisses it.
 * @property dialog the one open dialog, null = none.
 */
internal data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val selected: ProjectId? = null,
    val banner: UiText? = null,
    val dialog: Dialog? = null,
)

/** The dialog flow in progress; at most one at a time. */
internal sealed interface Dialog {
    /** The picked folder is being inspected; [SidebarEvent.Dismiss] cancels it. */
    data object Inspecting : Dialog

    /** Inspection report waiting for "Add"; [error] is set when adding failed and the dialog stays. */
    data class ConfirmAdd(
        val report: DirectoryReport,
        val error: UiText? = null,
    ) : Dialog

    /** Listing the sessions of [project] before the remove dialog opens. */
    data class CheckingSessions(
        val project: Project,
    ) : Dialog

    /** Remove confirmation; [active] = working and blocked sessions, null = they could not be listed. */
    data class ConfirmRemove(
        val project: Project,
        val active: Int?,
    ) : Dialog

    /** A confirmed command (add, stop, remove) is running. */
    data object Busy : Dialog

    /** A fixed message without paths, closed with [SidebarEvent.Dismiss]. */
    data class Message(
        val text: UiText,
    ) : Dialog
}

/** Clicks inside the sidebar and its dialogs. */
internal sealed interface SidebarEvent {
    data class Select(
        val id: ProjectId,
    ) : SidebarEvent

    /** "Add" in [Dialog.ConfirmAdd]. */
    data object ConfirmAdd : SidebarEvent

    /** "Stop sessions and remove" in [Dialog.ConfirmRemove] with active sessions. */
    data object StopAndRemove : SidebarEvent

    /** "Remove" / "Remove from list, sessions keep running" in [Dialog.ConfirmRemove]. */
    data object RemoveKeepSessions : SidebarEvent

    /** "Cancel" or "Close": closes the open dialog and cancels a running inspection. */
    data object Dismiss : SidebarEvent

    /** Closes the storage issue banner. */
    data object DismissBanner : SidebarEvent
}
