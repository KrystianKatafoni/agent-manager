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

/**
 * The suspending steps of the add and remove dialogs (AM-304 plan, decisions 1 and 2). Each step moves the
 * sidebar state through [update]; [ProjectsController] decides when a step may start and runs it.
 *
 * Logs fixed categories and project ids only, never a path, a name or a runtime error text (ENG-C8).
 */
internal class ProjectsDialogFlows(
    private val projects: ProjectsApi,
    private val inspector: ProjectInspector,
    private val runtime: AgentRuntime,
    private val picker: DirectoryPicker,
    private val logger: Logger,
    private val update: ((ProjectsUiState) -> ProjectsUiState) -> Unit,
) {
    /** Active sessions of the project in the last [Dialog.ConfirmRemove], stopped by [remove] with `stop`. */
    private var activeSessions: List<SessionId> = emptyList()

    /** Folder chooser, then the read-only inspection; a cancelled chooser changes nothing. */
    suspend fun pickAndInspect() {
        val dir = picker.pick() ?: return
        update { it.copy(dialog = Dialog.Inspecting) }
        when (val inspected = inspector.inspect(dir)) {
            is Outcome.Err -> fail("project inspection failed", inspected.error)
            is Outcome.Ok -> offer(inspected.value)
        }
    }

    /** "Add" in [dialog]: an error keeps the dialog open with its fixed text. */
    suspend fun add(dialog: Dialog.ConfirmAdd) {
        update { it.copy(dialog = Dialog.Busy) }
        when (val added = projects.add(dialog.report.dir)) {
            is Outcome.Ok -> {
                logger.info("project added", "projectId" to added.value.id.value)
                update { it.copy(selected = added.value.id, dialog = null) }
            }

            is Outcome.Err -> {
                logger.warn("project add failed", "reason" to added.error.category())
                update { it.copy(dialog = dialog.copy(error = ProjectMessages.of(added.error))) }
            }
        }
    }

    /** Lists the sessions under [project] for the remove dialog; a runtime error leaves only "remove from list". */
    suspend fun checkSessions(project: Project) {
        update { it.copy(dialog = Dialog.CheckingSessions(project)) }
        val listed = runtime.listSessions(under = project.dir)
        activeSessions = (listed as? Outcome.Ok)?.let { ActiveSessions.of(it.value) }.orEmpty()
        if (listed is Outcome.Err) logger.warn("session listing failed", "projectId" to project.id.value)
        val active = if (listed is Outcome.Ok) activeSessions.size else null
        update { it.copy(dialog = Dialog.ConfirmRemove(project, active)) }
    }

    /** With [stop], stops the active sessions one by one first; a failed stop keeps the project on the list. */
    suspend fun remove(
        project: Project,
        stop: Boolean,
    ) {
        update { it.copy(dialog = Dialog.Busy) }
        if (stop && activeSessions.any { runtime.stop(it) is Outcome.Err }) {
            logger.warn("session stop failed", "projectId" to project.id.value)
            update { it.copy(dialog = Dialog.Message(UiText("remove.stopFailed"))) }
            return
        }
        when (val removed = projects.remove(project.id)) {
            is Outcome.Ok -> {
                logger.info("project removed", "projectId" to project.id.value, "sessionsStopped" to stop)
                update { it.copy(dialog = null) }
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
            update { it.copy(dialog = Dialog.ConfirmAdd(report)) }
        } else {
            val message = Dialog.Message(ProjectMessages.of(ProjectError.AlreadyAdded(existing)))
            update { it.copy(selected = existing, dialog = message) }
        }
    }

    private fun fail(
        message: String,
        error: ProjectError,
    ) {
        logger.warn(message, "reason" to error.category())
        update { it.copy(dialog = Dialog.Message(ProjectMessages.of(error))) }
    }

    /** A fixed log category: the variant name only, never the path it may hold. */
    private fun ProjectError.category(): String = this::class.simpleName.orEmpty()
}
