package com.project.wellbeingapp.ui.stats

import com.project.wellbeingapp.stats.AppStat
import com.project.wellbeingapp.ui.common.TimeRange

/** Hält nur, was der Stats-Screen zum Zeichnen braucht. */
data class StatsUiState(
    val apps: List<AppStat> = emptyList(),
    /** Zeit (ms), in der der Bildschirm im Zeitraum aus war; `null` = noch nicht ermittelt. */
    val screenOffMs: Long? = null,
    /** Aktuell gewählter Zeitraum (Tag/Woche). */
    val range: TimeRange = TimeRange.WEEK,
    val isLoading: Boolean = true
)
