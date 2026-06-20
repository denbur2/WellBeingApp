package com.project.wellbeingapp.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateMapOf
import com.project.wellbeingapp.score.AppUsage
import com.project.wellbeingapp.ui.theme.TerminalBackground
import com.project.wellbeingapp.ui.theme.TerminalDim
import com.project.wellbeingapp.ui.theme.TerminalError
import com.project.wellbeingapp.ui.theme.TerminalForeground
import com.project.wellbeingapp.ui.theme.TerminalGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Verlauf-Screen im Terminal-Stil: jede heutige Stunde mit Uhrzeit,
 * Nutzungsbalken und Netto-Punkten (−18..+6). So ist sichtbar, wann Punkte
 * gesammelt (wenig/keine Nutzung) bzw. verloren wurden (viel Nutzung).
 */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "WELLBEING // VERLAUF",
            color = TerminalGreen,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "> pro stunde heute",
            color = TerminalDim,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "> punkte heute: ${signed(state.netPoints)} (${state.hours.size} std)",
            color = TerminalGreen,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "> gesamt (alle tage): ${signed(state.totalPoints)}",
            color = TerminalGreen,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))

        if (state.isLoading) {
            Text("> lade daten ...", color = TerminalDim)
        } else {
            // Merkt sich je Stunde, ob die App-Aufschlüsselung aufgeklappt ist.
            val expanded = remember { mutableStateMapOf<Long, Boolean>() }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.hours.isEmpty()) {
                    item { Text("> noch keine stunden heute", color = TerminalDim) }
                } else {
                    items(state.hours, key = { "h${it.startMillis}" }) { hour ->
                        HourRow(
                            hour = hour,
                            expanded = expanded[hour.startMillis] == true,
                            onToggle = {
                                expanded[hour.startMillis] = expanded[hour.startMillis] != true
                            }
                        )
                    }
                }

                if (state.daySummaries.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "> vergangene tage",
                            color = TerminalDim,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    items(state.daySummaries, key = { "d${it.dateMillis}" }) { day ->
                        DaySummaryRow(day)
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySummaryRow(day: DaySummary) {
    val color = if (day.points >= 0) TerminalGreen else TerminalError
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = formatDay(day.dateMillis),
            color = TerminalForeground,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = signed(day.points),
            color = color,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun HourRow(hour: HourSummary, expanded: Boolean, onToggle: () -> Unit) {
    val pointsColor = when {
        hour.points > 0 -> TerminalGreen
        hour.points < 0 -> TerminalError
        else -> TerminalDim
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = if (expanded) "▾" else "▸",
                color = TerminalDim,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(16.dp)
            )
            Text(
                text = formatHour(hour.startMillis),
                color = TerminalForeground,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(10.dp))
            // Balken zeigt die Punkte relativ zum Extremwert (+6 bzw. −18).
            Text(
                text = bar(hour.points),
                color = pointsColor,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = formatUsage(hour.usageMillis),
                color = TerminalDim,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(56.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = signed(hour.points),
                color = pointsColor,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (expanded) {
            if (hour.apps.isEmpty()) {
                Text(
                    text = "    · keine app-nutzung",
                    color = TerminalDim,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 2.dp)
                )
            } else {
                hour.apps.forEach { app -> AppRow(app) }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppUsage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 26.dp, top = 1.dp, bottom = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = app.appName,
            color = TerminalForeground,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatAppDuration(app.durationMillis),
            color = TerminalDim,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// Extremwerte je Stunde (6 Zehn-Minuten-Fenster): bestes Fenster +1, schlechtestes −3.
private const val WINDOWS_PER_HOUR = 6
private const val MAX_HOUR_POINTS = WINDOWS_PER_HOUR * 1       // +6
private const val MIN_HOUR_POINTS = WINDOWS_PER_HOUR * 3       // |−18|

/**
 * ASCII-Balken fester Breite, gefüllt proportional zum Anteil am jeweiligen
 * Extremwert: +6 (bestmögliche Stunde) bzw. −18 (schlechtestmögliche Stunde)
 * füllen den Balken voll. Positive und negative Skala sind dadurch asymmetrisch.
 */
private fun bar(points: Int, width: Int = 6): String {
    val maxMagnitude = if (points >= 0) MAX_HOUR_POINTS else MIN_HOUR_POINTS
    val frac = kotlin.math.abs(points).toDouble() / maxMagnitude
    val filled = Math.round(frac * width).toInt().coerceIn(0, width)
    return "█".repeat(filled) + "░".repeat(width - filled)
}

/** Nutzung der Stunde in Minuten gerundet, z. B. "12m"; keine Nutzung → "0m". */
private fun formatUsage(ms: Long): String {
    val minutes = Math.round(ms / 60_000.0).toInt()
    return "${minutes}m"
}

/** App-Dauer als "Xm Ys" bzw. "Ys". */
private fun formatAppDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}

private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

/** Stundenbeginn als zweistellige Stunde, z. B. "13". */
private fun formatHour(millis: Long): String =
    SimpleDateFormat("HH", Locale.getDefault()).format(Date(millis))

/** Deutscher Wochentag + Datum, z. B. "Donnerstag 19.06.". */
private fun formatDay(millis: Long): String =
    SimpleDateFormat("EEEE dd.MM.", Locale.GERMAN).format(Date(millis))
