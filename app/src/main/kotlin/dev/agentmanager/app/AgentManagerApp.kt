package dev.agentmanager.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.agentmanager.projects.ui.ProjectsController

/** Root of the window content. Mission Control moves to `feature:mission-control:ui` once it has data. */
@Composable
internal fun AgentManagerApp(modifier: Modifier = Modifier) {
    MissionControlEmpty(modifier)
}

/** The window content: the projects sidebar next to Mission Control (AM-304). */
@Composable
internal fun AgentManagerApp(
    projects: ProjectsController,
    modifier: Modifier = Modifier,
): Unit = TODO("AM-304: $projects $modifier")

/** Mission Control before any project is added. */
@Composable
internal fun MissionControlEmpty(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(color = AppColors.background).testTag(TestTags.EMPTY_STATE),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            BasicText(
                text = Strings["missionControl.empty.title"],
                style = TextStyle(color = AppColors.text, fontSize = 20.sp),
            )
            BasicText(
                text = Strings["missionControl.empty.hint"],
                style = TextStyle(color = AppColors.secondaryText, fontSize = 14.sp),
            )
        }
    }
}

/** Placeholder colors until `core:design-tokens` and appearance themes exist. */
internal object AppColors {
    val background = Color(color = 0xFF1C1C1E)
    val text = Color(color = 0xFFF2F2F7)
    val secondaryText = Color(color = 0xFF8E8E93)
}

/** Semantics tags used by UI tests. */
internal object TestTags {
    const val EMPTY_STATE = "missionControl.empty"
}
