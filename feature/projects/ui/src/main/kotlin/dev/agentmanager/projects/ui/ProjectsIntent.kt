package dev.agentmanager.projects.ui

/** Which window-menu commands are enabled; both false while a dialog is open, so ⌘⌫ reaches text fields. */
public data class ProjectsMenuState(
    val canAdd: Boolean,
    val canRemove: Boolean,
)

/** User intents from the sidebar and the window menu (MVI, docs/11 §1). */
public sealed interface ProjectsIntent {
    /** ⌘⇧O or the sidebar button: pick a folder, inspect it, confirm adding. */
    public data object AddRequested : ProjectsIntent

    /** ⌘⌫: remove the selected project after a confirmation; ignored without a selection. */
    public data object RemoveSelectedRequested : ProjectsIntent
}
