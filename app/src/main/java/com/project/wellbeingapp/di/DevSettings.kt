package com.project.wellbeingapp.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Entwickler-Bonuspunkte zum Testen des Baum-Wachstums (verstecktes Dev-Menü in
 * den Settings). Fließt additiv in den Roh-Score ein — landet bewusst NICHT im
 * Archiv (`score_history`), das nur echte Daten enthält.
 */
class DevSettings {
    val bonusPoints = MutableStateFlow(0)

    fun add(delta: Int) = bonusPoints.update { it + delta }
}
