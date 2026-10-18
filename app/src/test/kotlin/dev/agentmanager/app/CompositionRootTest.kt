package dev.agentmanager.app

import dev.agentmanager.core.domain.SystemClock
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Test

class CompositionRootTest {
    @Test
    @Suppress("InjectDispatcher") // Asserts the one place that picks real dispatchers.
    fun `production dependencies use the real clock, dispatchers and SLF4J`() {
        val core = productionCoreDeps()

        core.clock shouldBe SystemClock
        core.dispatchers.io shouldBe Dispatchers.IO
        core.dispatchers.default shouldBe Dispatchers.Default
        core.logger.shouldBeInstanceOf<Slf4jLogger>()
    }

    @Test
    fun `every UI text exists and is not blank`() {
        Strings.keys shouldContainAll setOf("app.title", "missionControl.empty.title", "missionControl.empty.hint")
        Strings.keys.forEach { Strings[it].shouldNotBeBlank() }
    }
}
