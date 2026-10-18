package dev.agentmanager.projects.ui

import dev.agentmanager.projects.api.DirectoryReport

/** Lines of the add dialog, in order: git, CLAUDE.md, sessions, hooks. */
internal object ReportSummary {
    fun lines(report: DirectoryReport): List<UiText> = TODO("AM-304: ${report.hooks}")
}
