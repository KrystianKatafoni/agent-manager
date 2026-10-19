package dev.agentmanager.app

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import dev.agentmanager.agentruntime.api.FakeAgentRuntime
import dev.agentmanager.core.domain.NoOpLogger
import dev.agentmanager.core.testing.TestClock
import dev.agentmanager.missioncontrol.ui.MissionControlController
import dev.agentmanager.projects.api.FakeProjectInspector
import dev.agentmanager.projects.api.FakeProjectsApi
import dev.agentmanager.projects.ui.DirectoryPicker
import dev.agentmanager.projects.ui.ProjectsController
import dev.agentmanager.sessions.api.FakeSessions
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours

/**
 * UI test of the window on fakes; the empty state itself is tested in `feature:mission-control:ui`. The full
 * functional scenario moves to `tests:functional` (AM-904).
 */
@OptIn(ExperimentalTestApi::class)
class MissionControlEmptyTest {
    @Test
    fun `window shows the sidebar next to Mission Control`() =
        runComposeUiTest {
            // Given the window on fakes, both controllers in the composition scope like the window scope
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
                val missionControl =
                    remember {
                        // A one-hour tick: the age loop must not keep the UI test busy.
                        val selection = controller.selection
                        MissionControlController(FakeSessions(), projects, selection, TestClock(), scope, 1.hours)
                    }
                AgentManagerApp(projects = controller, missionControl = missionControl)
            }

            // Then Mission Control is on the right of the sidebar and says there are no projects
            waitUntilExactlyOneExists(hasTestTag(MISSION_CONTROL_TAG))
            val sidebar = onNodeWithTag(SIDEBAR_TAG).assertIsDisplayed().getBoundsInRoot()
            val missionControl = onNodeWithTag(MISSION_CONTROL_TAG).assertIsDisplayed().getBoundsInRoot()
            (sidebar.right <= missionControl.left) shouldBe true
            onAllNodes(hasText("Brak projektów") and hasAnyAncestor(hasTestTag(MISSION_CONTROL_TAG)))
                .onFirst()
                .assertIsDisplayed()
        }

    private companion object {
        /** `ProjectsTags.SIDEBAR`, internal to `feature:projects:ui`. */
        const val SIDEBAR_TAG = "projects.sidebar"

        /** `MissionControlTags.ROOT`, internal to `feature:mission-control:ui`. */
        const val MISSION_CONTROL_TAG = "missionControl"
    }
}
