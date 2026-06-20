package com.project.wellbeingapp.services

/**
 * Liefert die App-Nutzung seit einem Zeitpunkt. Kapselt den Android
 * UsageStatsManager, damit der [TrackingCoordinator] testbar bleibt.
 */
interface AppUsageProvider {
    /** @param startTime Unix-Timestamp; @return Nutzung pro App seitdem. */
    suspend fun getUsageSince(startTime: Long): List<AppUsageSample>

    /**
     * Gesamtzeit in Millisekunden, in der der Bildschirm im Zeitraum [start, end]
     * ausgeschaltet (non-interactive) war. Liefert 0, wenn die System-Events
     * nicht verfügbar sind.
     */
    suspend fun getScreenOffMillis(start: Long, end: Long): Long
}

/** Roh-Nutzung ohne Zeitstempel — der Coordinator setzt die Minuten-ID. */
data class AppUsageSample(
    val packageName: String,
    val appName: String,
    val durationMs: Long
)
