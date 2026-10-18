package dev.agentmanager.core.process

import dev.agentmanager.core.domain.Logger
import dev.agentmanager.core.domain.NoOpLogger
import dev.agentmanager.core.domain.Outcome
import dev.agentmanager.core.domain.ShellCommand
import dev.agentmanager.core.domain.catching
import dev.agentmanager.core.domain.debug
import dev.agentmanager.core.domain.flatMap
import dev.agentmanager.core.domain.warn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * [ProcessRunner] that starts every command as `<shell> -l -c 'exec "$@"' agent-manager <argv>`.
 *
 * A macOS app launched from Finder gets a minimal `PATH`; the login shell loads the user's profile
 * (`.zprofile` for zsh), so programs such as `claude` are found as in Terminal. Arguments are passed
 * as positional parameters, never re-parsed by the shell. On timeout or cancellation the process and
 * all its descendants are killed. Blocking IO runs on [io].
 *
 * @param io dispatcher for the blocking process calls, usually `AppDispatchers.io`.
 * @param shell login shell to start; defaults to [defaultShell].
 * @param logger receives the program name and working directory, never arguments or environment.
 */
public class LoginShellProcessRunner(
    private val io: CoroutineDispatcher,
    private val shell: Path = defaultShell(),
    private val logger: Logger = NoOpLogger,
) : ProcessRunner {
    override suspend fun run(
        command: ShellCommand,
        options: RunOptions,
    ): Outcome<ProcessResult, ProcessError> =
        withContext(io) {
            logger.debug("process start", "program" to command.argv.first(), "workDir" to command.workDir)
            when (val started = start(command)) {
                is Outcome.Err -> Outcome.Err(started.error)
                is Outcome.Ok -> catching(::ioFailed) { collect(started.value, command, options) }.flatMap { it }
            }
        }

    private fun start(command: ShellCommand): Outcome<Process, ProcessError> =
        catching({ ProcessError.StartFailed(it.describe()) }) {
            ProcessBuilder(listOf(shell.toString(), "-l", "-c", EXEC_ARGS, ARG0) + command.argv)
                .directory(command.workDir.toFile())
                .apply { environment().putAll(command.env) }
                .start()
                .also { it.outputStream.close() }
        }

    private suspend fun collect(
        process: Process,
        command: ShellCommand,
        options: RunOptions,
    ): Outcome<ProcessResult, ProcessError> =
        coroutineScope {
            val stdout = async { process.inputStream.readCapped(options.maxOutputBytes) }
            val stderr = async { process.errorStream.readCapped(options.maxOutputBytes) }
            val exited =
                try {
                    runInterruptible { process.waitFor(options.timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS) }
                } finally {
                    // Timeout or cancellation: killing the tree closes the pipes, so the readers finish.
                    if (process.isAlive) kill(process)
                }
            val out = stdout.await()
            val err = stderr.await()
            val program = command.argv.first()
            when {
                !exited -> {
                    logger.warn("process timeout", "program" to program, "timeout" to options.timeout)
                    Outcome.Err(ProcessError.Timeout(options.timeout))
                }

                isNotFound(process.exitValue(), err.text, program) -> {
                    Outcome.Err(ProcessError.NotFound(program))
                }

                else -> {
                    Outcome.Ok(ProcessResult(process.exitValue(), out.text, err.text, out.truncated, err.truncated))
                }
            }
        }

    private fun kill(process: Process) {
        process.descendants().forEach { it.destroyForcibly() }
        process.destroyForcibly()
        process.waitFor()
    }

    private fun ioFailed(e: Exception): ProcessError = ProcessError.IoFailed(e.describe())

    private fun Exception.describe(): String = message ?: javaClass.simpleName

    public companion object {
        private const val EXEC_ARGS = "exec \"\$@\""
        private const val ARG0 = "agent-manager"
        private const val NOT_FOUND_EXIT = 127
        private const val NOT_FOUND_TEXT = "not found"

        /** Shell exit 127 with its own "not found" line naming the program, not a profile error. */
        internal fun isNotFound(
            exitCode: Int,
            stderr: String,
            program: String,
        ): Boolean = exitCode == NOT_FOUND_EXIT && stderr.lines().any { NOT_FOUND_TEXT in it && program in it }

        /** The user's shell from `SHELL`, or `/bin/zsh` (the macOS default) when it is not set. */
        public fun defaultShell(): Path =
            System.getenv("SHELL")?.takeIf { it.isNotBlank() }?.let { Path.of(it) } ?: Path.of("/bin/zsh")
    }
}

internal class Captured(
    val text: String,
    val truncated: Boolean,
)

private const val BUFFER_SIZE = 8192

/** Reads the stream to the end, keeping at most [limit] bytes from its start. */
internal fun InputStream.readCapped(limit: Int): Captured =
    use { stream ->
        val kept = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var truncated = false
        var read = stream.read(buffer)
        while (read >= 0) {
            val room = limit - kept.size()
            if (room > 0) kept.write(buffer, 0, minOf(read, room))
            if (read > room) truncated = true
            read = stream.read(buffer)
        }
        Captured(kept.toString(Charsets.UTF_8), truncated)
    }
