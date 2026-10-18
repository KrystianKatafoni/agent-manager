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
     * Latest view of all sessions. [SessionsSnapshot.Initial] until the first listing, then refreshed
     * at least every 3 s (NFR-2). Conflated: a slow reader sees only the newest snapshot.
     */
    public val snapshot: StateFlow<SessionsSnapshot>

    /**
     * State transitions, hot and without replay. Every event is emitted after [snapshot] already
     * shows [SessionEvent.session]. A session first seen waiting counts as becoming blocked
     * ([SessionEvent.BecameBlocked], a new request for the user, FR-N1); sessions present at startup
     * emit nothing.
     */
    public val events: Flow<SessionEvent>
}
