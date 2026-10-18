package dev.agentmanager.projects.ui

import dev.agentmanager.agentruntime.api.AgentRuntime
import dev.agentmanager.core.domain.Logger
import dev.agentmanager.projects.api.Project
import dev.agentmanager.projects.api.ProjectInspector
import dev.agentmanager.projects.api.ProjectsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State holder of the projects sidebar. Runs every command in [scope] (the window scope, Main dispatcher);
 * at most one dialog flow at a time, further requests are ignored until it ends. Never throws: errors
 * become fixed texts without paths (ENG-C8). Logs only fixed categories and project ids.
 *
 * Six constructor parameters on purpose: a parameter object would be one more public type.
 *
 * @param picker the folder chooser; called from [scope], so on the UI thread.
 * @param scope confined to one thread, like the window's Main dispatcher: the controller is not thread-safe.
 */
@Suppress("LongParameterList")
public class ProjectsController(
    projects: ProjectsApi,
    inspector: ProjectInspector,
    runtime: AgentRuntime,
    picker: DirectoryPicker,
    private val scope: CoroutineScope,
    logger: Logger,
) {
    private val ui =
        MutableStateFlow(
            ProjectsUiState(
                projects = projects.projects.value,
                banner = projects.storageIssue.value?.let(ProjectMessages::of),
            ),
        )
    private val menuState = MutableStateFlow(ProjectsMenuState(canAdd = true, canRemove = false))
    private val flows = ProjectsDialogFlows(projects, inspector, runtime, picker, logger, ::update)

    /** The running step of a dialog flow: the folder chooser, an inspection, a listing or a command. */
    private var running: Job? = null

    /** Enabled window-menu commands. */
    public val menu: StateFlow<ProjectsMenuState> = menuState.asStateFlow()

    /** Read by the composables only. */
    internal val state: StateFlow<ProjectsUiState> = ui.asStateFlow()

    init {
        scope.launch { projects.projects.collect { list -> update { it.withProjects(list) } } }
    }

    /** Handles a sidebar or window-menu intent; ignored while a dialog flow is in progress. */
    public fun onIntent(intent: ProjectsIntent) {
        if (!isIdle()) return
        when (intent) {
            ProjectsIntent.AddRequested -> {
                start { flows.pickAndInspect() }
            }

            ProjectsIntent.RemoveSelectedRequested -> {
                ui.value.selectedProject()?.let { project -> start { flows.checkSessions(project) } }
            }
        }
    }

    /** Clicks inside the sidebar and its dialogs; a click that does not fit the open dialog is ignored. */
    internal fun onEvent(event: SidebarEvent) {
        val dialog = ui.value.dialog
        when (event) {
            is SidebarEvent.Select -> update { it.copy(selected = event.id) }
            SidebarEvent.DismissBanner -> update { it.copy(banner = null) }
            SidebarEvent.Dismiss -> dismiss(dialog)
            SidebarEvent.ConfirmAdd -> if (dialog is Dialog.ConfirmAdd) start { flows.add(dialog) }
            SidebarEvent.StopAndRemove -> confirmRemove(dialog, stop = true)
            SidebarEvent.RemoveKeepSessions -> confirmRemove(dialog, stop = false)
        }
    }

    private fun isIdle(): Boolean = running?.isActive != true && ui.value.dialog == null

    /** Closes the dialog and cancels an inspection or listing; a confirmed command ([Dialog.Busy]) runs to the end. */
    private fun dismiss(dialog: Dialog?) {
        if (dialog == null || dialog == Dialog.Busy) return
        running?.cancel()
        update { it.copy(dialog = null) }
    }

    /** Stopping needs listed active sessions: without them (none, or the listing failed) only removing is offered. */
    private fun confirmRemove(
        dialog: Dialog?,
        stop: Boolean,
    ) {
        if (dialog !is Dialog.ConfirmRemove || (stop && (dialog.active ?: 0) == 0)) return
        start { flows.remove(dialog.project, stop) }
    }

    private fun start(step: suspend () -> Unit) {
        val job = scope.launch { step() }
        running = job
        refreshMenu()
        job.invokeOnCompletion { refreshMenu() }
    }

    private fun update(transform: (ProjectsUiState) -> ProjectsUiState) {
        ui.update(transform)
        refreshMenu()
    }

    private fun refreshMenu() {
        val idle = isIdle()
        menuState.value = ProjectsMenuState(canAdd = idle, canRemove = idle && ui.value.selected != null)
    }
}

/** The new [list]; a selected project that left it is no longer selected. */
private fun ProjectsUiState.withProjects(list: List<Project>): ProjectsUiState =
    copy(projects = list, selected = selected?.takeIf { id -> list.any { it.id == id } })

private fun ProjectsUiState.selectedProject(): Project? = selected?.let { id -> projects.find { it.id == id } }
