package dev.agentmanager.app

/** Semantics tags of other modules that are `internal` there (the same trick as in `MissionControlEmptyTest`). */
internal object WindowTags {
    const val PALETTE = "palette"
    const val PALETTE_FIELD = "palette.field"

    /** The focused `Box` with `WindowKeys` that stands for `Window.onKeyEvent` in UI tests (see `showWindow`). */
    const val KEYS = "window.keys"
    const val DETAILS = "missionControl.details"
    const val DETAILS_STOP = "missionControl.details.stop"
    const val DETAILS_RESPAWN = "missionControl.details.respawn"
    const val DETAILS_REMOVE = "missionControl.details.remove"
    const val CONFIRM_REMOVE = "missionControl.confirmRemove"
    const val CONFIRM_REMOVE_OK = "missionControl.confirmRemove.ok"
    const val CONFIRM_REMOVE_CANCEL = "missionControl.confirmRemove.cancel"
    const val DETAILS_EMPTY = "missionControl.details.empty"
    const val SIDEBAR = "projects.sidebar"
    const val MODE_BAR = "missionControl.modeBar"
    const val HEADER = "missionControl.header"
    const val RENAME_DIALOG = "projects.dialog.rename"
    const val RENAME_FIELD = "projects.dialog.rename.field"
    const val RENAME_SAVE = "projects.dialog.rename.save"

    fun paletteRow(commandId: String): String = "palette.row.$commandId"

    fun projectRow(projectId: String): String = "projects.row.$projectId"

    /** The entry of a global view in the left column; [name] is `waiting`, `working` or `failed`. */
    fun view(name: String): String = "missionControl.view.$name"

    fun missionRow(sessionId: String): String = "missionControl.row.$sessionId"
}
