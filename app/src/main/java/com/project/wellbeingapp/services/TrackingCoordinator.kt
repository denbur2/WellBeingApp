package com.project.wellbeingapp.services

/**
 * Tickt jede Minute, holt von [LocationProvider] und [AppUsageProvider]
 * und schreibt genau eine location_entries-Zeile (+ app_usage_entries)
 * pro Minute in die Datenbank. Einziger Schreiber der Rohdaten.
 */
interface TrackingCoordinator {
    fun start()
    fun stop()
}
