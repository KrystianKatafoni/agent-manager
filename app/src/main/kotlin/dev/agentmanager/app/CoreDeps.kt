package dev.agentmanager.app

import dev.agentmanager.core.domain.AppDispatchers
import dev.agentmanager.core.domain.Clock
import dev.agentmanager.core.domain.Logger
import dev.agentmanager.core.domain.SystemClock
import kotlinx.coroutines.Dispatchers
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * Shared infrastructure handed to every module factory (docs/11 §9). The process runner and JSON storage
 * join here when the first feature module needs them.
 */
internal data class CoreDeps(
    val clock: Clock,
    val dispatchers: AppDispatchers,
    val logger: Logger,
)

/** Real clock, dispatchers and logging. The composition root is the one place that picks them (ENG-C6). */
@Suppress("InjectDispatcher")
internal fun productionCoreDeps(): CoreDeps =
    CoreDeps(
        clock = SystemClock,
        dispatchers = AppDispatchers(main = Dispatchers.Main, io = Dispatchers.IO, default = Dispatchers.Default),
        logger = Slf4jLogger(LoggerFactory.getLogger("am")),
    )

/** Directory of the app's JSON files, e.g. `projects.json`: `~/Library/Application Support/AgentManager` (AM-304). */
internal fun appSupportDir(userHome: Path): Path = TODO("AM-304: $userHome")
