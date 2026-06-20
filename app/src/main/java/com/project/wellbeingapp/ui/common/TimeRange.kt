package com.project.wellbeingapp.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.wellbeingapp.ui.theme.TerminalDim
import com.project.wellbeingapp.ui.theme.TerminalGreen

/**
 * Auswählbarer Zeitraum für Stats & Heatmap. [millis] ist ein gleitendes Fenster
 * relativ zu „jetzt" (z. B. die letzten 24 h bzw. 7 Tage).
 */
enum class TimeRange(val label: String, val millis: Long) {
    DAY("TAG", 24L * 60 * 60 * 1000),
    WEEK("WOCHE", 7L * 24 * 60 * 60 * 1000)
}

/**
 * Terminal-Stil-Umschalter [ TAG ] WOCHE für die Zeitraum-Auswahl.
 * @param label erlaubt screen-spezifische Beschriftung (z. B. "24H" statt "TAG").
 */
@Composable
fun RangeToggle(
    selected: TimeRange,
    onSelect: (TimeRange) -> Unit,
    modifier: Modifier = Modifier,
    label: (TimeRange) -> String = { it.label }
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TimeRange.entries.forEach { range ->
            val active = range == selected
            val color = if (active) TerminalGreen else TerminalDim
            Box(
                Modifier
                    .border(1.dp, color)
                    .clickable { onSelect(range) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (active) "[${label(range)}]" else label(range),
                    color = color,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
