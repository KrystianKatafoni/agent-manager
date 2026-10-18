package dev.agentmanager.sessions.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Read side of the sessions feature (ENG-I2): what every Claude Code session is doing now.
 *
 * Hot: it watches the supervisor whether anybody collects or not. Session state lives in the
 * supervisor, so it survives app restarts and is rebuilt on start (NFR-3).
 */
public interface SessionMonitor {
    /**
     * Latest view of all sessions. Starts as [SessionsSnapshot.Initial] and is refreshed at least every
     * 3 s (NFR-2). Conflated: a slow reader sees only the newest snapshot.
     */
    public val snapshot: StateFlow<SessionsSnapshot>

    /**
     * State transitions, hot and without replay. Every event is emitted after [snapshot] already
     * shows [SessionEvent.session]. Sessions already present in the first snapshot emit nothing.
     */
    public val events: Flow<SessionEvent>
}
