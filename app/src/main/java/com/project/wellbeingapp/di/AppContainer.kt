package com.project.wellbeingapp.di

import android.content.Context
import com.project.wellbeingapp.billing.BillingManager
import com.project.wellbeingapp.database.AppDatabase
import com.project.wellbeingapp.heatmap.DefaultHeatmapBuilder
import com.project.wellbeingapp.heatmap.HeatmapBuilder
import com.project.wellbeingapp.score.DefaultScoreCalculator
import com.project.wellbeingapp.score.ScoreCalculator
import com.project.wellbeingapp.services.AndroidAppUsageProvider
import com.project.wellbeingapp.services.AppUsageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Schlankes manuelles DI (Service-Locator). Erzeugt die App-weiten Singletons
 * einmal und stellt sie ViewModels & Service bereit — kein Hilt/Koin nötig.
 */
class AppContainer(context: Context) {

    private val db = AppDatabase.getDatabase(context)

    val appUsageDao = db.appUsageDao()
    val locationDao = db.locationDao()
    val scoreHistoryDao = db.scoreHistoryDao()

    /** Entwickler-Bonuspunkte (verstecktes Dev-Menü in den Settings). */
    val devSettings = DevSettings()

    /** Persistente Einstellungen (z. B. Tracking-Schalter). */
    val appPreferences = AppPreferences(context)

    /** App-Nutzung + Screen-on/off (für die Statistik). */
    val appUsageProvider: AppUsageProvider = AndroidAppUsageProvider(context)

    val scoreCalculator: ScoreCalculator =
        DefaultScoreCalculator(
            appUsageDao,
            scoreHistoryDao,
            locationDao,
            devSettings.bonusPoints,
            ownPackage = context.packageName
        )

    val heatmapBuilder: HeatmapBuilder =
        DefaultHeatmapBuilder(appUsageDao)

    /** Google-Play-Billing für die Premium-Freischaltung (Paywall ab Level 2). */
    val billingManager = BillingManager(context)

    /**
     * Löscht alle getrackten Daten (Standorte, App-Nutzung, Score-Archiv) und
     * setzt den Dev-Bonus zurück. Die per Flow gebundenen Screens (Score,
     * Verlauf, Stats, Heatmap) aktualisieren sich danach automatisch.
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        devSettings.reset()
    }
}
