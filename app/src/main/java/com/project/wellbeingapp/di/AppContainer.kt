package com.project.wellbeingapp.di

import android.content.Context
import com.project.wellbeingapp.database.AppDatabase
import com.project.wellbeingapp.heatmap.DefaultHeatmapBuilder
import com.project.wellbeingapp.heatmap.HeatmapBuilder
import com.project.wellbeingapp.score.DefaultScoreCalculator
import com.project.wellbeingapp.score.ScoreCalculator
import com.project.wellbeingapp.services.AndroidAppUsageProvider
import com.project.wellbeingapp.services.AppUsageProvider

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

    /** App-Nutzung + Screen-on/off (für die Statistik). */
    val appUsageProvider: AppUsageProvider = AndroidAppUsageProvider(context)

    val scoreCalculator: ScoreCalculator =
        DefaultScoreCalculator(
            appUsageDao,
            scoreHistoryDao,
            devSettings.bonusPoints,
            ownPackage = context.packageName
        )

    val heatmapBuilder: HeatmapBuilder =
        DefaultHeatmapBuilder(appUsageDao)
}
