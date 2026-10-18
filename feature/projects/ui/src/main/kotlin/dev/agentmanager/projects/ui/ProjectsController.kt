package dev.agentmanager.projects.ui

import dev.agentmanager.agentruntime.api.AgentRuntime
import dev.agentmanager.core.domain.Logger
import dev.agentmanager.core.domain.Outcome
import dev.agentmanager.core.domain.SessionId
import dev.agentmanager.core.domain.info
import dev.agentmanager.core.domain.warn
import dev.agentmanager.projects.api.DirectoryReport
import dev.agentmanager.projects.api.Project
import dev.agentmanager.projects.api.ProjectError
import dev.agentmanager.projects.api.ProjectInspector
import dev.agentmanager.projects.api.ProjectsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State holder of the projects sidebar. Runs every command in [scope] (the window scope, Main dispatcher);
 * at most one dialog flow at a time, further requests are ignored until it ends. Never throws: errors
 * become fixed texts without paths (ENG-C8). Logs only fixed categories, project and session ids.
 *
 * @param picker the folder chooser; called from [scope].
 * @param scope confined to one thread, like the window's Main dispatcher: the controller is not thread-safe.
 */
public class ProjectsController(
    private val projects: ProjectsApi,
    private val inspector: ProjectInspector,
    private val runtime: AgentRuntime,
    private val picker: DirectoryPicker,
    private val scope: CoroutineScope,
    private val logger: Logger,
) {
    private val ui =
        MutableStateFlow(
            ProjectsUiState(
                projects = projects.projects.value,
                banner = projects.storageIssue.value?.let(ProjectMessages::of),
            ),
        )

    /** The running step of a dialog flow, kept only to cancel an inspection or a listing. */
    private var running: Job? = null

    /** Read by the composables only. */
    internal val state: StateFlow<ProjectsUiState> = ui.asStateFlow()

    /** Enabled window-menu commands. */
    public val menu: StateFlow<ProjectsMenuState> =
        ui.map(ProjectsUiState::menu).stateIn(scope, SharingStarted.Eagerly, ui.value.menu())

    init {
        scope.launch { projects.projects.collect { list -> ui.update { it.withProjects(list) } } }
    }

    /** Handles a sidebar or window-menu intent; ignored while a dialog flow is in progress. */
    public fun onIntent(intent: ProjectsIntent) {
        val current = ui.value
        if (current.dialog != null) return
        when (intent) {
            ProjectsIntent.AddRequested -> {
                begin(Dialog.Picking) { pickAndInspect() }
            }

            ProjectsIntent.RemoveSelectedRequested -> {
                current.selectedProject()?.let { begin(Dialog.Waiting.CHECKING_SESSIONS) { checkSessions(it) } }
            }
        }
    }

    /** Clicks inside the sidebar and its dialogs; a click that does not fit the open dialog is ignored. */
    internal fun onEvent(event: SidebarEvent) {
        val dialog = ui.value.dialog
        when (event) {
            is SidebarEvent.Select -> if (dialog == null) ui.update { it.copy(selected = event.id) }
            SidebarEvent.DismissBanner -> ui.update { it.copy(banner = null) }
            SidebarEvent.Dismiss -> dismiss(dialog)
            SidebarEvent.ConfirmAdd -> if (dialog is Dialog.ConfirmAdd) begin(Dialog.Waiting.BUSY) { add(dialog) }
            SidebarEvent.StopAndRemove -> confirmRemove(dialog, stop = true)
            SidebarEvent.RemoveKeepSessions -> confirmRemove(dialog, stop = false)
        }
    }

    /** Closes the dialog and cancels an inspection or listing; the folder chooser and a confirmed command run on. */
    private fun dismiss(dialog: Dialog?) {
        if (dialog == null || dialog == Dialog.Picking || dialog == Dialog.Waiting.BUSY) return
        running?.cancel()
        ui.update { it.copy(dialog = null) }
    }

    /** Stopping needs listed active sessions: without them (none, or the listing failed) only removing is offered. */
    private fun confirmRemove(
        dialog: Dialog?,
        stop: Boolean,
    ) {
        if (dialog !is Dialog.ConfirmRemove) return
        val sessions = dialog.sessions.orEmpty()
        if (stop && sessions.isEmpty()) return
        begin(Dialog.Waiting.BUSY) { remove(dialog.project, if (stop) sessions else emptyList()) }
    }

    /**
     * Opens [dialog] before [step] is launched, in the same call: a second click that arrives before the step
     * runs already sees [dialog] and is ignored, so a command never runs twice.
     */
    private fun begin(
        dialog: Dialog,
        step: suspend () -> Unit,
    ) {
        ui.update { it.copy(dialog = dialog) }
        running = scope.launch { step() }
    }

    /** Folder chooser, then the read-only inspection; a cancelled chooser changes nothing. */
    private suspend fun pickAndInspect() {
        val dir = picker.pick()
        if (dir == null) {
            ui.update { it.copy(dialog = null) }
            return
        }
        ui.update { it.copy(dialog = Dialog.Waiting.INSPECTING) }
        when (val inspected = inspector.inspect(dir)) {
            is Outcome.Err -> fail("project inspection failed", inspected.error)
            is Outcome.Ok -> offer(inspected.value)
        }
    }

    /** "Add" in [dialog]: an error keeps the dialog open with its fixed text. */
    private suspend fun add(dialog: Dialog.ConfirmAdd) {
        when (val added = projects.add(dialog.report.dir)) {
            is Outcome.Ok -> {
                logger.info("project added", "projectId" to added.value.id.value)
                ui.update { it.copy(selected = added.value.id, dialog = null) }
            }

            is Outcome.Err -> {
                logger.warn("project add failed", "reason" to added.error.category())
                ui.update { it.copy(dialog = dialog.copy(error = ProjectMessages.of(added.error))) }
            }
        }
    }

    /** Lists the sessions under [project] for the remove dialog; a runtime error leaves only "remove from list". */
    private suspend fun checkSessions(project: Project) {
        val listed = runtime.listSessions(under = project.dir)
        if (listed is Outcome.Err) logger.warn("session listing failed", "projectId" to project.id.value)
        val sessions = (listed as? Outcome.Ok)?.let { ActiveSessions.of(it.value) }
        ui.update { it.copy(dialog = Dialog.ConfirmRemove(project, sessions)) }
    }

    /** Stops [sessions] one by one, then removes [project]; a failed stop keeps the project on the list. */
    private suspend fun remove(
        project: Project,
        sessions: List<SessionId>,
    ) {
        for (session in sessions) {
            // The first failure ends the flow: the project stays on the list, so the user can retry or keep it.
            if (runtime.stop(session) is Outcome.Err) {
                logger.warn("session stop failed", "projectId" to project.id.value, "sessionId" to session.value)
                ui.update { it.copy(dialog = Dialog.Message(UiText("remove.stopFailed"))) }
                return
            }
        }
        when (val removed = projects.remove(project.id)) {
            is Outcome.Ok -> {
                logger.info("project removed", "projectId" to project.id.value, "sessionsStopped" to sessions.size)
                ui.update { it.copy(dialog = null) }
            }

            is Outcome.Err -> {
                fail("project remove failed", removed.error)
            }
        }
    }

    /** A folder already on the list selects its project instead of adding it again. */
    private fun offer(report: DirectoryReport) {
        val existing = report.alreadyAdded
        if (existing == null) {
            ui.update { it.copy(dialog = Dialog.ConfirmAdd(report)) }
        } else {
            val message = Dialog.Message(ProjectMessages.of(ProjectError.AlreadyAdded(existing)))
            ui.update { it.copy(selected = existing, dialog = message) }
        }
    }

    private fun fail(
        message: String,
        error: ProjectError,
    ) {
        logger.warn(message, "reason" to error.category())
        ui.update { it.copy(dialog = Dialog.Message(ProjectMessages.of(error))) }
    }
}

/** Window-menu commands are enabled only without an open dialog, so ⌘⌫ reaches text fields in dialogs. */
private fun ProjectsUiState.menu(): ProjectsMenuState =
    ProjectsMenuState(canAdd = dialog == null, canRemove = dialog == null && selected != null)

/** The new [list]; a selected project that left it is no longer selected. */
private fun ProjectsUiState.withProjects(list: List<Project>): ProjectsUiState =
    copy(projects = list, selected = selected?.takeIf { id -> list.any { it.id == id } })

private fun ProjectsUiState.selectedProject(): Project? = selected?.let { id -> projects.find { it.id == id } }

/** A fixed log category per variant, never the path or name it may hold (ENG-C8). */
private fun ProjectError.category(): String =
    when (this) {
        is ProjectError.NotAbsolute -> "notAbsolute"
        is ProjectError.NotFound -> "notFound"
        is ProjectError.NotADirectory -> "notADirectory"
        is ProjectError.AlreadyAdded -> "alreadyAdded"
        is ProjectError.UnknownProject -> "unknownProject"
        is ProjectError.InvalidDisplayName -> "invalidDisplayName"
        is ProjectError.StorageFailed -> "storageFailed"
    }
