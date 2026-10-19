package dev.agentmanager.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import dev.agentmanager.commandpalette.ui.CommandPalette
import dev.agentmanager.commandpalette.ui.CommandPaletteController
import dev.agentmanager.missioncontrol.ui.GlobalViews
import dev.agentmanager.missioncontrol.ui.MissionControl
import dev.agentmanager.missioncontrol.ui.MissionControlController
import dev.agentmanager.projects.ui.ProjectsController
import dev.agentmanager.projects.ui.ProjectsIntent
import dev.agentmanager.projects.ui.ProjectsSidebar
import kotlinx.coroutines.flow.filterNotNull

/**
 * The window content: the projects sidebar with the global views under it, next to Mission Control (AM-304,
 * AM-405, AM-407). A shown global view clears the project in the sidebar (AM-407, decision 1).
 */
@Composable
internal fun AgentManagerApp(
    projects: ProjectsController,
    missionControl: MissionControlController,
    palette: CommandPaletteController,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(projects, missionControl) {
        missionControl.view.filterNotNull().collect { projects.onIntent(ProjectsIntent.ClearSelection) }
    }
    Row(modifier = modifier.fillMaxSize()) {
        Column {
            ProjectsSidebar(projects, Modifier.weight(weight = 1f))
            GlobalViews(missionControl)
        }
        MissionControl(missionControl, Modifier.weight(weight = 1f))
        CommandPalette(palette)
    }
}
