package dev.agentmanager.terminal.impl

import dev.agentmanager.core.domain.Outcome
import dev.agentmanager.core.domain.ShellCommand
import dev.agentmanager.core.testing.RecordingLogger
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.awt.GraphicsEnvironment
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

/** Needs a display (macOS runner, not headless); skipped otherwise. */
@Timeout(value = 20, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class TerminalSpikeTest {
    @TempDir
    lateinit var dir: Path

    @Test
    fun `fabryka tworzy komponent i close zabija proces`() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        val before =
            ProcessHandle
                .current()
                .descendants()
                .map { it.pid() }
                .toList()
                .toSet()
        val command = ShellCommand(listOf("/bin/sh", "-c", "sleep 100"), dir)

        var result: Outcome<Pair<javax.swing.JComponent, AutoCloseable>, String>? = null
        SwingUtilities.invokeAndWait { result = openSpikeTerminal(command, RecordingLogger()) }

        val (component, handle) =
            result
                .shouldBeInstanceOf<Outcome.Ok<Pair<javax.swing.JComponent, AutoCloseable>>>()
                .value
        component.shouldBeInstanceOf<javax.swing.JComponent>()
        val started =
            ProcessHandle
                .current()
                .descendants()
                .filter { it.pid() !in before }
                .toList()
        started.shouldNotBeEmpty()

        SwingUtilities.invokeAndWait { handle.close() }

        started.forEach { it.onExit().get(5, TimeUnit.SECONDS) }
        started.all { !it.isAlive } shouldBe true
    }
}
