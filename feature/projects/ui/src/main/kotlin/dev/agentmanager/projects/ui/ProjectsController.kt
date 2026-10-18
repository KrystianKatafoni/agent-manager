package dev.agentmanager.projects.ui

import dev.agentmanager.agentruntime.api.AgentRuntime
import dev.agentmanager.core.domain.Logger
import dev.agentmanager.projects.api.ProjectInspector
import dev.agentmanager.projects.api.ProjectsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * State holder of the projects sidebar. Runs every command in [scope] (the window scope, Main dispatcher);
 * at most one dialog flow at a time, further requests are ignored until it ends. Never throws: errors
 * become fixed texts without paths (ENG-C8). Logs only fixed categories and project ids.
 *
 * Six constructor parameters on purpose: a parameter object would be one more public type.
 */
@Suppress("LongParameterList", "UnusedPrivateProperty")
public class ProjectsController(
    private val projects: ProjectsApi,
    private val inspector: ProjectInspector,
    private val runtime: AgentRuntime,
    private val picker: DirectoryPicker,
    private val scope: CoroutineScope,
    private val logger: Logger,
) {
    /** Enabled window-menu commands. */
    public val menu: StateFlow<ProjectsMenuState> get() = TODO("AM-304")

    /** Handles a sidebar or window-menu intent. */
    public fun onIntent(intent: ProjectsIntent): Unit = TODO("AM-304: $intent")

    /** Read by the composables only. */
    internal val state: StateFlow<ProjectsUiState> get() = TODO("AM-304")

    /** Clicks inside the sidebar and its dialogs. */
    internal fun onEvent(event: SidebarEvent): Unit = TODO("AM-304: $event")
}
