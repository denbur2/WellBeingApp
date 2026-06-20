package com.project.wellbeingapp.services

import com.project.wellbeingapp.database.dao.AppUsageDao
import com.project.wellbeingapp.database.dao.LocationDao
import com.project.wellbeingapp.database.entity.AppUsageEntry
import com.project.wellbeingapp.database.entity.LocationEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Standard-Implementierung des [TrackingCoordinator].
 *
 * Tickt im [intervalMillis]-Takt (Default 1 Minute), holt Standort + App-Nutzung
 * und schreibt pro Tick **eine** [LocationEntry] und dazu die [AppUsageEntry]s.
 * Einziger Schreiber der Rohdaten.
 *
 * Wichtig: Die [LocationEntry] muss zuerst geschrieben werden — sie ist das
 * Fremdschlüssel-Ziel der [AppUsageEntry]s (gleicher `timestamp`).
 *
 * Einheit: [AppUsageEntry.duration] wird in **Millisekunden** gespeichert
 * (Score-/Heatmap-Berechnung rechnet auf Minuten um).
 */
class DefaultTrackingCoordinator(
    private val scope: CoroutineScope,
    private val locationProvider: LocationProvider,
    private val appUsageProvider: AppUsageProvider,
    private val locationDao: LocationDao,
    private val appUsageDao: AppUsageDao,
    private val intervalMillis: Long = 60_000L
) : TrackingCoordinator {

    private var job: Job? = null
    private var lastTick: Long = 0L

    /**
     * App, die beim letzten Tick am Fensterende im Vordergrund war. Wird in den
     * nächsten [AppUsageProvider.getUsageSince]-Aufruf gegeben, damit durchgehende
     * Sessions auch in ereignislosen Minuten lückenlos gezählt werden.
     */
    private var foreground: String? = null

    override fun start() {
        if (job?.isActive == true) return
        lastTick = System.currentTimeMillis() - intervalMillis
        foreground = null
        job = scope.launch {
            while (isActive) {
                tick()
                delay(intervalMillis)
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun tick() {
        val now = System.currentTimeMillis()
        val location = locationProvider.getCurrentLocation()

        // FK-Ziel zuerst: jede Minute genau eine Zeile, lat/lon ggf. null.
        locationDao.insert(
            LocationEntry(
                timestamp = now,
                latitude = location?.latitude,
                longitude = location?.longitude,
                accuracy = location?.accuracy
            )
        )

        val snapshot = appUsageProvider.getUsageSince(lastTick, foreground)
        snapshot.samples.forEach { sample ->
            appUsageDao.insert(
                AppUsageEntry(
                    timestamp = now,
                    packageName = sample.packageName,
                    appName = sample.appName,
                    duration = sample.durationMs
                )
            )
        }

        // Vordergrund-App für den nächsten Tick merken (Seed).
        foreground = snapshot.foregroundAtEnd
        lastTick = now
    }
}
