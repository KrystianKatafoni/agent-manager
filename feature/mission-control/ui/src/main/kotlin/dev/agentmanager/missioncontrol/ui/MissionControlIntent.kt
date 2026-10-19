package dev.agentmanager.missioncontrol.ui

/** Intents of Mission Control from the palette and window keys (MVI, docs/11 §1; ADR 0011). */
public sealed interface MissionControlIntent {
    /** `j`: selects the next visible session; without a selection the first; stays on the last (no wrap). */
    public data object NextSession : MissionControlIntent

    /** `k`: selects the previous visible session; without a selection the last; stays on the first. */
    public data object PreviousSession : MissionControlIntent

    /** Palette "Filter: …": shows only sessions that [filter] matches (FR-S9). */
    public data class SelectFilter(
        val filter: SessionFilter,
    ) : MissionControlIntent
}
