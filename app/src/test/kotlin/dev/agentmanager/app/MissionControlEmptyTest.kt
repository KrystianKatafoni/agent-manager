package dev.agentmanager.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import org.junit.jupiter.api.Test

/** UI test of the first screen; the full functional scenario moves to `tests:functional` (AM-904). */
@OptIn(ExperimentalTestApi::class)
class MissionControlEmptyTest {
    @Test
    fun `shows the empty state when there are no projects`() =
        runComposeUiTest {
            setContent { AgentManagerApp() }

            onNodeWithTag(TestTags.EMPTY_STATE).assertIsDisplayed()
            onNodeWithText("Brak projektów").assertIsDisplayed()
            onNodeWithText(Strings["missionControl.empty.hint"]).assertIsDisplayed()
        }
}
