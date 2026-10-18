package dev.agentmanager.sessions.api

import dev.agentmanager.core.domain.ProjectId
import java.nio.file.Path

/** Pure rules shared by every sessions implementation and its fakes (ENG-C9). No IO. */
public object SessionRules {
    /**
     * The project whose directory in [projects] is the deepest one that equals or contains [cwd],
     * compared by path components (`/p/ab` is not inside `/p/a`); null when none contains it.
     */
    public fun projectFor(
        cwd: Path,
        projects: Map<ProjectId, Path>,
    ): ProjectId? =
        projects.entries
            .filter { (_, dir) -> cwd.startsWith(dir) }
            .maxByOrNull { (_, dir) -> dir.nameCount }
            ?.key
}
