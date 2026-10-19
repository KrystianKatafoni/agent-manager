package dev.agentmanager.missioncontrol.ui

import dev.agentmanager.core.domain.SessionId

/** Semantics tags of Mission Control for UI tests. */
internal object MissionControlTags {
    const val ROOT = "missionControl"
    const val EMPTY = "missionControl.empty"
    const val HEADER = "missionControl.header"
    const val LOADING = "missionControl.loading"
    const val BANNER = "missionControl.banner"
    const val DETAILS = "missionControl.details"
    const val DETAILS_EMPTY = "missionControl.details.empty"
    const val MODE_BAR = "missionControl.modeBar"

    /** The filter button of [filter]. */
    fun filter(filter: SessionFilter): String = "missionControl.filter.${filter.name.lowercase()}"

    /** The row of session [id]; clicking it selects the session. */
    fun row(id: SessionId): String = "missionControl.row.${id.value}"
}
