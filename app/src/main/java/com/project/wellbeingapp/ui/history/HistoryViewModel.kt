package com.project.wellbeingapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.wellbeingapp.database.dao.ScoreHistoryDao
import com.project.wellbeingapp.score.AppUsage
import com.project.wellbeingapp.score.ScoreCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

/**
 * Liefert die heutigen 10-Min-Fenster aus dem [ScoreCalculator] sowie die
 * Tages-Zusammenfassungen vergangener Tage aus [ScoreHistoryDao] als
 * [HistoryUiState] — damit nachvollziehbar ist, wann Punkte gesammelt bzw.
 * verloren wurden, und wie sich der Score über die Tage entwickelt.
 */
class HistoryViewModel(
    calculator: ScoreCalculator,
    scoreHistoryDao: ScoreHistoryDao,
    clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val todayStart = startOfDay(clock())

    val uiState: StateFlow<HistoryUiState> =
        combine(
            calculator.observeWindowHistory(),
            scoreHistoryDao.getAll()
        ) { windows, history ->
            HistoryUiState(
                // Die 10-Min-Fenster zu Stunden aufsummieren (neueste zuerst).
                hours = windows
                    .groupBy { it.startMillis / HOUR_MS }
                    .map { (hourIndex, group) ->
                        HourSummary(
                            startMillis = hourIndex * HOUR_MS,
                            usageMillis = group.sumOf { it.usageMillis },
                            points = group.sumOf { it.delta },
                            apps = group.flatMap { it.apps }
                                .groupBy { it.appName }
                                .map { (name, list) -> AppUsage(name, list.sumOf { it.durationMillis }) }
                                .sortedByDescending { it.durationMillis }
                        )
                    }
                    .sortedByDescending { it.startMillis },
                // Nur abgeschlossene (vergangene) Tage; heute steckt in `hours`.
                daySummaries = history
                    .filter { it.date < todayStart }
                    .map { DaySummary(dateMillis = it.date, points = it.score) },
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState()
        )

    private fun startOfDay(timestamp: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private companion object {
        const val HOUR_MS = 60 * 60 * 1000L
    }
}
