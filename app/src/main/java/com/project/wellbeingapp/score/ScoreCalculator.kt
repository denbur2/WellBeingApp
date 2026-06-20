package com.project.wellbeingapp.score

import kotlinx.coroutines.flow.Flow

/**
 * Berechnet den Wellbeing-Score aus app_usage_entries.
 *
 * Zwei bewusst getrennte Wege:
 *  - [observeCurrentScore] = Live-Wert für heute, NICHT gespeichert.
 *  - [snapshotDay]         = aggregiert einen Tag und schreibt 1 Zeile
 *                            in score_history (Archiv, bleibt erhalten).
 */
interface ScoreCalculator {
    fun observeCurrentScore(): Flow<ScoreData>

    /**
     * Aufschlüsselung des heutigen Scores in einzelne 10-Min-Fenster
     * (neueste zuerst) — macht nachvollziehbar, wann Punkte gesammelt
     * bzw. verloren wurden.
     */
    fun observeWindowHistory(): Flow<List<ScoreWindow>>

    suspend fun snapshotDay(date: Long)
}

/** Live-Score ohne Datum — das Datum kommt erst beim Snapshot dazu. */
data class ScoreData(
    val score: Int,
    val level: Int,
    val screenTimeMinutes: Int
)

/**
 * Ein einzelnes 10-Min-Bewertungsfenster für den Verlaufs-Screen.
 * @param startMillis Beginn des Fensters (Epoch-ms).
 * @param usageMillis erfasste Bildschirmzeit im Fenster (ohne die eigene App).
 * @param delta Punkte-Beitrag dieses Fensters (+1 / 0 / −1).
 * @param apps App-Aufschlüsselung im Fenster (ohne die eigene App).
 */
data class ScoreWindow(
    val startMillis: Long,
    val usageMillis: Long,
    val delta: Int,
    val apps: List<AppUsage> = emptyList()
)

/** Nutzung einer einzelnen App über einen Zeitraum (für die Aufschlüsselung). */
data class AppUsage(
    val appName: String,
    val durationMillis: Long
)
