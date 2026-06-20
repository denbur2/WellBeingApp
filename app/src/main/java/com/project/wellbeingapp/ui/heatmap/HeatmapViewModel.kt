package com.project.wellbeingapp.ui.heatmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.wellbeingapp.heatmap.HeatmapBuilder
import com.project.wellbeingapp.ui.common.TimeRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Hält die gewählte App + den Zeitraum und mappt den passenden
 * [HeatmapBuilder]-Flow auf [HeatmapUiState]. Beim App-/Zeitraum-Wechsel ändern
 * sich nur die Zellen — die Karte (Kameraposition/Zoom) liegt allein in der UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HeatmapViewModel(
    private val builder: HeatmapBuilder,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    /** `null` = „Gesamt" (alle Apps kombiniert). */
    private val selectedApp = MutableStateFlow<String?>(null)
    private val selectedRange = MutableStateFlow(TimeRange.WEEK)

    /** (start, end) für den aktuell gewählten Zeitraum, relativ zu „jetzt". */
    private fun bounds(range: TimeRange): Pair<Long, Long> {
        val end = clock()
        return (end - range.millis) to end
    }

    val uiState: StateFlow<HeatmapUiState> =
        combine(
            combine(selectedApp, selectedRange) { app, range -> app to range }
                .flatMapLatest { (app, range) ->
                    val (start, end) = bounds(range)
                    builder.observeHeatmap(start, end, app)
                },
            selectedRange.flatMapLatest { range ->
                val (start, end) = bounds(range)
                builder.observeAvailableApps(start, end)
            },
            selectedApp,
            selectedRange
        ) { data, apps, selected, range ->
            HeatmapUiState(
                cells = data.cells,
                availableApps = apps,
                selectedApp = selected,
                range = range,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HeatmapUiState()
        )

    /** App wählen; `null` für die „Gesamt"-Ansicht. */
    fun selectApp(packageName: String?) {
        selectedApp.value = packageName
    }

    /** Zeitraum (Tag/Woche) umschalten. */
    fun selectRange(range: TimeRange) {
        selectedRange.value = range
    }
}
