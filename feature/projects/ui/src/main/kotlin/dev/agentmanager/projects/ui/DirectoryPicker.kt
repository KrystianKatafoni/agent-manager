package dev.agentmanager.projects.ui

import java.nio.file.Path

/** Opens the system folder chooser. Must be called on the UI thread; null = the user cancelled. */
public fun interface DirectoryPicker {
    public suspend fun pick(): Path?
}
