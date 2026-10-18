package dev.agentmanager.projects.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * The open [dialog] over the whole window, inside it rather than a separate window (AM-304 plan, decision 4).
 * Escape is "Cancel"; the controller ignores it while a confirmed command runs. Nothing is shown while the
 * system folder chooser is open ([Dialog.Picking]).
 */
@Composable
internal fun ProjectsDialog(
    dialog: Dialog,
    onEvent: (SidebarEvent) -> Unit,
) {
    if (dialog == Dialog.Picking) return
    Popup(
        popupPositionProvider = WholeWindow,
        onDismissRequest = { onEvent(SidebarEvent.Dismiss) },
        properties = PopupProperties(focusable = true),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(color = SidebarColors.scrim),
            contentAlignment = Alignment.Center,
        ) {
            DialogContent(dialog, onEvent)
        }
    }
}

@Composable
private fun DialogContent(
    dialog: Dialog,
    onEvent: (SidebarEvent) -> Unit,
) {
    val cancel = { onEvent(SidebarEvent.Dismiss) }
    when (dialog) {
        // Not shown, see ProjectsDialog.
        Dialog.Picking -> Unit

        is Dialog.Waiting -> Waiting(dialog, onCancel = cancel.takeIf { dialog != Dialog.Waiting.BUSY })

        is Dialog.ConfirmAdd -> ConfirmAdd(dialog, onEvent)

        is Dialog.ConfirmRemove -> ConfirmRemove(dialog, onEvent)

        is Dialog.Message -> Message(dialog.text, cancel)
    }
}

/** A dialog waiting for a step to finish; [onCancel] null = the step cannot be cancelled. */
@Composable
private fun Waiting(
    step: Dialog.Waiting,
    onCancel: (() -> Unit)?,
) {
    Panel(step.tag, ProjectsStrings[step.key]) { onCancel?.let { Cancel(it) } }
}

@Composable
private fun Message(
    text: UiText,
    onClose: () -> Unit,
) {
    Panel(ProjectsTags.MESSAGE, ProjectsStrings.format(text)) {
        TextButton(ProjectsStrings["dialog.close"], ProjectsTags.CANCEL, onClose)
    }
}

@Composable
private fun ConfirmAdd(
    dialog: Dialog.ConfirmAdd,
    onEvent: (SidebarEvent) -> Unit,
) {
    Panel(ProjectsTags.CONFIRM_ADD, ProjectsStrings["add.title"]) {
        // The path is user data: shown here, never logged (ENG-C8, KDoc of `ProjectError`).
        Label(dialog.report.dir.toString())
        ReportSummary.lines(dialog.report).forEach { Label(ProjectsStrings.format(it), SidebarColors.secondaryText) }
        dialog.error?.let { Label(ProjectsStrings.format(it)) }
        TextButton(ProjectsStrings["add.confirm"], ProjectsTags.ADD) { onEvent(SidebarEvent.ConfirmAdd) }
        Cancel { onEvent(SidebarEvent.Dismiss) }
    }
}

@Composable
private fun ConfirmRemove(
    dialog: Dialog.ConfirmRemove,
    onEvent: (SidebarEvent) -> Unit,
) {
    val keepSessions = { onEvent(SidebarEvent.RemoveKeepSessions) }
    Panel(ProjectsTags.CONFIRM_REMOVE, ProjectsStrings["remove.title"]) {
        Label(dialog.project.displayName)
        val sessions = dialog.sessions
        when {
            sessions == null -> {
                Label(ProjectsStrings["remove.sessionsUnknown"])
                TextButton(ProjectsStrings["remove.removeFromList"], ProjectsTags.REMOVE, keepSessions)
            }

            sessions.isEmpty() -> {
                TextButton(ProjectsStrings["remove.confirm"], ProjectsTags.REMOVE, keepSessions)
            }

            else -> {
                Label(ProjectsStrings.format(UiText("remove.activeSessions", listOf(sessions.size.toString()))))
                TextButton(ProjectsStrings["remove.stopAndRemove"], ProjectsTags.STOP_AND_REMOVE) {
                    onEvent(SidebarEvent.StopAndRemove)
                }
                TextButton(ProjectsStrings["remove.keepSessions"], ProjectsTags.REMOVE_KEEP_SESSIONS, keepSessions)
            }
        }
        Cancel { onEvent(SidebarEvent.Dismiss) }
    }
}

/** A dialog box tagged [tag]; merged for screen readers, so its texts are read as one. */
@Composable
private fun Panel(
    tag: String,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .width(width = 440.dp)
                .background(color = SidebarColors.dialog, shape = RoundedCornerShape(size = 10.dp))
                .padding(all = 16.dp)
                .testTag(tag)
                .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Label(title)
        content()
    }
}

@Composable
private fun Cancel(onClick: () -> Unit) {
    TextButton(ProjectsStrings["dialog.cancel"], ProjectsTags.CANCEL, onClick)
}

/** Places the dialog layer over the whole window, whatever the anchor. */
private object WholeWindow : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset.Zero
}
