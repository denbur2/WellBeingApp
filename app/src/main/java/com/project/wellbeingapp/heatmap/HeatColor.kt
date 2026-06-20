package com.project.wellbeingapp.heatmap

/**
 * Feste Heat-Farbskala (unabhängig von App/Datenmenge):
 *  - ~1 min  → kräftiges Cyan (gut sichtbar auf der gedimmten Karte)
 *  - ~60 min → tiefes Blau, voll deckend
 * Dazwischen linear interpoliert, oberhalb 60 min geclampt.
 *
 * Durchgehend hohes Alpha, damit auch kurze Aufenthalte sichtbar sind
 * (die alte weiß→blau-Skala verschwand fast komplett auf grauem Kartengrund).
 *
 * Rein & testbar: liefert ein ARGB-Int, keine Android-Abhängigkeit.
 */
object HeatColor {

    const val MIN_MINUTES: Float = 1f
    const val MAX_MINUTES: Float = 60f

    // Endpunkte der Skala.
    private val LIGHT = intArrayOf(60, 220, 255) // RGB kräftiges Cyan (wenig Nutzung)
    private val DARK = intArrayOf(10, 30, 170)   // RGB tiefes Blau (viel Nutzung)
    private const val ALPHA_MIN = 150
    private const val ALPHA_MAX = 255

    /** Position auf der Skala 0..1 für eine Intensität in Minuten. */
    fun fraction(minutes: Float): Float =
        ((minutes - MIN_MINUTES) / (MAX_MINUTES - MIN_MINUTES)).coerceIn(0f, 1f)

    /** ARGB-Farbe für eine Intensität in Minuten. */
    fun argb(minutes: Float): Int {
        val t = fraction(minutes)
        val r = lerp(LIGHT[0], DARK[0], t)
        val g = lerp(LIGHT[1], DARK[1], t)
        val b = lerp(LIGHT[2], DARK[2], t)
        val a = lerp(ALPHA_MIN, ALPHA_MAX, t)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun lerp(from: Int, to: Int, t: Float): Int =
        (from + (to - from) * t).toInt().coerceIn(0, 255)
}
