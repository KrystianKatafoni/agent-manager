package dev.agentmanager.app

import dev.agentmanager.commandpalette.api.Command
import dev.agentmanager.commandpalette.api.CommandGroup
import dev.agentmanager.commandpalette.api.CommandId
import dev.agentmanager.commandpalette.api.CommandProvider
import dev.agentmanager.core.domain.Logger
import dev.agentmanager.core.domain.SessionId
import dev.agentmanager.core.domain.ShellCommand
import dev.agentmanager.core.domain.info
import dev.agentmanager.missioncontrol.ui.MissionControlController
import dev.agentmanager.missioncontrol.ui.SessionActionsState
import dev.agentmanager.projects.ui.ProjectsController
import dev.agentmanager.projects.ui.ProjectsMenuState
import dev.agentmanager.terminal.ui.TerminalController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The attach command of the palette (`⏎`); empty unless [SessionActionsState.canAttach] and no dialog is open. */
internal fun attachCommands(
    dialogOpen: Boolean,
    actions: SessionActionsState,
    attach: () -> Unit,
): List<Command> {
    if (dialogOpen || actions.dialogOpen || !actions.canAttach) return emptyList()
    val command =
        Command(
            id = CommandId("sessions.attach"),
            title = Strings["command.attachSession"],
            group = CommandGroup.SESSION,
            keywords = listOf("attach", "terminal", "podłącz"),
            shortcut = AppShortcuts.ATTACH_SESSION,
        ) { attach() }
    return listOf(command)
}

/** Opens `claude attach` of the selected session in a new terminal tab of its project (AM-505). */
internal class SessionAttacher(
    private val missionControl: MissionControlController,
    private val terminal: TerminalController,
    private val attachCommand: (SessionId) -> ShellCommand,
    private val logger: Logger,
) {
    /** Opens `claude attach` of [MissionControlController.attachTarget] in a new tab of its project; no-op without. */
    fun attachSelected() {
        val target = missionControl.attachTarget() ?: return
        logger.info("session attach", "sessionId" to target.session.value)
        terminal.openCommand(
            terminalProjectOf(target.project),
            Strings.format("terminal.attachTab", target.name),
            attachCommand(target.session),
        )
    }
}

/**
 * Offers [attachCommands] for the project and Mission Control dialogs and the selected session.
 * Availability follows `actions.canAttach`, which comes after `stateIn` and may lag one frame behind on the first
 * selection. The attach itself reads its target afresh through `attachTarget()`, so the opposite case
 * (a dialog and `⏎` in the same frame) is a safe no-op.
 */
internal class AttachCommands(
    controller: ProjectsController,
    missionControl: MissionControlController,
    attacher: SessionAttacher,
    scope: CoroutineScope,
) : CommandProvider {
    private val offered =
        { menu: ProjectsMenuState, actions: SessionActionsState ->
            attachCommands(menu.dialogOpen, actions, attacher::attachSelected)
        }

    override val commands: StateFlow<List<Command>> =
        combine(controller.menu, missionControl.actions, offered)
            .stateIn(scope, SharingStarted.Eagerly, offered(controller.menu.value, missionControl.actions.value))
}
