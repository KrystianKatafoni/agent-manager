package dev.agentmanager.missioncontrol.ui

import dev.agentmanager.core.domain.Clock
import dev.agentmanager.core.domain.ProjectId
import dev.agentmanager.projects.api.ProjectsApi
import dev.agentmanager.sessions.api.SessionMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * State holder of Mission Control (MVI, docs/11 §1) for the project selected in the sidebar. Collects in
 * [scope] (the window scope, Main dispatcher; not thread-safe) and re-reads [clock] every [tick] for session
 * ages. Never throws, runs no commands and does no IO; shows only [SessionMonitor.snapshot].
 * @throws IllegalArgumentException if [tick] is not positive.
 */
public class MissionControlController(
    sessions: SessionMonitor,
    projects: ProjectsApi,
    selectedProject: StateFlow<ProjectId?>,
    clock: Clock,
    scope: CoroutineScope,
    tick: Duration = 1.seconds,
) {
    init {
        require(tick.isPositive()) { "tick must be positive" }
    }

    /** Times of the stale banner are shown in the zone of the Mac. */
    private val zone: ZoneId = ZoneId.systemDefault()
    private val selection = MutableStateFlow(Selection(project = selectedProject.value))
    private val now = MutableStateFlow(clock.now())

    /** Read by the composables only. */
    internal val state: StateFlow<MissionControlUiState> =
        combine(sessions.snapshot, projects.projects, selection, now) { snapshot, list, picked, at ->
            MissionControlReducer.reduce(snapshot, list, picked, at, zone)
        }.stateIn(
            scope,
            SharingStarted.Eagerly,
            MissionControlReducer.reduce(
                sessions.snapshot.value,
                projects.projects.value,
                selection.value,
                now.value,
                zone,
            ),
        )

    init {
        // Another project starts with the whole list and no selected session (plan, decision 1).
        scope.launch {
            selectedProject.collect { id -> selection.update { if (it.project == id) it else Selection(project = id) } }
        }
        // Ages tick only while a project is shown (plan, decision 2).
        scope.launch {
            selectedProject.collectLatest { id ->
                if (id != null) now.value = clock.now()
                while (id != null) {
                    delay(tick)
                    now.value = clock.now()
                }
            }
        }
    }

    /** Handles a click in the filter or the session list. */
    internal fun onEvent(event: MissionControlEvent) {
        when (event) {
            is MissionControlEvent.SelectFilter -> selection.update { it.copy(filter = event.filter) }
            is MissionControlEvent.SelectSession -> selection.update { it.copy(session = event.id) }
        }
    }
}
