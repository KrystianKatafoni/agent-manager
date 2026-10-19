package dev.agentmanager.missioncontrol.ui

import dev.agentmanager.core.domain.Outcome
import dev.agentmanager.core.domain.SessionId
import dev.agentmanager.sessions.api.FakeSessions
import dev.agentmanager.sessions.api.SessionCommands
import dev.agentmanager.sessions.api.SessionError
import dev.agentmanager.sessions.api.SessionsApi
import kotlinx.coroutines.CompletableDeferred

/** [FakeSessions] as both sides of the sessions feature. */
internal fun FakeSessions.api(): SessionsApi = SessionsApi(monitor = this, commands = this)

/**
 * Commands that stay "in progress" until the test opens [gate]: stop, respawn and remove record the call in [started],
 * wait for the gate and then delegate to [inner]. Gives the "action runs" state without a real clock.
 */
internal class GatedCommands(
    private val inner: SessionCommands,
) : SessionCommands by inner {
    val gate = CompletableDeferred<Unit>()
    val started = mutableListOf<String>()

    override suspend fun stop(id: SessionId): Outcome<Unit, SessionError> {
        started += "stop ${id.value}"
        gate.await()
        return inner.stop(id)
    }

    override suspend fun respawn(id: SessionId): Outcome<Unit, SessionError> {
        started += "respawn ${id.value}"
        gate.await()
        return inner.respawn(id)
    }

    override suspend fun remove(id: SessionId): Outcome<Unit, SessionError> {
        started += "remove ${id.value}"
        gate.await()
        return inner.remove(id)
    }
}
