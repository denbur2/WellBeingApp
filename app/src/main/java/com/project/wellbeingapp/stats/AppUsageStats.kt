package com.project.wellbeingapp.stats

import com.project.wellbeingapp.database.entity.AppUsageEntry

/**
 * Reine, testbare Aggregations-Logik für die App-Statistiken — entkoppelt von
 * Room/Android. Summiert die Vordergrund-Zeit je App und sortiert absteigend.
 */
object AppUsageStats {

    /**
     * @return eine [AppStat] pro App, nach Gesamtnutzung absteigend sortiert.
     *         Apps ohne Nutzung (0 ms) werden weggelassen.
     */
    fun ranking(entries: List<AppUsageEntry>): List<AppStat> =
        entries.groupBy { it.packageName }
            .map { (pkg, rows) ->
                AppStat(
                    packageName = pkg,
                    appName = rows.first().appName,
                    totalMs = rows.sumOf { it.duration }
                )
            }
            .filter { it.totalMs > 0 }
            .sortedByDescending { it.totalMs }
}
