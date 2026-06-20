package com.project.wellbeingapp.ui.history

import com.project.wellbeingapp.score.AppUsage

/**
 * Stundenweise Zusammenfassung des heutigen Verlaufs: die 6 Zehn-Minuten-Fenster
 * einer Stunde werden zu Punkten, Nutzung und App-Aufschlüsselung aufsummiert.
 */
data class HourSummary(
    val startMillis: Long,
    val usageMillis: Long,
    val points: Int,
    /** Genutzte Apps in dieser Stunde (absteigend nach Dauer), für die Aufklapp-Ansicht. */
    val apps: List<AppUsage> = emptyList()
)

/** Netto-Punkte eines vergangenen Tages (aus dem Archiv `score_history`). */
data class DaySummary(
    val dateMillis: Long,
    val points: Int
)

/** Hält den heutigen Stunden-Verlauf (neueste zuerst) für den Verlauf-Screen. */
data class HistoryUiState(
    val hours: List<HourSummary> = emptyList(),
    /** Tages-Zusammenfassungen vergangener Tage (neueste zuerst). */
    val daySummaries: List<DaySummary> = emptyList(),
    val isLoading: Boolean = true
) {
    /** Summe der Punkte über alle heutigen Stunden. */
    val netPoints: Int get() = hours.sumOf { it.points }

    /** Punkte heute plus alle archivierten Tage = Gesamt seit App-Start. */
    val totalPoints: Int get() = netPoints + daySummaries.sumOf { it.points }
}
