package com.project.wellbeingapp.services

/**
 * Liefert den aktuellen Standort. Kapselt die Android-Location-APIs,
 * damit der [TrackingCoordinator] nicht direkt davon abhängt.
 */
interface LocationProvider {
    /** @return aktueller Standort, oder null wenn kein Signal verfügbar ist. */
    suspend fun getCurrentLocation(): LocationSample?
}

/** Roh-Standort ohne Zeitstempel — der Coordinator setzt die Minuten-ID. */
data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float
)
