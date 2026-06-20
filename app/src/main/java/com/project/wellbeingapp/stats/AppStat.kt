package com.project.wellbeingapp.stats

/** Aggregierte Nutzung einer App über einen Zeitraum. */
data class AppStat(
    val packageName: String,
    val appName: String,
    /** Aufsummierte Vordergrund-Zeit in Millisekunden. */
    val totalMs: Long
) {
    /** Auf ganze Minuten gerundete Nutzungszeit (für die Anzeige). */
    val minutes: Long get() = totalMs / 60_000L
}
