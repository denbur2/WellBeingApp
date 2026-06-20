package com.project.wellbeingapp.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.project.wellbeingapp.ui.heatmap.HeatmapViewModel
import com.project.wellbeingapp.ui.history.HistoryViewModel
import com.project.wellbeingapp.ui.score.ScoreViewModel
import com.project.wellbeingapp.ui.stats.StatsViewModel

/**
 * Zentrale [ViewModelProvider.Factory] für ViewModels mit Konstruktor-Parametern.
 * Die Abhängigkeiten kommen aus dem [AppContainer].
 */
class AppViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        when {
            modelClass.isAssignableFrom(ScoreViewModel::class.java) ->
                ScoreViewModel(container.scoreCalculator, container.scoreHistoryDao)

            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(container.scoreCalculator, container.scoreHistoryDao)

            modelClass.isAssignableFrom(HeatmapViewModel::class.java) ->
                HeatmapViewModel(container.heatmapBuilder)

            modelClass.isAssignableFrom(StatsViewModel::class.java) ->
                StatsViewModel(container.appUsageDao, container.appUsageProvider)

            else -> error("Unbekanntes ViewModel: ${modelClass.name}")
        } as T
}
