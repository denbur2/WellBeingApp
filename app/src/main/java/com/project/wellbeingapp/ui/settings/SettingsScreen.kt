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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.wellbeingapp.ui.theme.TerminalDim
import com.project.wellbeingapp.ui.theme.TerminalError
import com.project.wellbeingapp.ui.theme.TerminalGreen

/**
 * Settings-Screen im Terminal-Stil mit Tracking-Schalter und Daten-Reset.
 *
 * Versteckt: 7× auf den Titel tippen blendet das Entwickler-Menü ein (±Punkte
 * zum Testen von Baum-Wachstum und Error-Bild über die Dev-Bonuspunkte).
 *
 * @param trackingEnabled aktueller Zustand des Hintergrund-Trackings.
 * @param onSetTracking schaltet das Tracking an/aus (persistent).
 * @param onResetData löscht alle getrackten Daten (bereits bestätigt).
 * @param onAddPoints Dev-Bonuspunkte (nur Test-Menü).
 */
@Composable
fun SettingsScreen(
    trackingEnabled: Boolean,
    onSetTracking: (Boolean) -> Unit,
    onResetData: () -> Unit,
    onAddPoints: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var taps by remember { mutableIntStateOf(0) }
    var devVisible by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

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

        Text(
            text = if (trackingEnabled) "> tracking laeuft im hintergrund" else "> tracking gestoppt",
            color = if (trackingEnabled) TerminalGreen else TerminalError
        )
        Text("> daten bleiben lokal auf dem geraet", color = TerminalDim)
        Text("> heatmap: letzte 7 tage", color = TerminalDim)

        Spacer(Modifier.height(28.dp))

        // --- Tracking-Schalter ---
        ActionButton(
            label = if (trackingEnabled) "> TRACKING STOPPEN" else "> TRACKING STARTEN"
        ) {
            onSetTracking(!trackingEnabled)
        }

        Spacer(Modifier.height(12.dp))

        // --- Daten zurücksetzen (mit Bestätigung) ---
        if (!confirmReset) {
            ActionButton(label = "> DATEN ZURUECKSETZEN", color = TerminalError) {
                confirmReset = true
            }
        } else {
            Text(
                text = "> alle getrackten daten unwiderruflich loeschen?",
                color = TerminalError,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    label = "> JA, LOESCHEN",
                    color = TerminalError,
                    modifier = Modifier.weight(1f)
                ) {
                    onResetData()
                    confirmReset = false
                }
                ActionButton(
                    label = "> ABBRECHEN",
                    color = TerminalDim,
                    modifier = Modifier.weight(1f)
                ) {
                    confirmReset = false
                }
            }
        }

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

/** Vollbreiter Aktions-Button im Terminal-Stil. */
@Composable
private fun ActionButton(
    label: String,
    color: Color = TerminalGreen,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, color)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        textAlign = TextAlign.Center
    )
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
