package dev.agentmanager.agentruntime.impl

import dev.agentmanager.agentruntime.api.RuntimeSession
import dev.agentmanager.agentruntime.api.RuntimeWorld
import dev.agentmanager.agentruntime.api.WaitingFor
import dev.agentmanager.core.testing.T0
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.time.Instant
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.setPosixFilePermissions
import kotlin.io.path.writeText

/**
 * Fake `claude` executable for the adapter tests (docs/12, plan AM-203): `fake-claude.sh` from the
 * test resources plus its state in files, installed in a test-owned directory.
 *
 * [executable] is an absolute path to `bin/claude`, so neither the real `claude` on `PATH` nor the
 * one added by `path_helper` on macOS is ever run. For a world that is not installed it is a name
 * that no `PATH` has, so the login shell reports it as not found.
 */
internal class FakeClaude private constructor(
    val executable: String,
    val stateDir: Path,
) {
    /** Arguments (without the program) of every call so far, in call order. */
    fun calls(): List<List<String>> {
        val log = stateDir.resolve("calls.log")
        if (!log.exists()) return emptyList()
        return log
            .readText()
            .split(CALL_END)
            .dropLast(1)
            .map { call -> call.split(ARG_END).dropLast(1) }
    }

    /** Switches the behaviour of later calls: `normal`, `garbage`, `hang`, `fail <code> <stderr>`, `bg-child`. */
    fun setMode(mode: String) {
        stateDir.resolve("mode").writeText(mode)
    }

    /** Pids written by the `hang` (`hang.pid`) or `bg-child` (`bg-child.pid`) mode; empty before they run. */
    fun pids(file: String): List<Long> {
        val path = stateDir.resolve(file)
        if (!path.exists()) return emptyList()
        return path
            .readText()
            .lines()
            .filter { it.isNotBlank() }
            .map { it.trim().toLong() }
    }

    /** Raw lines of `sessions.tsv`, for tests of the fake itself. */
    fun sessionLines(): List<String> =
        stateDir
            .resolve("sessions.tsv")
            .readText()
            .lines()
            .filter { it.isNotBlank() }

    companion object {
        private const val ARG_END = '\u001f'
        private const val CALL_END = '\u001e'
        private const val EMPTY = "-"
        private const val SCRIPT_RESOURCE = "/fake-claude/fake-claude.sh"

        /**
         * Installs the fake in [dir] with the supervisor state of [world]; new sessions get [now] as
         * their start time. [RuntimeWorld.agentViewEnabled] is not modelled yet (health is AM-204).
         */
        fun install(
            dir: Path,
            world: RuntimeWorld,
            now: Instant = T0,
        ): FakeClaude {
            val state = dir.resolve("state").createDirectories()
            val logs = state.resolve("logs").createDirectories()
            state.resolve("sessions.tsv").writeText(world.sessions.joinToString("") { tsvLine(it) + "\n" })
            world.outputs.forEach { (id, output) -> logs.resolve(id.value).writeText(output) }
            state.resolve("counter").writeText("0\n")
            state.resolve("now").writeText("${now.toEpochMilli()}\n")
            state.resolve("version").writeText("${world.version}\n")
            state.resolve("mode").writeText("normal\n")

            val script = dir.resolve("fake-claude.sh")
            script.writeText(scriptText())
            val bin = dir.resolve("bin").createDirectories()
            val claude = bin.resolve("claude")
            claude.writeText("#!/bin/sh\nexec /bin/sh '$script' '$state' \"\$@\"\n")
            claude.setPosixFilePermissions(PosixFilePermissions.fromString("rwxr-xr-x"))

            val executable = if (world.installed) claude.toString() else "claude-am-missing-${UUID.randomUUID()}"
            return FakeClaude(executable, state)
        }

        private fun scriptText(): String =
            requireNotNull(FakeClaude::class.java.getResource(SCRIPT_RESOURCE)) { "missing $SCRIPT_RESOURCE" }
                .readText()

        private fun tsvLine(session: RuntimeSession): String {
            val fields =
                listOf(
                    session.id.value,
                    session.conversationId?.value ?: EMPTY,
                    session.name,
                    session.cwd.toString(),
                    session.phase.name.lowercase(),
                    session.waitingFor?.let(::waitingToken) ?: EMPTY,
                    session.startedAt.toEpochMilli().toString(),
                    session.pid?.toString() ?: EMPTY,
                )
            require(fields.none { it.isEmpty() || it.contains('\t') || it.contains('\n') }) {
                "FakeClaude cannot store session ${session.id}: empty field, tab or newline"
            }
            return fields.joinToString("\t")
        }

        private fun waitingToken(waitingFor: WaitingFor): String =
            when (waitingFor) {
                WaitingFor.Permission -> "permission prompt"
                WaitingFor.Input -> "input needed"
                is WaitingFor.Other -> waitingFor.raw
            }
    }
}
