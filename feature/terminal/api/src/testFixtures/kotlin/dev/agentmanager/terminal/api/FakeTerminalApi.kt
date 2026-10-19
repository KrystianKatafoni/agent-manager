package dev.agentmanager.terminal.api

import dev.agentmanager.core.domain.Outcome
import dev.agentmanager.core.domain.ShellCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * In-memory [TerminalApi] for tests of other features: no process, no PTY, a plain Swing panel as the
 * component. A test ends a "process" with [FakeTerminalSession.finish] and reads typed text from
 * [FakeTerminalSession.sent].
 */
public class FakeTerminalApi : TerminalApi {
    private val counter = AtomicInteger()
    private val opened = CopyOnWriteArrayList<FakeTerminalSession>()

    @Volatile
    private var closed = false

    /** Every session opened so far, in order (closed ones included). */
    public val sessions: List<FakeTerminalSession> get() = opened.toList()

    /** Commands passed to [openCommand] that succeeded, in order. */
    public val commands: List<ShellCommand> get() = ranCommands.toList()

    private val ranCommands = CopyOnWriteArrayList<ShellCommand>()

    override suspend fun openShell(dir: Path): Outcome<TerminalSession, TerminalError> = open(dir)

    override suspend fun openCommand(command: ShellCommand): Outcome<TerminalSession, TerminalError> =
        open(command.workDir).also { if (it is Outcome.Ok) ranCommands += command }

    private fun open(dir: Path): Outcome<TerminalSession, TerminalError> =
        when {
            closed -> {
                Outcome.Err(TerminalError.Closed)
            }

            !Files.isDirectory(dir) -> {
                Outcome.Err(TerminalError.WorkDirMissing)
            }

            else -> {
                Outcome.Ok(
                    FakeTerminalSession(TerminalId("fake-${counter.incrementAndGet()}")).also { opened += it },
                )
            }
        }

    override fun close() {
        closed = true
        opened.forEach { it.close() }
    }
}

/** Session of [FakeTerminalApi]; running until [finish] or [close]. */
public class FakeTerminalSession internal constructor(
    override val id: TerminalId,
) : TerminalSession {
    private val exit = MutableStateFlow<Int?>(null)
    private val typed = CopyOnWriteArrayList<String>()
    private val panel: JPanel by lazy { JPanel() }

    @Volatile
    private var closed = false

    override val exitCode: StateFlow<Int?> get() = exit

    /** Everything accepted by [send], in order. */
    public val sent: List<String> get() = typed.toList()

    /** True after [close]. */
    public val isClosed: Boolean get() = closed

    /** Simulates the process ending with [code]; the session stays open. */
    public fun finish(code: Int) {
        exit.value = code
    }

    override fun component(): JComponent = panel

    override suspend fun send(text: String): Outcome<Unit, TerminalError> =
        if (closed || exit.value != null) {
            Outcome.Err(TerminalError.Closed)
        } else {
            typed += text
            Outcome.Ok(Unit)
        }

    override fun close() {
        closed = true
    }
}
