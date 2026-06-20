package com.project.wellbeingapp.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.wellbeingapp.ui.theme.TerminalDim
import com.project.wellbeingapp.ui.theme.TerminalGreen

/**
 * Settings-Screen im Terminal-Stil. Enthält ein verstecktes Entwickler-Menü:
 * 7× auf den Titel tippen blendet ±Punkte-Buttons ein (zum Testen von
 * Baum-Wachstum und Error-Bild über die Dev-Bonuspunkte).
 */
@Composable
fun SettingsScreen(
    onAddPoints: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var taps by remember { mutableIntStateOf(0) }
    var devVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "WELLBEING // SETTINGS",
            color = TerminalGreen,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    taps++
                    if (taps >= 7) devVisible = true
                },
            textAlign = TextAlign.Start
        )
        Spacer(Modifier.height(16.dp))

        Text("> tracking laeuft im hintergrund", color = TerminalDim)
        Text("> daten bleiben lokal auf dem geraet", color = TerminalDim)
        Text("> heatmap: letzte 7 tage", color = TerminalDim)

        if (devVisible) {
            Spacer(Modifier.height(32.dp))
            Text(
                "── DEV MENU ──",
                color = TerminalGreen,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text("score-bonus (nur zum testen):", color = TerminalDim)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DevButton("-100") { onAddPoints(-100) }
                DevButton("-10") { onAddPoints(-10) }
                DevButton("+10") { onAddPoints(10) }
                DevButton("+100") { onAddPoints(100) }
            }
        }
    }
}

@Composable
private fun DevButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .border(1.dp, TerminalGreen)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, color = TerminalGreen, style = MaterialTheme.typography.labelLarge)
    }
}
