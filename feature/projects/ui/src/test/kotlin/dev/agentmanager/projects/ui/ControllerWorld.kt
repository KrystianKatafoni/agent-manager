@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.agentmanager.projects.ui

import dev.agentmanager.agentruntime.api.FakeAgentRuntime
import dev.agentmanager.core.testing.RecordingLogger
import dev.agentmanager.projects.api.FakeProjectInspector
import dev.agentmanager.projects.api.FakeProjectsApi
import dev.agentmanager.projects.api.InspectionWorld
import dev.agentmanager.projects.api.PathKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import java.nio.file.Path

/**
 * Fakes around a [ProjectsController]: the project list, the runtime, the folder chooser and an inspector
 * that answers from [inspection] and sees the same list as [projects], so `alreadyAdded` is real.
 */
internal class ControllerWorld(
    val projects: FakeProjectsApi = FakeProjectsApi(),
    val runtime: FakeAgentRuntime = FakeAgentRuntime(),
    inspection: InspectionWorld = InspectionWorld(),
    pathKind: (Path) -> PathKind = { PathKind.DIRECTORY },
    gatedInspection: Boolean = false,
) {
    val picker = FakeDirectoryPicker()
    val inspector = RecordingInspector(FakeProjectInspector(inspection, projects, pathKind), gatedInspection)
    val logger = RecordingLogger()

    fun controller(scope: CoroutineScope): ProjectsController =
        ProjectsController(projects, inspector, runtime, picker, scope, logger)
}

/** The controller of [world] running in `backgroundScope`, after it has read the initial state. */
internal fun TestScope.start(world: ControllerWorld): ProjectsController =
    world.controller(backgroundScope).also { runCurrent() }

/**
 * Runs [block], usually one intent or event, and lets the controller finish what it started. `runCurrent`,
 * not `advanceUntilIdle`: the latter skips tasks of `backgroundScope`, where the controller runs.
 */
internal fun TestScope.act(block: () -> Unit) {
    block()
    runCurrent()
}
