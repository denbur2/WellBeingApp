package com.project.wellbeingapp.heatmap

/**
 * Feste Heat-Farbskala (unabhängig von App/Datenmenge):
 *  - ~1 min   → kräftiges Cyan (gut sichtbar auf der gedimmten Karte)
 *  - ~60 min  → tiefes Blau, voll deckend
 *  - ~120 min → kräftiges Rot („Hotspot": lange Nutzung am selben Ort)
 * Dazwischen linear interpoliert, oberhalb 120 min geclampt.
 *
 * Durchgehend hohes Alpha, damit auch kurze Aufenthalte sichtbar sind
 * (die alte weiß→blau-Skala verschwand fast komplett auf grauem Kartengrund).
 *
 * Rein & testbar: liefert ein ARGB-Int, keine Android-Abhängigkeit.
 */
object HeatColor {

    const val MIN_MINUTES: Float = 1f
    const val MAX_MINUTES: Float = 60f
    const val HOT_MINUTES: Float = 120f

    // Endpunkte der Skala.
    private val LIGHT = intArrayOf(60, 220, 255) // RGB kräftiges Cyan (wenig Nutzung)
    private val DARK = intArrayOf(10, 30, 170)   // RGB tiefes Blau (mittlere Nutzung)
    private val HOT = intArrayOf(230, 30, 20)    // RGB kräftiges Rot (sehr lange Nutzung)
    private const val ALPHA_MIN = 150
    private const val ALPHA_MAX = 255

    /** Position auf der Skala 0..1 für eine Intensität in Minuten (Cyan→Blau-Segment). */
    fun fraction(minutes: Float): Float =
        ((minutes - MIN_MINUTES) / (MAX_MINUTES - MIN_MINUTES)).coerceIn(0f, 1f)

    /** ARGB-Farbe für eine Intensität in Minuten. */
    fun argb(minutes: Float): Int {
        return if (minutes <= MAX_MINUTES) {
            // 1..60 min: Cyan → Tiefblau, Alpha steigt mit der Nutzung.
            val t = fraction(minutes)
            pack(
                lerp(LIGHT[0], DARK[0], t),
                lerp(LIGHT[1], DARK[1], t),
                lerp(LIGHT[2], DARK[2], t),
                lerp(ALPHA_MIN, ALPHA_MAX, t)
            )
        } else {
            // 60..120 min: Tiefblau → Rot, voll deckend, oberhalb 120 min geclampt.
            val t = ((minutes - MAX_MINUTES) / (HOT_MINUTES - MAX_MINUTES)).coerceIn(0f, 1f)
            pack(
                lerp(DARK[0], HOT[0], t),
                lerp(DARK[1], HOT[1], t),
                lerp(DARK[2], HOT[2], t),
                ALPHA_MAX
            )
        }
    }

    private fun pack(r: Int, g: Int, b: Int, a: Int): Int =
        (a shl 24) or (r shl 16) or (g shl 8) or b

    private fun lerp(from: Int, to: Int, t: Float): Int =
        (from + (to - from) * t).toInt().coerceIn(0, 255)
}
