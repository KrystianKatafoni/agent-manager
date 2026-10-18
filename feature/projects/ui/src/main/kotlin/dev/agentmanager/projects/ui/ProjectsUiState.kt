package dev.agentmanager.projects.ui

import dev.agentmanager.core.domain.ProjectId
import dev.agentmanager.core.domain.SessionId
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
    /** The system folder chooser is open; it is modal, so the sidebar shows nothing over it. */
    data object Picking : Dialog

    /** A step is running; [INSPECTING] and [CHECKING_SESSIONS] end with [SidebarEvent.Dismiss], [BUSY] does not. */
    enum class Waiting(
        val tag: String,
        val key: String,
    ) : Dialog {
        /** The picked folder is being inspected. */
        INSPECTING(ProjectsTags.INSPECTING, "add.inspecting"),

        /** The sessions of the project are listed before the remove dialog opens. */
        CHECKING_SESSIONS(ProjectsTags.CHECKING_SESSIONS, "remove.checking"),

        /** A confirmed command (add, stop, remove) is running. */
        BUSY(ProjectsTags.BUSY, "dialog.busy"),
    }

    /** Inspection report waiting for "Add"; [error] is set when adding failed and the dialog stays. */
    data class ConfirmAdd(
        val report: DirectoryReport,
        val error: UiText? = null,
    ) : Dialog

    /** Remove confirmation; [sessions] = working and blocked sessions, null = they could not be listed. */
    data class ConfirmRemove(
        val project: Project,
        val sessions: List<SessionId>?,
    ) : Dialog

    /** A fixed message without paths, closed with [SidebarEvent.Dismiss]. */
    data class Message(
        val text: UiText,
    ) : Dialog
}

/** Clicks inside the sidebar and its dialogs. */
internal sealed interface SidebarEvent {
    /** A row click; ignored while a dialog is open. */
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
