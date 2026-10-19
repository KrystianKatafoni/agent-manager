package dev.agentmanager.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.agentmanager.missioncontrol.ui.MissionControl
import dev.agentmanager.missioncontrol.ui.MissionControlController
import dev.agentmanager.projects.ui.ProjectsController
import dev.agentmanager.projects.ui.ProjectsSidebar

/**
 * The window content: the projects sidebar next to Mission Control (AM-304, AM-405).
 */
@Composable
internal fun AgentManagerApp(
    projects: ProjectsController,
    missionControl: MissionControlController,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        ProjectsSidebar(projects)
        MissionControl(missionControl, Modifier.weight(weight = 1f))
    }
}
