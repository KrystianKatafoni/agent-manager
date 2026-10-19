package dev.agentmanager.missioncontrol.ui

import dev.agentmanager.sessions.api.SessionEvent
import dev.agentmanager.sessions.api.SessionMonitor
import dev.agentmanager.sessions.api.SessionsSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * A [SessionMonitor] whose snapshot the test sets directly: summaries and stale snapshots that `FakeSessions`
 * cannot produce. Mission Control reads only [snapshot], so [events] stays empty.
 */
internal class ScriptedSessionMonitor(
    initial: SessionsSnapshot,
) : SessionMonitor {
    private val current = MutableStateFlow(initial)

    override val snapshot: StateFlow<SessionsSnapshot> = current.asStateFlow()

    override val events: Flow<SessionEvent> = emptyFlow()

    fun show(snapshot: SessionsSnapshot) {
        current.value = snapshot
    }
}
