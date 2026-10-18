package dev.agentmanager.sessions.api

import dev.agentmanager.core.domain.Clock
import dev.agentmanager.core.domain.Outcome
import dev.agentmanager.core.domain.ProjectId
import dev.agentmanager.core.domain.SessionId
import dev.agentmanager.core.testing.TestClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Scriptable in-memory sessions feature for tests of other modules (ENG-I7): one state behind both
 * [SessionMonitor] and [SessionCommands]. Passes [SessionMonitorContract] and [SessionCommandsContract].
 *
 * Sessions of [world] are attributed to its projects by [SessionRules.projectFor]. The snapshot is
 * [SnapshotStatus.Fresh] at [Clock.now] from the start, and every change is visible in it at once.
 * Started sessions get ids `00000001`, `00000002`, … and [Clock.now] as their start time.
 */
public class FakeSessions(
    private val world: SessionsWorld = SessionsWorld(),
    private val clock: Clock = TestClock(),
) : SessionMonitor,
    SessionCommands {
    override val snapshot: StateFlow<SessionsSnapshot> get() = TODO("AM-401: FakeSessions.snapshot")

    override val events: Flow<SessionEvent> get() = TODO("AM-401: FakeSessions.events")

    /** Commands called so far, in order, e.g. `start agent-manager`, `stop 7c5dcf5d`, `stopAllIn agent-manager`. */
    public val calls: List<String> get() = TODO("AM-401: FakeSessions.calls")

    /** The next command returns `Err(error)` and changes nothing; later commands work again. */
    public fun failNext(error: SessionError): Unit = TODO("AM-401: FakeSessions.failNext")

    /** The existing session [id] moves to [state]; emits the matching [SessionEvent], if any. */
    public suspend fun setState(
        id: SessionId,
        state: SessionState,
    ): Unit = TODO("AM-401: FakeSessions.setState")

    /** Adds [session] as if it had been started outside the app; emits no event. */
    public fun appear(session: WorldSession): Unit = TODO("AM-401: FakeSessions.appear")

    /**
     * false: the snapshot keeps its sessions and turns [SnapshotStatus.Stale] with
     * [RuntimeProblem.NOT_INSTALLED], and every command fails with it; true: fresh again.
     */
    public fun runtimeAvailable(available: Boolean): Unit = TODO("AM-401: FakeSessions.runtimeAvailable")

    /** Every later stop of [id] fails with [RuntimeProblem.COMMAND_FAILED] and leaves it running. */
    public fun failStop(id: SessionId): Unit = TODO("AM-401: FakeSessions.failStop")

    override suspend fun start(
        project: ProjectId,
        spec: NewSessionSpec,
    ): Outcome<SessionId, SessionError> = TODO("AM-401: FakeSessions.start")

    override suspend fun stop(id: SessionId): Outcome<Unit, SessionError> = TODO("AM-401: FakeSessions.stop")

    override suspend fun stopAllIn(project: ProjectId): Outcome<Int, SessionError> =
        TODO("AM-401: FakeSessions.stopAllIn")

    override suspend fun respawn(id: SessionId): Outcome<Unit, SessionError> = TODO("AM-401: FakeSessions.respawn")

    override suspend fun remove(id: SessionId): Outcome<Unit, SessionError> = TODO("AM-401: FakeSessions.remove")
}
