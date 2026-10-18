package dev.agentmanager.sessions.api

import dev.agentmanager.core.domain.ProjectId
import java.nio.file.Path

/** Pure rules shared by every sessions implementation and its fakes (ENG-C9). No IO. */
public object SessionRules {
    /**
     * The project whose directory in [projects] is the deepest one that equals or contains [cwd],
     * compared by path components (`/p/ab` is not inside `/p/a`); null when none contains it.
     */
    @Suppress("UnusedParameter") // Skeleton for the test-first step; the implementation uses both.
    public fun projectFor(
        cwd: Path,
        projects: Map<ProjectId, Path>,
    ): ProjectId? = TODO("AM-401: projectFor")
}
