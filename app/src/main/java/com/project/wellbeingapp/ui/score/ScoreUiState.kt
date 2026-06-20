package com.project.wellbeingapp.ui.score

import com.project.wellbeingapp.database.entity.ScoreHistoryEntry
import com.project.wellbeingapp.score.ScoreData

data class ScoreUiState(
    /** Heutiger Live-Score. */
    val current: ScoreData? = null,
    /** Vergangene Tage für den Trend-Graph. */
    val history: List<ScoreHistoryEntry> = emptyList(),
    val isLoading: Boolean = true
)
