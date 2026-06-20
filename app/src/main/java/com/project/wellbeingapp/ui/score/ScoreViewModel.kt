package com.project.wellbeingapp.ui.score

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.wellbeingapp.database.dao.ScoreHistoryDao
import com.project.wellbeingapp.score.ScoreCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Verbindet den Live-Score aus [ScoreCalculator] mit dem Verlauf aus
 * [ScoreHistoryDao] zu einem gemeinsamen [ScoreUiState].
 */
class ScoreViewModel(
    private val calculator: ScoreCalculator,
    private val scoreHistoryDao: ScoreHistoryDao
) : ViewModel() {

    val uiState: StateFlow<ScoreUiState> =
        combine(
            calculator.observeCurrentScore(),
            scoreHistoryDao.getAll()
        ) { current, history ->
            ScoreUiState(current = current, history = history, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScoreUiState()
        )
}
