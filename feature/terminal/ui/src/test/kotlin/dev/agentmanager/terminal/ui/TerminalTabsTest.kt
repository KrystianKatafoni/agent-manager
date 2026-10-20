package dev.agentmanager.terminal.ui

import dev.agentmanager.terminal.ui.TerminalEvent.Close
import dev.agentmanager.terminal.ui.TerminalEvent.Exited
import dev.agentmanager.terminal.ui.TerminalEvent.Opened
import dev.agentmanager.terminal.ui.TerminalEvent.Select
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** Tabs in a single pane (plan AM-503, table 1, decisions 1 to 3). */
class TerminalTabsTest {
    private val t1 = aTab(1)
    private val t2 = aTab(2)

    @Test
    fun `first tab opens the first pane and activates it`() {
        val state = stateAfter(Opened(t1))

        state.tabs.map { it.id } shouldBe listOf(tabId(1))
        state.layout shouldBe leaf(1, 1)
        state.active shouldBe pane(1)
        state.maximized shouldBe false
        state.error shouldBe null
    }

    @Test
    fun `second tab takes the pane and hides the first`() {
        val state = stateAfter(Opened(t1), Opened(t2))

        state.tabs.map { it.id } shouldBe listOf(tabId(1), tabId(2))
        state.layout shouldBe leaf(1, 2)
        state.active shouldBe pane(1)
    }

    @Test
    fun `selecting a hidden tab shows it in the active pane`() {
        val state = stateAfter(Opened(t1), Opened(t2), Select(tabId(1)))

        state.layout shouldBe leaf(1, 1)
        state.tabs.size shouldBe 2
    }

    @Test
    fun `closing the shown tab shows its next neighbour in the group`() {
        val state =
            stateAfter(Opened(aTab(1)), Opened(aTab(2)), Opened(aTab(3)), Select(tabId(2)), Close(tabId(2)))

        state.layout shouldBe leaf(1, 3)
        state.tabs.map { it.id.value } shouldBe listOf(1, 3)
    }

    @Test
    fun `closing the last tab of a group shows the last tab of another group`() {
        val state =
            stateAfter(
                Opened(aTab(1, ALPHA)),
                Opened(aTab(2, BETA)),
                Opened(aTab(3, BETA)),
                Select(tabId(1)),
                Close(tabId(1)),
            )

        state.layout shouldBe leaf(1, 3)
        state.tabs.map { it.id.value } shouldBe listOf(2, 3)
    }

    @Test
    fun `closing a hidden tab keeps the layout`() {
        val before = stateAfter(Opened(t1), Opened(t2))

        val state = TerminalState.reduce(before, Close(tabId(1)))

        state.layout shouldBe leaf(1, 2)
        state.active shouldBe pane(1)
        state.tabs.map { it.id.value } shouldBe listOf(2)
    }

    @Test
    fun `closing the only tab empties the layout`() {
        val state = stateAfter(Opened(t1), Close(tabId(1)))

        state.tabs shouldBe emptyList()
        state.layout shouldBe null
        state.active shouldBe null
    }

    @Test
    fun `exit records the code and keeps the tab shown`() {
        val state = stateAfter(Opened(t1), Exited(tabId(1), 3))

        state.tab(1).exitCode shouldBe 3
        state.layout shouldBe leaf(1, 1)
    }

    @Test
    fun `unknown tab ids change nothing`() {
        val before = stateAfter(Opened(t1), Opened(t2))

        TerminalState.reduce(before, Select(tabId(9))) shouldBe before
        TerminalState.reduce(before, Close(tabId(9))) shouldBe before
        TerminalState.reduce(before, Exited(tabId(9), 1)) shouldBe before
    }
}
