package com.project.wellbeingapp.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.wellbeingapp.database.dao.AppUsageDao
import com.project.wellbeingapp.services.AppUsageProvider
import com.project.wellbeingapp.stats.AppUsageStats
import com.project.wellbeingapp.ui.common.TimeRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * Mappt die App-Nutzung im gewählten [TimeRange] auf ein nach Bildschirmzeit
 * sortiertes Ranking und ermittelt zusätzlich die Bildschirm-Aus-Zeit.
 * Die Aggregation liegt in [AppUsageStats] (rein/testbar). Der Zeitraum ist
 * per [selectRange] zwischen Tag und Woche umschaltbar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    private val appUsageDao: AppUsageDao,
    private val appUsageProvider: AppUsageProvider,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val selectedRange = MutableStateFlow(TimeRange.WEEK)

    val uiState: StateFlow<StatsUiState> =
        selectedRange.flatMapLatest { range ->
            val end = clock()
            val start = end - range.millis

            val ranking = appUsageDao.getByTimeRange(start, end)
                .map { AppUsageStats.ranking(it) }
            // Bildschirm-Aus-Zeit einmal je Zeitraum ermitteln; vorher `null`.
            val screenOff = flow<Long?> { emit(appUsageProvider.getScreenOffMillis(start, end)) }
                .onStart { emit(null) }

            combine(ranking, screenOff) { apps, off ->
                StatsUiState(apps = apps, screenOffMs = off, range = range, isLoading = false)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState()
        )

    /** Zeitraum (Tag/Woche) umschalten. */
    fun selectRange(range: TimeRange) {
        selectedRange.value = range
    }
}
