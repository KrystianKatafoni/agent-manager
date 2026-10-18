package dev.agentmanager.app

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.agentmanager.agentruntime.api.FakeAgentRuntime
import dev.agentmanager.core.domain.NoOpLogger
import dev.agentmanager.projects.api.FakeProjectInspector
import dev.agentmanager.projects.api.FakeProjectsApi
import dev.agentmanager.projects.ui.DirectoryPicker
import dev.agentmanager.projects.ui.ProjectsController
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** UI test of the first screen; the full functional scenario moves to `tests:functional` (AM-904). */
@OptIn(ExperimentalTestApi::class)
class MissionControlEmptyTest {
    @Test
    fun `shows the empty state when there are no projects`() =
        runComposeUiTest {
            setContent { MissionControlEmpty() }

            onNodeWithTag(TestTags.EMPTY_STATE).assertIsDisplayed()
            onNodeWithText("Brak projektów").assertIsDisplayed()
            onNodeWithText(Strings["missionControl.empty.hint"]).assertIsDisplayed()
        }

    @Test
    fun `window shows the sidebar next to Mission Control`() =
        runComposeUiTest {
            // Given the window on fakes, the controller in the composition scope like the window scope
            setContent {
                val scope = rememberCoroutineScope()
                val projects = remember { FakeProjectsApi() }
                val controller =
                    remember {
                        ProjectsController(
                            projects = projects,
                            inspector = FakeProjectInspector(projects = projects),
                            runtime = FakeAgentRuntime(),
                            picker = DirectoryPicker { null },
                            scope = scope,
                            logger = NoOpLogger,
                        )
                    }
                AgentManagerApp(projects = controller)
            }

            // Then the sidebar is on the left of Mission Control
            val sidebar = onNodeWithTag(SIDEBAR_TAG).assertIsDisplayed().getBoundsInRoot()
            val missionControl = onNodeWithTag(TestTags.EMPTY_STATE).assertIsDisplayed().getBoundsInRoot()
            (sidebar.right <= missionControl.left) shouldBe true
        }

    private companion object {
        /** `ProjectsTags.SIDEBAR`, internal to `feature:projects:ui`. */
        const val SIDEBAR_TAG = "projects.sidebar"
    }
}
