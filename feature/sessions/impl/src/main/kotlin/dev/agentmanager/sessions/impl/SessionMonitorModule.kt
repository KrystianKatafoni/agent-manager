package dev.agentmanager.sessions.impl

import dev.agentmanager.agentruntime.api.AgentRuntime
import dev.agentmanager.core.domain.Clock
import dev.agentmanager.core.domain.Logger
import dev.agentmanager.projects.api.ProjectsApi
import dev.agentmanager.sessions.api.RuntimeProblem
import dev.agentmanager.sessions.api.SessionMonitor
import dev.agentmanager.sessions.api.SessionsSnapshot
import dev.agentmanager.sessions.api.SnapshotStatus
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Starts watching the supervisor in [scope] and returns the read side of sessions (FR-S1, NFR-2, NFR-3).
 * The first listing runs at once, then one every [pollInterval] after the previous one ends; a change of
 * the project dirs in [projects] lists again at once. After repeated failures the wait grows up to
 * [maxRetryInterval] while the snapshot stays [SnapshotStatus.Stale]. Cancelling [scope] stops polling; the
 * last snapshot stays readable. Not suspending: the snapshot is [SessionsSnapshot.Initial] until the first
 * listing. The long parameter list is deliberate: `impl` may expose only factories.
 *
 * @param scope owner of the loop, e.g. the app scope with a SupervisorJob; tests pass `backgroundScope`.
 * @param clock time of each listing ([SnapshotStatus.Fresh.at], `lastActivityAt`).
 * @param logger gets session counts and [RuntimeProblem] only, never cwd, names or questions (ENG-C8).
 * @throws IllegalArgumentException if [pollInterval] is not positive or [maxRetryInterval] is shorter.
 */
@Suppress("LongParameterList")
public fun sessionMonitorModule(
    runtime: AgentRuntime,
    projects: ProjectsApi,
    scope: CoroutineScope,
    clock: Clock,
    logger: Logger,
    pollInterval: Duration = 2.seconds,
    maxRetryInterval: Duration = 15.seconds,
): SessionMonitor =
    PollingSessionMonitor(
        runtime = runtime,
        projects = projects,
        state = SessionsState(clock),
        schedule = PollSchedule(pollInterval, maxRetryInterval),
        logger = logger,
    ).also { it.start(scope) }
