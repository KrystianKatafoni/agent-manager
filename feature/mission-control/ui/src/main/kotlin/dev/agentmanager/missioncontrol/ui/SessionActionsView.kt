package dev.agentmanager.missioncontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Stop, Respawn and Remove buttons of the details panel, then the "in progress" text or the last failure. */
@Composable
internal fun SessionActionsView(
    details: SessionDetails,
    onRun: (SessionAction) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(space = 6.dp)) {
        SessionAction.entries.filter { it in details.actions }.forEach { action ->
            Box(
                modifier =
                    Modifier
                        .testTag(tagOf(action))
                        .clickable(role = Role.Button) { onRun(action) }
                        .background(color = Colors.button)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Label(MissionControlStrings["action.${action.name.lowercase()}"])
            }
        }
    }
    details.busy?.let {
        Box(Modifier.testTag(MissionControlTags.DETAILS_BUSY)) {
            Label(MissionControlStrings["action.${it.name.lowercase()}.busy"], Colors.secondary)
        }
    }
    details.failure?.let {
        Column(Modifier.testTag(MissionControlTags.DETAILS_FAILURE)) {
            Label(MissionControlStrings["action.${it.action.name.lowercase()}.failed"], Colors.of(Tone.ERR))
            Label(MissionControlStrings.format(it.reason), Colors.of(Tone.ERR))
        }
    }
}

private fun tagOf(action: SessionAction): String =
    when (action) {
        SessionAction.STOP -> MissionControlTags.DETAILS_STOP
        SessionAction.RESPAWN -> MissionControlTags.DETAILS_RESPAWN
        SessionAction.REMOVE -> MissionControlTags.DETAILS_REMOVE
    }
