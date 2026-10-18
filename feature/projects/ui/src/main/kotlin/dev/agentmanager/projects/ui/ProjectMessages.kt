package dev.agentmanager.projects.ui

import dev.agentmanager.projects.api.ProjectError
import dev.agentmanager.projects.api.StorageIssue

/** Fixed texts for errors and storage issues; never contain a path or a name (ENG-C8). */
internal object ProjectMessages {
    fun of(error: ProjectError): UiText = TODO("AM-304: ${error::class.simpleName}")

    fun of(issue: StorageIssue): UiText = TODO("AM-304: $issue")
}
