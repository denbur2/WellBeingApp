package com.project.wellbeingapp.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.wellbeingapp.stats.AppStat
import com.project.wellbeingapp.ui.common.RangeToggle
import com.project.wellbeingapp.ui.common.TimeRange
import com.project.wellbeingapp.ui.theme.TerminalBackground
import com.project.wellbeingapp.ui.theme.TerminalDim
import com.project.wellbeingapp.ui.theme.TerminalForeground
import com.project.wellbeingapp.ui.theme.TerminalGreen

/**
 * Stats-Screen im Terminal-Stil: Bildschirmzeit pro App der letzten 7 Tage,
 * absteigend sortiert, mit ASCII-Balken relativ zur meistgenutzten App.
 */
@Composable
fun StatsScreen(
    state: StatsUiState,
    onSelectRange: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "WELLBEING // STATS",
            color = TerminalGreen,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "> bildschirmzeit pro app (${rangeLabel(state.range)})",
            color = TerminalDim,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(10.dp))
        RangeToggle(selected = state.range, onSelect = onSelectRange)
        state.screenOffMs?.let { off ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = "> bildschirm aus: ${formatDuration(off)}",
                color = TerminalGreen,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(16.dp))

        when {
            state.isLoading ->
                Text("> lade daten ...", color = TerminalDim)

            state.apps.isEmpty() ->
                Text("> keine nutzungsdaten im zeitraum", color = TerminalDim)

            else -> {
                val max = state.apps.first().totalMs.coerceAtLeast(1)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.apps, key = { it.packageName }) { app ->
                        AppRow(app = app, maxMs = max)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppStat, maxMs: Long) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = app.appName,
                color = TerminalForeground,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatDuration(app.totalMs),
                color = TerminalGreen,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            text = bar(app.totalMs, maxMs),
            color = TerminalGreen,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** ASCII-Balken fester Breite, gefüllt proportional zur meistgenutzten App. */
private fun bar(valueMs: Long, maxMs: Long, width: Int = 24): String {
    val filled = (valueMs.toDouble() / maxMs * width).toInt().coerceIn(0, width)
    return "█".repeat(filled) + "░".repeat(width - filled)
}

/** Menschlich lesbarer Zeitraum-Hinweis für die Überschrift. */
private fun rangeLabel(range: TimeRange): String =
    when (range) {
        TimeRange.DAY -> "24 stunden"
        TimeRange.WEEK -> "7 tage"
    }

/** Formatiert Millisekunden als "Xh Ym" bzw. "Ym". */
private fun formatDuration(ms: Long): String {
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
