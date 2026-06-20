package com.project.wellbeingapp.di

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistente App-Einstellungen (SharedPreferences) — alles, was einen
 * Prozess-/Geräte-Neustart überleben muss. Aktuell nur der Tracking-Schalter.
 */
class AppPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _trackingEnabled = MutableStateFlow(prefs.getBoolean(KEY_TRACKING, true))
    /** Ob das Hintergrund-Tracking laufen soll (Default: an). */
    val trackingEnabled: StateFlow<Boolean> = _trackingEnabled.asStateFlow()

    fun setTrackingEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_TRACKING, enabled) }
        _trackingEnabled.value = enabled
    }

    private companion object {
        const val PREFS_NAME = "wellbeing_prefs"
        const val KEY_TRACKING = "tracking_enabled"
    }
}
