package com.project.wellbeingapp.services

/**
 * Liefert die App-Nutzung seit einem Zeitpunkt. Kapselt den Android
 * UsageStatsManager, damit der [TrackingCoordinator] testbar bleibt.
 */
interface AppUsageProvider {
    /**
     * Vordergrund-Nutzung pro App im Fenster [startTime, jetzt].
     *
     * @param startTime Fensterbeginn (Unix-ms).
     * @param foregroundAtStart Paket, das zu [startTime] bereits im Vordergrund
     *   war (vom vorherigen Tick weitergereicht), oder `null` falls unbekannt.
     *   Wird ab Fensterbeginn angerechnet — entscheidend, damit durchgehende
     *   Sessions auch in ereignislosen Minuten gezählt werden.
     * @return Nutzung im Fenster + das am Fensterende vorderste Paket (Seed fürs
     *   nächste Mal).
     */
    suspend fun getUsageSince(
        startTime: Long,
        foregroundAtStart: String? = null
    ): UsageSnapshot

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

/**
 * Ergebnis von [AppUsageProvider.getUsageSince].
 * @param samples Nutzung pro App im abgefragten Fenster.
 * @param foregroundAtEnd Paket, das am Fensterende noch im Vordergrund ist
 *   (oder `null`) — als `foregroundAtStart` in den nächsten Aufruf zu geben.
 */
data class UsageSnapshot(
    val samples: List<AppUsageSample>,
    val foregroundAtEnd: String?
)
