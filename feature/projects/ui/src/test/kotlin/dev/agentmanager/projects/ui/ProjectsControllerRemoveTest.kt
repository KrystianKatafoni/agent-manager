package dev.agentmanager.projects.ui

import dev.agentmanager.agentruntime.api.FakeAgentRuntime
import dev.agentmanager.agentruntime.api.RuntimeError
import dev.agentmanager.agentruntime.api.RuntimePhase
import dev.agentmanager.agentruntime.api.RuntimeWorld
import dev.agentmanager.agentruntime.api.aRuntimeSession
import dev.agentmanager.core.testing.aSessionId
import dev.agentmanager.projects.api.A_PROJECT_DIR
import dev.agentmanager.projects.api.FakeProjectsApi
import dev.agentmanager.projects.api.ProjectError
import dev.agentmanager.projects.api.StorageProblem
import dev.agentmanager.projects.api.aProject
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.nio.file.Path

/** Removing a project with a confirmation, also with running sessions (FR-P3, FR-P4; AM-304 plan, decision 1). */
class ProjectsControllerRemoveTest {
    private val project = aProject(id = "project0001", dir = A_PROJECT_DIR)

    /** Two active sessions of [project] (one in a subfolder), one ended, one active in a sibling folder. */
    private val sessions =
        listOf(
            aRuntimeSession(id = aSessionId("working"), cwd = A_PROJECT_DIR, phase = RuntimePhase.WORKING),
            aRuntimeSession(
                id = aSessionId("blocked"),
                cwd = A_PROJECT_DIR.resolve("src"),
                phase = RuntimePhase.BLOCKED,
            ),
            aRuntimeSession(id = aSessionId("done"), cwd = A_PROJECT_DIR, phase = RuntimePhase.DONE),
            aRuntimeSession(
                id = aSessionId("sibling"),
                cwd = Path.of("/Users/test/projects/agent-manager-web"),
                phase = RuntimePhase.WORKING,
            ),
        )

    private fun world(
        runtime: FakeAgentRuntime = FakeAgentRuntime(RuntimeWorld(sessions = sessions)),
    ): ControllerWorld = ControllerWorld(projects = FakeProjectsApi(initial = listOf(project)), runtime = runtime)

    /** Selects [project] and asks to remove it, as ⌘⌫ does. */
    private fun TestScope.requestRemove(controller: ProjectsController) {
        act { controller.onEvent(SidebarEvent.Select(project.id)) }
        act { controller.onIntent(ProjectsIntent.RemoveSelectedRequested) }
    }

    private fun FakeAgentRuntime.stops(): List<String> = calls.filter { it.startsWith("stop") }

    @Test
    fun `remove without active sessions removes after confirmation`() =
        runTest {
            val world = world(FakeAgentRuntime(RuntimeWorld(sessions = sessions.filter { it.id.value == "done" })))
            val controller = start(world)

            requestRemove(controller)
            controller.state.value.dialog shouldBe Dialog.ConfirmRemove(project, sessions = emptyList())
            world.projects.calls.shouldBeEmpty()

            act { controller.onEvent(SidebarEvent.RemoveKeepSessions) }
            world.projects.calls shouldBe listOf("remove project0001")
            controller.state.value shouldBe ProjectsUiState()
            world.runtime.stops().shouldBeEmpty()
        }

    @Test
    fun `remove with active sessions offers stop and keep`() =
        runTest {
            val world = world()
            val controller = start(world)

            requestRemove(controller)

            controller.state.value.dialog shouldBe
                Dialog.ConfirmRemove(project, sessions = listOf(aSessionId("working"), aSessionId("blocked")))
            world.runtime.calls shouldBe listOf("listSessions")
        }

