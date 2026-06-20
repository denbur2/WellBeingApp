package com.project.wellbeingapp.score

/**
 * Reine, seiteneffektfreie Score-Regeln — bewusst von Room/Android entkoppelt,
 * damit sie als einfache JVM-Unit-Tests prüfbar sind.
 *
 * Regel pro 10-Minuten-Fenster (Summe der Bildschirmzeit aller Apps):
 *  - ≤ 1 min      → +1
 *  - 1 – 5 min    → −1
 *  - 5 – 7 min    → −2
 *  - > 7 min      → −3
 *
 * Der kumulative Roh-Score darf negativ werden. Anzeige-Score & Level leiten sich
 * daraus ab: alle 1000 Punkte ein Level-Up, Anzeige-Score zurück auf 0.
 */
object ScoreRules {

    const val WINDOW_MS: Long = 10 * 60 * 1000L
    const val LEVEL_THRESHOLD: Int = 1000

    private const val MS_PER_MINUTE: Double = 60_000.0

    /**
     * Delta für ein einzelnes Fenster anhand seiner Gesamt-Bildschirmzeit (ms):
     * ≤1min → +1, 1–5min → −1, 5–7min → −2, >7min → −3.
     */
    fun windowDelta(windowMs: Long): Int {
        val minutes = windowMs / MS_PER_MINUTE
        return when {
            minutes <= 1.0 -> +1
            minutes > 1.0 && minutes <= 5.0 -> -1
            minutes > 5.0 && minutes <= 7.0 -> -2
            else -> -3
        }
    }

    /** Kumulativer Roh-Score über alle Fenster-Summen (in ms). Kann negativ sein. */
    fun rawScore(windowSumsMs: Collection<Long>): Int =
        windowSumsMs.sumOf { windowDelta(it) }

    /** Level beginnt bei 1; jede volle 1000er-Stufe erhöht es. */
    fun level(rawScore: Int): Int =
        if (rawScore <= 0) 1 else 1 + rawScore / LEVEL_THRESHOLD

    /**
     * Anzeige-Score (steuert die Baum-Stufe):
     *  - rawScore ≥ 0 → 0..999 (Rest nach Level-Reset)
     *  - rawScore < 0 → negativ (Screen zeigt dann das ASCII-Error-Bild)
     */
    fun displayScore(rawScore: Int): Int =
        if (rawScore < 0) rawScore else rawScore % LEVEL_THRESHOLD

    /** Anzahl der Baum-Wachstumsstufen (Index 0..MAX_TREE_STAGE). */
    const val MAX_TREE_STAGE: Int = 20

    /**
     * Baum-Stufe aus dem Anzeige-Score: eine Stufe je 50 Punkte, geclampt 0..20
     * (1000 Punkte = Stufe 20 = ausgewachsen). Negativer Score → Stufe 0, der
     * Screen zeigt dann aber das Error-Bild statt eines Baums.
     */
    fun treeStage(displayScore: Int): Int =
        (displayScore / 50).coerceIn(0, MAX_TREE_STAGE)

    /**
     * Bucketet App-Nutzungs-Dauern (ms) zu 10-Min-Fenstern und summiert pro Fenster.
     * @param entries Paare (timestamp, durationMs).
     * @return Map Fenster-Index → Summe ms.
     */
    fun windowSums(entries: List<Pair<Long, Long>>): Map<Long, Long> =
        entries.groupBy { (timestamp, _) -> timestamp / WINDOW_MS }
            .mapValues { (_, list) -> list.sumOf { it.second } }
}
