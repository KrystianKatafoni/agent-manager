package dev.agentmanager.app

import dev.agentmanager.commandpalette.api.CommandRegistry
import dev.agentmanager.projects.api.ProjectInspector
import dev.agentmanager.projects.api.ProjectsApi
import dev.agentmanager.sessions.api.SessionsApi
import dev.agentmanager.terminal.api.TerminalApi

/** The modules the window is built from; keeps [MainWindow] within the parameter limit of detekt. */
internal class AppModules(
    val projects: ProjectsApi,
    val sessions: SessionsApi,
    val inspector: ProjectInspector,
    val registry: CommandRegistry,
    val terminal: TerminalApi,
    val shellLabel: String,
)
