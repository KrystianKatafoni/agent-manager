package dev.agentmanager.missioncontrol.ui

import dev.agentmanager.core.testing.T0
import dev.agentmanager.sessions.api.RuntimeProblem
import dev.agentmanager.sessions.api.SessionState
import dev.agentmanager.sessions.api.SnapshotStatus
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.ZoneOffset

/** Texts of `i18n/missioncontrol.properties` (NFR-10): every key Mission Control can show exists and is filled in. */
internal class MissionControlStringsTest {
    @Test
    fun `every text key has a text`() {
        MissionControlStrings.keys shouldContainAll allKeys()
        allKeys().forEach { key -> withClue(key) { MissionControlStrings[key].shouldNotBeBlank() } }
    }

    @Test
    fun `arguments are filled into the text`() {
        argumentTexts().forEach { text ->
            withClue(text) { text.args.forEach { MissionControlStrings.format(text) shouldContain it } }
        }
    }

    @Test
    fun `every stale text starts with the time of the last listing`() {
        RuntimeProblem.entries.forEach { problem ->
            val text = Freshness.of(SnapshotStatus.Stale(T0, problem), ZoneOffset.UTC)
            withClue(problem) { MissionControlStrings.format(text) shouldStartWith "Nieaktualne od 09:00:00" }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixedTexts")
    fun `fixed texts say what the plan says`(
        key: String,
        expected: String,
    ) {
        MissionControlStrings[key] shouldBe expected
    }

    companion object {
        /** Fixed texts of the composables (not from [StateGlyph], [LastEvent], [AgeText] or [Freshness]). */
        val UI_KEYS: Set<String> =
            setOf(
                "empty.noProjects",
                "empty.noProjects.hint",
                "empty.noSelection",
                "header.sessions",
                "filter.all",
                "filter.active",
                "filter.finished",
                "list.loading",
                "details.none",
                "details.worktree",
                "details.id",
                "details.startedAt",
                "details.lastActivity",
                "modeBar.waiting",
                "modeBar.working",
            )

        private val states =
            listOf(
                SessionState.Working,
                SessionState.AwaitingInput("q"),
                SessionState.AwaitingPermission("Bash"),
                SessionState.AwaitingPermission(null),
                SessionState.Done,
                SessionState.Failed,
                SessionState.Stopped,
            )

        private val statuses: List<SnapshotStatus> =
            listOf(SnapshotStatus.Loading, SnapshotStatus.Fresh(T0)) +
                RuntimeProblem.entries.flatMap { listOf(SnapshotStatus.Stale(T0, it), SnapshotStatus.Stale(null, it)) }

        private val ages = listOf(0L, 5L, 120L, 7_200L, 172_800L)

        private fun generated(): List<UiText> =
            states.map { UiText(StateGlyph.of(it).key) } +
                states.map(LastEvent::of) +
                ages.map { AgeText.of(T0, T0.plusSeconds(it)) } +
                statuses.map { Freshness.of(it, ZoneOffset.UTC) } +
                statuses.map { Freshness.of(it, ZoneOffset.UTC, short = true) }

        private fun argumentTexts(): List<UiText> =
            generated().filter { it.args.isNotEmpty() } +
                listOf(
                    UiText("header.sessions", listOf("3")),
                    UiText("modeBar.waiting", listOf("2")),
                    UiText("modeBar.working", listOf("4")),
                )

        fun allKeys(): Set<String> = generated().map { it.key }.toSet() + UI_KEYS

        @JvmStatic
        fun fixedTexts(): List<Arguments> =
            listOf(
                arguments("empty.noProjects", "Brak projektów"),
                arguments("empty.noSelection", "Wybierz projekt"),
                arguments("filter.all", "Wszystkie"),
                arguments("filter.active", "Aktywne"),
                arguments("filter.finished", "Zakończone"),
                arguments("list.loading", "Wczytywanie sesji…"),
                arguments("details.none", "Wybierz sesję"),
                arguments("fresh.live", "na żywo"),
                arguments("fresh.loading", "wczytywanie"),
                arguments("fresh.short.stale", "nieaktualne od {0}"),
            )
    }
}
