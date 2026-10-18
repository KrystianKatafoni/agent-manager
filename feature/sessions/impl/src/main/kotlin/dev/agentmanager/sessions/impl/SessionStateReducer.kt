package dev.agentmanager.sessions.impl

import dev.agentmanager.agentruntime.api.RuntimeError
import dev.agentmanager.agentruntime.api.RuntimeSession
import dev.agentmanager.core.domain.Outcome
import dev.agentmanager.core.domain.ProjectId
import dev.agentmanager.sessions.api.SessionEvent
import dev.agentmanager.sessions.api.SessionsSnapshot
import java.nio.file.Path
import java.time.Instant

/** Result of one [SessionStateReducer.reduce]: publish [snapshot] first, then [events] (AM-401 contract). */
internal data class Reduction(
    val snapshot: SessionsSnapshot,
    val events: List<SessionEvent>,
)

/** Pure core of the sessions feature (docs/11 §7). Stateless; no IO, no clock, never throws. */
internal object SessionStateReducer {
    /**
     * @param previous the snapshot published so far ([SessionsSnapshot.Initial] on start).
     * @param listing result of `AgentRuntime.listSessions()`; AM-404 passes it through its command overlay first.
     * @param projects registered project dirs for [dev.agentmanager.sessions.api.SessionRules.projectFor].
     * @param now time of the listing (from the injected `Clock` in AM-403).
     * @return sessions ordered by startedAt, then id; events in the same order, only for transitions.
     */
    @Suppress("UnusedParameter") // Test-first skeleton of AM-402; the implementation uses every parameter.
    fun reduce(
        previous: SessionsSnapshot,
        listing: Outcome<List<RuntimeSession>, RuntimeError>,
        projects: Map<ProjectId, Path>,
        now: Instant,
    ): Reduction = TODO("AM-402")
}
