package dev.agentmanager.terminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** The bar of tabs, one group per project in the order of the first tab. [activeTab]: the tab of the active pane. */
@Composable
internal fun TerminalTabBar(
    groups: List<TabGroup>,
    activeTab: TabId?,
    controller: TerminalController,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(color = TerminalColors.panel)
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        groups.forEach { TabGroupChip(it, activeTab, controller) }
    }
}

/** The name of a project, its tabs and the "+" that opens a shell in that project. */
@Composable
internal fun TabGroupChip(
    group: TabGroup,
    activeTab: TabId?,
    controller: TerminalController,
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(
            text = group.project.name,
            modifier = Modifier.testTag(TerminalTags.group(group.project.id)),
            color = TerminalColors.secondary,
        )
        group.tabs.forEach { TabChip(it, selected = it.id == activeTab, controller) }
        GlyphButton(TerminalTags.groupAdd(group.project.id), glyph = "+") { controller.openShell(group.project) }
    }
}

/** A tab: its label (click shows it), the `exit N` marker of an exited process and the x that closes it. */
@Composable
internal fun TabChip(
    tab: TerminalTab,
    selected: Boolean,
    controller: TerminalController,
) {
    val shade = if (selected) TerminalColors.selection else TerminalColors.panel
    Row(modifier = Modifier.background(color = shade), verticalAlignment = Alignment.CenterVertically) {
        Label(
            text = tab.label,
            modifier =
                Modifier
                    .testTag(TerminalTags.tab(tab.id))
                    .focusProperties { canFocus = false } // else it would consume Space and Enter
                    .selectable(selected = selected, role = Role.Tab) { controller.select(tab.id) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        tab.exitCode?.let {
            Label(
                text = TerminalStrings.format("tab.exit", it.toString()),
                modifier = Modifier.testTag(TerminalTags.tabExit(tab.id)),
                color = TerminalColors.secondary,
                mono = true,
            )
        }
        GlyphButton(TerminalTags.tabClose(tab.id), glyph = "×") { controller.close(tab.id) }
    }
}
