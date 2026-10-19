package dev.agentmanager.app

import dev.agentmanager.commandpalette.api.Command
import dev.agentmanager.commandpalette.api.CommandGroup
import dev.agentmanager.commandpalette.api.CommandId
import dev.agentmanager.commandpalette.api.Shortcut
import dev.agentmanager.missioncontrol.ui.MissionControlIntent
import dev.agentmanager.missioncontrol.ui.MissionControlIntent.NextSession
import dev.agentmanager.missioncontrol.ui.MissionControlIntent.PreviousSession
import dev.agentmanager.missioncontrol.ui.MissionControlIntent.RemoveSelected
import dev.agentmanager.missioncontrol.ui.MissionControlIntent.RespawnSelected
import dev.agentmanager.missioncontrol.ui.MissionControlIntent.SelectFilter
import dev.agentmanager.missioncontrol.ui.MissionControlIntent.StopSelected
import dev.agentmanager.missioncontrol.ui.SessionActionsState
import dev.agentmanager.missioncontrol.ui.SessionFilter
import dev.agentmanager.projects.api.Project
import dev.agentmanager.projects.ui.ProjectsMenuState

/** Session and navigation commands of the palette (AM-409, decision 1); none while a dialog is open. */
internal fun sessionCommands(
    projects: List<Project>,
    menu: ProjectsMenuState,
    actions: SessionActionsState,
    onIntent: (MissionControlIntent) -> Unit,
    openPalette: (CommandGroup) -> Unit,
): List<Command> {
    if (menu.dialogOpen || actions.dialogOpen) return emptyList()
    val session =
        listOf(
            sessionCommand("sessions.next", "command.nextSession", AppShortcuts.NEXT_SESSION, NextSession, onIntent),
            sessionCommand(
                "sessions.previous",
                "command.previousSession",
                AppShortcuts.PREVIOUS_SESSION,
                PreviousSession,
                onIntent,
            ),
            filterCommand("all", "command.filterAll", SessionFilter.ALL, onIntent),
            filterCommand("active", "command.filterActive", SessionFilter.ACTIVE, onIntent),
            filterCommand("finished", "command.filterFinished", SessionFilter.FINISHED, onIntent),
        )
    val all = selectedSessionActions(actions, onIntent) + session
    return if (projects.isEmpty()) all else all + switchProject(openPalette)
}

private fun sessionCommand(
    id: String,
    titleKey: String,
    shortcut: Shortcut?,
    intent: MissionControlIntent,
    onIntent: (MissionControlIntent) -> Unit,
): Command = Command(CommandId(id), Strings[titleKey], CommandGroup.SESSION, emptyList(), shortcut) { onIntent(intent) }

/** Stop, Respawn and Remove of the selected session, offered by what the session allows now (AM-410). */
private fun selectedSessionActions(
    actions: SessionActionsState,
    onIntent: (MissionControlIntent) -> Unit,
): List<Command> =
    listOfNotNull(
        sessionCommand(
            "sessions.stop",
            "command.stopSession",
            AppShortcuts.STOP_SESSION,
            StopSelected,
            onIntent,
        ).copy(keywords = listOf("stop")).takeIf { actions.canStop },
        sessionCommand(
            "sessions.respawn",
            "command.respawnSession",
            null,
            RespawnSelected,
            onIntent,
        ).copy(keywords = listOf("respawn", "wznów")).takeIf { actions.canRespawn },
        sessionCommand(
            "sessions.remove",
            "command.removeSession",
            null,
            RemoveSelected,
            onIntent,
        ).copy(keywords = listOf("remove", "rm", "usuń")).takeIf { actions.canRemove },
    )

private fun filterCommand(
    suffix: String,
    titleKey: String,
    filter: SessionFilter,
    onIntent: (MissionControlIntent) -> Unit,
): Command = sessionCommand("sessions.filter.$suffix", titleKey, null, SelectFilter(filter), onIntent)

private fun switchProject(openPalette: (CommandGroup) -> Unit): Command =
    Command(
        CommandId("projects.switch"),
        Strings["command.switchProject"],
        CommandGroup.PROJECT,
        listOf("switch", "projekt"),
        AppShortcuts.SWITCH_PROJECT,
    ) { openPalette(CommandGroup.GO_TO_PROJECT) }
