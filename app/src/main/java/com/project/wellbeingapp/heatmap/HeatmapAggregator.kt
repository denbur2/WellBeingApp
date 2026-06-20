package com.project.wellbeingapp.heatmap

import com.project.wellbeingapp.database.TrackingEntry

/**
 * Reine, testbare Aggregations-Logik für die Heatmap — entkoppelt von Room/Android.
 *
 * Bucketet Standorte in ein festes Raster (~30 m) und summiert je Zelle die
 * Nutzungs-Minuten der gewählten App.
 */
object HeatmapAggregator {

    /** Rastergröße in Grad (~30 m). Bestimmt die Auflösung der Zellen. */
    const val GRID: Double = 0.0003

    private const val MS_PER_MINUTE: Double = 60_000.0

    /**
     * @param packageName App-Filter; `null` = alle Apps kombiniert.
     * @return eine Zelle pro Rasterpunkt mit aufsummierten Nutzungs-Minuten.
     */
    fun cells(entries: List<TrackingEntry>, packageName: String?): List<HeatmapCell> =
        entries.asSequence()
            .filter { it.latitude != null && it.longitude != null }
            .filter { packageName == null || it.packageName == packageName }
            .groupBy { gridKey(it.latitude!!, it.longitude!!) }
            .map { (key, rows) ->
                val (latBucket, lonBucket) = key
                HeatmapCell(
                    latitude = latBucket * GRID,
                    longitude = lonBucket * GRID,
                    intensity = (rows.sumOf { it.duration } / MS_PER_MINUTE).toFloat()
                )
            }

    /** Getrackte Apps, nach Gesamtnutzung im Zeitraum absteigend sortiert (häufigste zuerst). */
    fun availableApps(entries: List<TrackingEntry>): List<HeatmapApp> =
        entries.groupBy { it.packageName }
            .map { (pkg, rows) ->
                HeatmapApp(pkg, rows.first().appName) to rows.sumOf { it.duration }
            }
            .sortedByDescending { it.second }
            .map { it.first }

    private fun gridKey(lat: Double, lon: Double): Pair<Long, Long> =
        Math.round(lat / GRID) to Math.round(lon / GRID)
}
