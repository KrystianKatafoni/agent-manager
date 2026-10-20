@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.agentmanager.terminal.ui

import dev.agentmanager.core.domain.Outcome
import dev.agentmanager.core.domain.ProjectId
import dev.agentmanager.core.domain.ShellCommand
import dev.agentmanager.core.testing.RecordingLogger
import dev.agentmanager.terminal.api.FakeTerminalApi
import dev.agentmanager.terminal.api.TerminalApi
import dev.agentmanager.terminal.api.TerminalError
import dev.agentmanager.terminal.api.TerminalId
import dev.agentmanager.terminal.api.TerminalSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JPanel

/** Plain session for reducer tests: no process, a Swing panel as the widget. */
internal class StubSession(
    n: Int,
) : TerminalSession {
    override val id: TerminalId = TerminalId("stub-$n")
    override val exitCode: StateFlow<Int?> = MutableStateFlow(null)
    private val panel = JPanel()

    override fun component(): JComponent = panel

    override suspend fun send(text: String): Outcome<Unit, TerminalError> = Outcome.Ok(Unit)

    override fun close() = Unit
}

internal fun project(
    id: String,
    dir: Path = Path.of("/projects/$id"),
    name: String = id.replaceFirstChar { it.uppercase() },
): TerminalProject = TerminalProject(ProjectId(id), name, dir)

internal val ALPHA: TerminalProject = project("alpha")
internal val BETA: TerminalProject = project("beta")

internal fun aTab(
    id: Int,
    project: TerminalProject = ALPHA,
    label: String = "zsh",
    kind: TabKind = TabKind.SHELL,
    exitCode: Int? = null,
): TerminalTab = TerminalTab(TabId(id), project, label, kind, StubSession(id), exitCode)

internal fun leaf(
    pane: Int,
    tab: Int,
): PaneTree.Leaf = PaneTree.Leaf(PaneId(pane), TabId(tab))

internal fun pane(n: Int): PaneId = PaneId(n)

internal fun tabId(n: Int): TabId = TabId(n)

/** Folds [events] through the reducer from [from]. */
internal fun stateAfter(
    vararg events: TerminalEvent,
    from: TerminalUiState = TerminalUiState(),
): TerminalUiState = events.fold(from) { state, event -> TerminalState.reduce(state, event) }

internal fun TerminalUiState.tab(n: Int): TerminalTab = tabs.single { it.id == TabId(n) }

internal fun TerminalUiState.leafTabs(): List<Int> = layout?.leaves().orEmpty().map { it.tab.value }

internal fun key(
    keyCode: Int,
    meta: Boolean = false,
    shift: Boolean = false,
    alt: Boolean = false,
    ctrl: Boolean = false,
): TerminalKey = TerminalKey(keyCode, meta, shift, alt, ctrl)

/**
 * [FakeTerminalApi] that remembers the directories of shells, can fail on demand and can hold an open until
 * [gate] completes (a split in flight).
 */
internal class ScriptedTerminalApi(
    val fake: FakeTerminalApi = FakeTerminalApi(),
) : TerminalApi {
    val shellDirs = mutableListOf<Path>()

    @Volatile var failWith: TerminalError? = null

    @Volatile var gate: CompletableDeferred<Unit>? = null

    override suspend fun openShell(dir: Path): Outcome<TerminalSession, TerminalError> {
        shellDirs.add(dir)
        gate?.await()
        return failWith?.let { Outcome.Err(it) } ?: fake.openShell(dir)
    }

    override suspend fun openCommand(command: ShellCommand): Outcome<TerminalSession, TerminalError> {
        gate?.await()
        return failWith?.let { Outcome.Err(it) } ?: fake.openCommand(command)
    }

    override fun close() = fake.close()
}

/** World of controller tests: a controller over a [ScriptedTerminalApi] driven by the test scheduler. */
internal class ControllerWorld(
    scope: TestScope,
    val api: ScriptedTerminalApi,
) {
    val logger = RecordingLogger()
    val controller = TerminalController(api, scope.backgroundScope, logger, "zsh")
    val state get() = controller.state.value
}

internal fun TestScope.world(): ControllerWorld = ControllerWorld(this, ScriptedTerminalApi())

/** A project named [name] with a real directory under [root]. */
internal fun projectIn(
    root: Path,
    name: String,
    id: String = name.lowercase(),
): TerminalProject = project(id, Files.createDirectory(root.resolve(name)), name)

internal fun TestScope.act(block: () -> Unit) {
    block()
    runCurrent()
}
