package com.project.wellbeingapp.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

/**
 * [LocationProvider] auf Basis des FusedLocationProviderClient.
 *
 * Liefert einen aktuellen Standort oder `null`, wenn keine Berechtigung vorliegt
 * oder kein Signal verfügbar ist (z.B. GPS aus).
 */
class AndroidLocationProvider(
    context: Context
) : LocationProvider {

    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)

    @SuppressLint("MissingPermission") // Berechtigung wird vor dem Aufruf geprüft.
    override suspend fun getCurrentLocation(): LocationSample? {
        if (!hasLocationPermission()) return null

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .build()

        return try {
            // getCurrentLocation liefert v.a. drinnen / auf dem Emulator oft null.
            // Dann auf den letzten bekannten Fix zurückfallen, damit überhaupt
            // Koordinaten in die DB kommen (sonst bleibt die Heatmap leer).
            val location = client.getCurrentLocation(request, null).await()
                ?: client.lastLocation.await()
                ?: return null
            LocationSample(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = if (location.hasAccuracy()) location.accuracy else 0f
            )
        } catch (_: SecurityException) {
            null
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}
