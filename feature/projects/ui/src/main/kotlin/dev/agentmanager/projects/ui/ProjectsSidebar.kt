package dev.agentmanager.projects.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.agentmanager.projects.api.Project

/** The sidebar with its dialogs; stateless apart from [controller]. */
@Composable
public fun ProjectsSidebar(
    controller: ProjectsController,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsState()
    // The dialog popup sits outside the Column, so it takes no slot in its spacing.
    Box(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .width(width = 260.dp)
                    .fillMaxHeight()
                    .background(color = SidebarColors.background)
                    .padding(all = 12.dp)
                    .testTag(ProjectsTags.SIDEBAR),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            Label(ProjectsStrings["sidebar.title"], SidebarColors.secondaryText)
            state.banner?.let { Banner(it) { controller.onEvent(SidebarEvent.DismissBanner) } }
            ProjectList(state, controller)
            TextButton(ProjectsStrings["sidebar.add"], ProjectsTags.ADD_BUTTON) {
                controller.onIntent(ProjectsIntent.AddRequested)
            }
        }
        state.dialog?.let { ProjectsDialog(it, controller::onEvent) }
    }
}

@Composable
private fun ColumnScope.ProjectList(
    state: ProjectsUiState,
    controller: ProjectsController,
) {
    val list = Modifier.weight(weight = 1f).fillMaxWidth()
    if (state.projects.isEmpty()) {
        Box(modifier = list.testTag(ProjectsTags.EMPTY), contentAlignment = Alignment.Center) {
            Label(ProjectsStrings["sidebar.empty"], SidebarColors.secondaryText)
        }
    } else {
        Column(modifier = list.verticalScroll(rememberScrollState())) {
            state.projects.forEach { project ->
                ProjectRow(
                    project = project,
                    selected = project.id == state.selected,
                    onSelect = { controller.onEvent(SidebarEvent.Select(project.id)) },
                    onRemove = {
                        controller.onEvent(SidebarEvent.Select(project.id))
                        controller.onIntent(ProjectsIntent.RemoveSelectedRequested)
                    },
                )
            }
        }
    }
}

@Composable
private fun ProjectRow(
    project: Project,
    selected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(ProjectsTags.row(project.id))
                .selectable(selected = selected, onClick = onSelect)
                .background(color = if (selected) SidebarColors.selection else Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = project.displayName,
            modifier = Modifier.weight(weight = 1f),
            style = TextStyle(color = SidebarColors.text, fontSize = 14.sp),
            maxLines = 1,
        )
        RowMenu(project, onRemove)
    }
}

/** The ⋯ button of a row with its one-item menu. */
@Composable
private fun RowMenu(
    project: Project,
    onRemove: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton("⋯", ProjectsTags.rowMenu(project.id)) { open = true }
        if (open) {
            Popup(
                alignment = Alignment.TopEnd,
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                TextButton(ProjectsStrings["row.remove"], ProjectsTags.ROW_MENU_REMOVE) {
                    open = false
                    onRemove()
                }
            }
        }
    }
}

@Composable
private fun Banner(
    text: UiText,
    onDismiss: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(color = SidebarColors.warning)
                .padding(all = 8.dp)
                .testTag(ProjectsTags.BANNER),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Label(ProjectsStrings.format(text), SidebarColors.text)
        TextButton(ProjectsStrings["banner.dismiss"], ProjectsTags.BANNER_DISMISS, onDismiss)
    }
}

/** A clickable text: Material is not a dependency (AM-304 plan, decision 4). */
@Composable
internal fun TextButton(
    text: String,
    tag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .testTag(tag)
                .clickable(role = Role.Button, onClick = onClick)
                .background(color = SidebarColors.button)
                .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        BasicText(text = text, style = TextStyle(color = SidebarColors.text, fontSize = 13.sp))
    }
}

@Composable
internal fun Label(
    text: String,
    color: Color = SidebarColors.text,
) {
    BasicText(text = text, style = TextStyle(color = color, fontSize = 13.sp))
}

/** Placeholder colors until `core:design-system` and `AmTheme` exist (AM-802). */
internal object SidebarColors {
    val background = Color(color = 0xFF2C2C2E)
    val selection = Color(color = 0xFF3A3A3C)
    val button = Color(color = 0xFF48484A)
    val warning = Color(color = 0xFF5C4400)
    val scrim = Color(color = 0x99000000)
    val dialog = Color(color = 0xFF2C2C2E)
    val text = Color(color = 0xFFF2F2F7)
    val secondaryText = Color(color = 0xFF8E8E93)
}
