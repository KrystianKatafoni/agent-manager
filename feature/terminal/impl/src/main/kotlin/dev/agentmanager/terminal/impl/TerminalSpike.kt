package dev.agentmanager.terminal.impl

import com.jediterm.terminal.ui.JediTermWidget
import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import dev.agentmanager.core.domain.Logger
import dev.agentmanager.core.domain.Outcome
import dev.agentmanager.core.domain.ShellCommand
import dev.agentmanager.core.domain.catching
import dev.agentmanager.core.domain.flatMap
import dev.agentmanager.core.domain.info
import dev.agentmanager.core.domain.warn
import java.nio.file.Files
import javax.swing.JComponent

private const val WORK_DIR_MISSING = "work-dir-missing"
private const val PTY_START_FAILED = "pty-start-failed"
private const val INITIAL_COLUMNS = 80
private const val INITIAL_ROWS = 24

/**
 * Spike AM-501, replaced by `terminalModule` in AM-502. Starts [command] in a PTY (pty4j, TERM=xterm-256color)
 * and returns the JediTerm widget with a handle that closes the widget and kills the process tree.
 * No JediTerm or pty4j type escapes (ENG-I8). Call on the AWT event thread (it builds a Swing component).
 * @return Err with [TerminalSpikeErrors] category: work dir missing or the PTY did not start (never the
 *   exception message, it may quote the environment).
 */
public fun openSpikeTerminal(
    command: ShellCommand,
    logger: Logger,
): Outcome<Pair<JComponent, AutoCloseable>, String> {
    if (!Files.isDirectory(command.workDir)) {
        logger.warn("Terminal spike: work dir missing", "reason" to WORK_DIR_MISSING)
        return Outcome.Err(WORK_DIR_MISSING)
    }
    return catching(onException = { PTY_START_FAILED }) { startProcess(command) }
        .flatMap { process ->
            catching(onException = {
                process.destroyForcibly()
                PTY_START_FAILED
            }) { widgetFor(process, logger) }
        }.also { result ->
            if (result is Outcome.Err) logger.warn("Terminal spike: PTY did not start", "reason" to result.error)
        }
}

private fun startProcess(command: ShellCommand): PtyProcess =
    PtyProcessBuilder(command.argv.toTypedArray())
        .setEnvironment(System.getenv() + command.env + ("TERM" to "xterm-256color"))
        .setDirectory(command.workDir.toString())
        .setInitialColumns(INITIAL_COLUMNS)
        .setInitialRows(INITIAL_ROWS)
        .start()

private fun widgetFor(
    process: PtyProcess,
    logger: Logger,
): Pair<JComponent, AutoCloseable> {
    val connector = PtyTtyConnector(process)
    val widget = JediTermWidget(TerminalWidgetSettings())
    widget.setTtyConnector(connector)
    widget.start()
    logger.info("Terminal spike: PTY started")
    val handle =
        AutoCloseable {
            connector.close()
            widget.close()
        }
    return widget to handle
}

/** Stable error categories of [openSpikeTerminal]. */
public val TerminalSpikeErrors: Set<String> = setOf(WORK_DIR_MISSING, PTY_START_FAILED)
