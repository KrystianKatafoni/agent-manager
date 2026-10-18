package dev.agentmanager.sessions.impl

import dev.agentmanager.agentruntime.api.RuntimeError
import dev.agentmanager.agentruntime.api.RuntimeSession
import dev.agentmanager.core.domain.Clock
import dev.agentmanager.core.domain.Outcome
import dev.agentmanager.core.domain.ProjectId
import dev.agentmanager.sessions.api.SessionEvent
import dev.agentmanager.sessions.api.SessionsSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Path

/**
 * Single owner of the published state. [publish] reads the current snapshot, reduces, sets [snapshot],
 * then emits the events, all under one Mutex (AM-402 review: calls of reduce are serialized). AM-404
 * commands publish through the same instance.
 */
internal class SessionsState(
    private val clock: Clock,
) {
    private val mutex = Mutex()
    private val current = MutableStateFlow(SessionsSnapshot.Initial)
    private val emitted = MutableSharedFlow<SessionEvent>(extraBufferCapacity = EVENT_BUFFER)

    /** Latest snapshot, [SessionsSnapshot.Initial] until the first [publish]. */
    val snapshot: StateFlow<SessionsSnapshot> = current.asStateFlow()

    /** Transitions without replay; each one is emitted after [snapshot] shows it. */
    val events: SharedFlow<SessionEvent> = emitted.asSharedFlow()

    /** Reduces [listing] against the current snapshot at the clock's time and publishes the result. */
    suspend fun publish(
        listing: Outcome<List<RuntimeSession>, RuntimeError>,
        projects: Map<ProjectId, Path>,
    ): Reduction =
        mutex.withLock {
            val reduction = SessionStateReducer.reduce(current.value, listing, projects, clock.now())
            current.value = reduction.snapshot
            reduction.events.forEach { emitted.emit(it) }
            reduction
        }

    private companion object {
        // docs/11 §6: a slow collector suspends publishing instead of losing events.
        const val EVENT_BUFFER = 64
    }
}
