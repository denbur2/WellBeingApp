package com.project.wellbeingapp.heatmap

import kotlinx.coroutines.flow.Flow

/**
 * Liest Roh-Standorte + App-Nutzung (Join) und aggregiert sie zu Heatmap-Zellen.
 * Reaktiv: eine neue DB-Zeile erzeugt eine neue Heatmap. Speichert nichts zwischen.
 */
interface HeatmapBuilder {

    /**
     * Heatmap für den Zeitraum [start, end].
     * @param packageName App-Filter; `null` = alle Apps kombiniert ("Gesamt").
     */
    fun observeHeatmap(start: Long, end: Long, packageName: String?): Flow<HeatmapData>

    /** Die im Zeitraum getrackten Apps — für die Auswahl-UI. */
    fun observeAvailableApps(start: Long, end: Long): Flow<List<HeatmapApp>>
}

data class HeatmapData(val cells: List<HeatmapCell>)

data class HeatmapCell(
    val latitude: Double,
    val longitude: Double,
    /** Aufsummierte Nutzungs-Minuten in dieser Rasterzelle. */
    val intensity: Float
)

/** Eine auswählbare App in der Heatmap. */
data class HeatmapApp(
    val packageName: String,
    val appName: String
)
