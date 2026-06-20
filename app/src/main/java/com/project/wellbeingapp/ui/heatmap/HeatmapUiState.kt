package com.project.wellbeingapp.ui.heatmap

import com.project.wellbeingapp.heatmap.HeatmapApp
import com.project.wellbeingapp.heatmap.HeatmapCell
import com.project.wellbeingapp.ui.common.TimeRange

/** Hält nur, was die UI zum Zeichnen braucht — entkoppelt von der Domain. */
data class HeatmapUiState(
    val cells: List<HeatmapCell> = emptyList(),
    /** Auswählbare Apps (zusätzlich zur „Gesamt"-Option in der UI). */
    val availableApps: List<HeatmapApp> = emptyList(),
    /** Gewählte App; `null` = „Gesamt" (alle Apps kombiniert). */
    val selectedApp: String? = null,
    /** Aktuell gewählter Zeitraum (Tag/Woche). */
    val range: TimeRange = TimeRange.WEEK,
    val isLoading: Boolean = true
)
