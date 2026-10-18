package dev.agentmanager.projects.ui

import dev.agentmanager.agentruntime.api.RuntimePhase
import dev.agentmanager.agentruntime.api.RuntimeSession
import dev.agentmanager.core.domain.SessionId

/**
 * Sessions that keep running after a project leaves the list: WORKING and BLOCKED, in list order. The same
 * rule as the inspector count in `projects:impl`, duplicated on purpose (`ui` never sees `impl`, ENG-C9).
 */
internal object ActiveSessions {
    private val ACTIVE = setOf(RuntimePhase.WORKING, RuntimePhase.BLOCKED)

    fun of(sessions: List<RuntimeSession>): List<SessionId> = sessions.filter { it.phase in ACTIVE }.map { it.id }
}