    @Test
    fun `stop and remove stops only working and blocked sessions of the project`() =
        runTest {
            val world = world()
            val controller = start(world)
            requestRemove(controller)

            act { controller.onEvent(SidebarEvent.StopAndRemove) }

            world.runtime.stops() shouldContainExactly listOf("stop working", "stop blocked")
            world.projects.calls shouldBe listOf("remove project0001")
            controller.state.value shouldBe ProjectsUiState()
        }

    @Test
    fun `a second stop and remove before the first runs is ignored`() =
        runTest {
            val world = world()
            val controller = start(world)
            requestRemove(controller)

            act {
                controller.onEvent(SidebarEvent.StopAndRemove)
                controller.onEvent(SidebarEvent.StopAndRemove)
            }

            world.runtime.stops() shouldContainExactly listOf("stop working", "stop blocked")
            world.projects.calls shouldBe listOf("remove project0001")
            controller.state.value shouldBe ProjectsUiState()
        }

    @Test
    fun `keep sessions removes without stopping anything`() =
        runTest {
            val world = world()
            val controller = start(world)
            requestRemove(controller)

            act { controller.onEvent(SidebarEvent.RemoveKeepSessions) }

            world.runtime.stops().shouldBeEmpty()
            world.projects.calls shouldBe listOf("remove project0001")
            controller.state.value.projects
                .shouldBeEmpty()
        }

    @Test
    fun `failed stop keeps the project and shows a message`() =
        runTest {
            val world = world()
            val controller = start(world)
            requestRemove(controller)
            world.runtime.failNext(RuntimeError.CommandFailed(exitCode = 1, stderrTail = "boom"))

            act { controller.onEvent(SidebarEvent.StopAndRemove) }

            world.projects.calls.shouldBeEmpty()
            controller.state.value.projects shouldBe listOf(project)
            controller.state.value.dialog shouldBe Dialog.Message(UiText("remove.stopFailed"))
        }

    @Test
    fun `runtime error offers only remove from list`() =
        runTest {
            val world = world()
            val controller = start(world)
            world.runtime.failNext(RuntimeError.Timeout)
            requestRemove(controller)
            controller.state.value.dialog shouldBe Dialog.ConfirmRemove(project, sessions = null)

            act { controller.onEvent(SidebarEvent.StopAndRemove) }
            world.runtime.stops().shouldBeEmpty()
            world.projects.calls.shouldBeEmpty()
            controller.state.value.dialog shouldBe Dialog.ConfirmRemove(project, sessions = null)

            act { controller.onEvent(SidebarEvent.RemoveKeepSessions) }
            world.projects.calls shouldBe listOf("remove project0001")
            controller.state.value.dialog
                .shouldBeNull()
        }

    @Test
    fun `remove error shows a fixed message and keeps the project`() =
        runTest {
            val world = world()
            val controller = start(world)
            requestRemove(controller)
            val failure = ProjectError.StorageFailed(StorageProblem.WRITE_FAILED)
            world.projects.failNext(failure)

            act { controller.onEvent(SidebarEvent.RemoveKeepSessions) }

            controller.state.value.projects shouldBe listOf(project)
            controller.state.value.dialog shouldBe Dialog.Message(ProjectMessages.of(failure))
        }

    @Test
    fun `cancel remove changes nothing`() =
        runTest {
            val world = world()
            val controller = start(world)
            requestRemove(controller)

            act { controller.onEvent(SidebarEvent.Dismiss) }

            controller.state.value shouldBe ProjectsUiState(projects = listOf(project), selected = project.id)
            world.projects.calls.shouldBeEmpty()
            world.runtime.stops().shouldBeEmpty()
        }

    @Test
    fun `remove selected without selection does nothing`() =
        runTest {
            val world = world()
            val controller = start(world)

            act { controller.onIntent(ProjectsIntent.RemoveSelectedRequested) }

            controller.state.value shouldBe ProjectsUiState(projects = listOf(project))
            world.runtime.calls.shouldBeEmpty()
            world.projects.calls.shouldBeEmpty()
        }
}
