@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.agentmanager.projects.ui

import dev.agentmanager.agentruntime.api.FakeAgentRuntime
import dev.agentmanager.agentruntime.api.RuntimeError
import dev.agentmanager.agentruntime.api.RuntimePhase
import dev.agentmanager.agentruntime.api.RuntimeWorld
import dev.agentmanager.agentruntime.api.aRuntimeSession
import dev.agentmanager.core.testing.aSessionId
import dev.agentmanager.projects.api.FakeProjectsApi
import dev.agentmanager.projects.api.StorageIssue
import dev.agentmanager.projects.api.aProject
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.nio.file.Path

/** List, selection, window menu, storage banner and logging of the sidebar controller (AM-304). */
class ProjectsControllerTest {
    @Test
    fun `the list follows the projects api and nothing is selected at first`() =
        runTest {
            val world = ControllerWorld(projects = FakeProjectsApi(initial = listOf(aProject())))
            val controller = start(world)
            controller.state.value shouldBe ProjectsUiState(projects = listOf(aProject()))

            world.projects.remove(aProject().id)
            runCurrent()

            controller.state.value.projects shouldBe emptyList()
        }

    @Test
    fun `menu disables remove without selection and while a dialog is open`() =
        runTest {
            val world = ControllerWorld(projects = FakeProjectsApi(initial = listOf(aProject())))
            val controller = start(world)
            controller.menu.value shouldBe ProjectsMenuState(canAdd = true, canRemove = false)

            act { controller.onEvent(SidebarEvent.Select(aProject().id)) }
            controller.state.value.selected shouldBe aProject().id
            controller.menu.value shouldBe ProjectsMenuState(canAdd = true, canRemove = true)

            act { controller.onIntent(ProjectsIntent.RemoveSelectedRequested) }
            controller.menu.value shouldBe ProjectsMenuState(canAdd = false, canRemove = false)

            act { controller.onEvent(SidebarEvent.Dismiss) }
            controller.menu.value shouldBe ProjectsMenuState(canAdd = true, canRemove = true)
        }

    @Test
    fun `storage issue banner stays until dismissed`() =
        runTest {
            val world =
                ControllerWorld(projects = FakeProjectsApi(storageIssue = StorageIssue.RECOVERED_FROM_BROKEN_FILE))
            val controller = start(world)
            val banner = ProjectMessages.of(StorageIssue.RECOVERED_FROM_BROKEN_FILE)
            controller.state.value.banner shouldBe banner

            world.picker.willCancel()
            act { controller.onIntent(ProjectsIntent.AddRequested) }
            controller.state.value.banner shouldBe banner

            act { controller.onEvent(SidebarEvent.DismissBanner) }
            controller.state.value.banner
                .shouldBeNull()
        }

    @Test
    fun `no storage issue shows no banner`() =
        runTest {
            val controller = start(ControllerWorld())

            controller.state.value.banner
                .shouldBeNull()
        }

    @Test
    fun `logs hold project ids only, never paths or names`() =
        runTest {
            val dir = Path.of("/Users/$SECRET/projects/$SECRET-name")
            val session = aRuntimeSession(id = aSessionId("working"), cwd = dir, phase = RuntimePhase.WORKING)
            val world = ControllerWorld(runtime = FakeAgentRuntime(RuntimeWorld(sessions = listOf(session))))
            val controller = start(world)

            world.picker.willPick(dir)
            act { controller.onIntent(ProjectsIntent.AddRequested) }
            act { controller.onEvent(SidebarEvent.ConfirmAdd) }
            act { controller.onIntent(ProjectsIntent.RemoveSelectedRequested) }
            world.runtime.failNext(RuntimeError.CommandFailed(exitCode = 1, stderrTail = SECRET))
            act { controller.onEvent(SidebarEvent.StopAndRemove) }
            act { controller.onEvent(SidebarEvent.Dismiss) }
            act { controller.onIntent(ProjectsIntent.RemoveSelectedRequested) }
            act { controller.onEvent(SidebarEvent.RemoveKeepSessions) }

            world.projects.projects.value shouldBe emptyList()
            world.logger.entries.shouldNotBeEmpty()
            world.logger.entries.forEach { entry ->
                withClue(entry) {
                    entry.message shouldNotContain SECRET
                    entry.fields.values.forEach { it.toString() shouldNotContain SECRET }
                    entry.cause?.message.orEmpty() shouldNotContain SECRET
                }
            }
            world.logger.entries.any { it.fields["projectId"]?.toString() == "project0001" } shouldBe true
        }

    private companion object {
        const val SECRET = "secret-user"
    }
}
