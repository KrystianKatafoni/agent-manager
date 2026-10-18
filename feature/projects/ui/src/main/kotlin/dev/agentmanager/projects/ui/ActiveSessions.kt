package dev.agentmanager.projects.ui

import dev.agentmanager.agentruntime.api.RuntimeSession
import dev.agentmanager.core.domain.SessionId

/** Sessions that keep running after a project leaves the list: WORKING and BLOCKED. */
internal object ActiveSessions {
    fun of(sessions: List<RuntimeSession>): List<SessionId> = TODO("AM-304: ${sessions.size}")
}
