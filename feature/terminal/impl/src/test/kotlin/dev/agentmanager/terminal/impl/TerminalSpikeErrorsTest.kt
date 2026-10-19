package dev.agentmanager.terminal.impl

import dev.agentmanager.core.domain.Outcome
import dev.agentmanager.core.domain.ShellCommand
import dev.agentmanager.core.testing.RecordingLogger
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TerminalSpikeErrorsTest {
    @TempDir
    lateinit var dir: Path

    private val logger = RecordingLogger()

    @Test
    fun `kategorie bledow to work-dir-missing i pty-start-failed`() {
        TerminalSpikeErrors shouldContainExactlyInAnyOrder listOf("work-dir-missing", "pty-start-failed")
    }

    @Test
    fun `openSpikeTerminal zwraca work-dir-missing dla nieistniejacego katalogu`() {
        val command = ShellCommand(listOf("/bin/sh"), dir.resolve("does-not-exist"))

        val result = openSpikeTerminal(command, logger)

        result.shouldBeInstanceOf<Outcome.Err<String>>().error shouldBe "work-dir-missing"
    }

    @Test
    fun `kategorie bledow nie zawieraja tekstu wyjatku ani srodowiska`() {
        val command =
            ShellCommand(
                argv = listOf("/bin/sh"),
                workDir = dir.resolve("secret-dir-name"),
                env = mapOf("API_TOKEN" to "secret-token-value"),
            )

        val error = openSpikeTerminal(command, logger).shouldBeInstanceOf<Outcome.Err<String>>().error

        TerminalSpikeErrors.contains(error) shouldBe true
        error shouldNotContain "secret"
        error shouldNotContain dir.toString()
        error shouldNotContain "Exception"
        logger.entries.forEach {
            it.message shouldNotContain "secret-token-value"
            it.fields.values.forEach { value -> value.toString() shouldNotContain "secret-token-value" }
        }
    }

    @Test
    fun `start PTY nieudany daje pty-start-failed bez sciezki i sekretu`() {
        val command =
            ShellCommand(
                argv = listOf(dir.resolve("secret-bin").toString()),
                workDir = dir,
                env = mapOf("API_TOKEN" to "secret-token-value"),
            )

        val error = openSpikeTerminal(command, logger).shouldBeInstanceOf<Outcome.Err<String>>().error

        error shouldBe "pty-start-failed"
        logger.entries.forEach {
            it.message shouldNotContain "secret"
            it.fields.values.forEach { value -> value.toString() shouldNotContain "secret" }
        }
    }
}
