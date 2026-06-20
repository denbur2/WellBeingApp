package com.project.wellbeingapp.heatmap

import com.project.wellbeingapp.database.dao.AppUsageDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [HeatmapBuilder] auf Basis des Join-Querys [AppUsageDao.getTrackingEntries].
 * Die eigentliche Aggregation liegt in [HeatmapAggregator] (rein/testbar).
 */
class DefaultHeatmapBuilder(
    private val appUsageDao: AppUsageDao
) : HeatmapBuilder {

    override fun observeHeatmap(start: Long, end: Long, packageName: String?): Flow<HeatmapData> =
        appUsageDao.getTrackingEntries(start, end)
            .map { HeatmapData(HeatmapAggregator.cells(it, packageName)) }

    override fun observeAvailableApps(start: Long, end: Long): Flow<List<HeatmapApp>> =
        appUsageDao.getTrackingEntries(start, end)
            .map { HeatmapAggregator.availableApps(it) }
}
