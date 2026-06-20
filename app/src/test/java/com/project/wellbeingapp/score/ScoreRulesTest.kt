package com.project.wellbeingapp.score

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreRulesTest {

    private val oneMinute = 60_000L
    private val fiveMinutes = 5 * 60_000L
    private val sevenMinutes = 7 * 60_000L

    // --- windowDelta: vier Zonen (+1 / -1 / -2 / -3) + Grenzfälle ---

    @Test
    fun `kein bisschen Nutzung gibt Pluspunkt`() {
        assertEquals(1, ScoreRules.windowDelta(0L))
    }

    @Test
    fun `genau 1 Minute zaehlt noch als wenig (Pluspunkt)`() {
        assertEquals(1, ScoreRules.windowDelta(oneMinute))
    }

    @Test
    fun `knapp ueber 1 Minute gibt einen Minuspunkt`() {
        assertEquals(-1, ScoreRules.windowDelta(oneMinute + 1))
    }

    @Test
    fun `Mitte (3 Minuten) gibt einen Minuspunkt`() {
        assertEquals(-1, ScoreRules.windowDelta(3 * oneMinute))
    }

    @Test
    fun `genau 5 Minuten gibt noch einen Minuspunkt`() {
        assertEquals(-1, ScoreRules.windowDelta(fiveMinutes))
    }

    @Test
    fun `knapp ueber 5 Minuten gibt zwei Minuspunkte`() {
        assertEquals(-2, ScoreRules.windowDelta(fiveMinutes + 1))
    }

    @Test
    fun `genau 7 Minuten gibt zwei Minuspunkte`() {
        assertEquals(-2, ScoreRules.windowDelta(sevenMinutes))
    }

    @Test
    fun `knapp ueber 7 Minuten gibt drei Minuspunkte`() {
        assertEquals(-3, ScoreRules.windowDelta(sevenMinutes + 1))
    }

    @Test
    fun `viel Nutzung gibt drei Minuspunkte`() {
        assertEquals(-3, ScoreRules.windowDelta(20 * oneMinute))
    }

    // --- rawScore darf negativ werden ---

    @Test
    fun `rawScore summiert Deltas und darf negativ sein`() {
        // drei schlechte Fenster (je -3), ein gutes (+1) => -9 + 1 = -8
        val sums = listOf(10 * oneMinute, 10 * oneMinute, 10 * oneMinute, 0L)
        assertEquals(-8, ScoreRules.rawScore(sums))
    }

    // --- level ---

    @Test
    fun `level startet bei 1`() {
        assertEquals(1, ScoreRules.level(0))
        assertEquals(1, ScoreRules.level(999))
    }

    @Test
    fun `bei 1000 Punkten Level 2`() {
        assertEquals(2, ScoreRules.level(1000))
        assertEquals(3, ScoreRules.level(2500))
    }

    @Test
    fun `negativer Score bleibt Level 1`() {
        assertEquals(1, ScoreRules.level(-50))
    }

    // --- displayScore ---

    @Test
    fun `displayScore resettet bei 1000`() {
        assertEquals(0, ScoreRules.displayScore(1000))
        assertEquals(500, ScoreRules.displayScore(2500))
        assertEquals(999, ScoreRules.displayScore(999))
    }

    @Test
    fun `negativer Score wird unveraendert durchgereicht`() {
        assertEquals(-50, ScoreRules.displayScore(-50))
    }

    // --- treeStage: eine Stufe je 50 Punkte, geclampt 0..20 ---

    @Test
    fun `treeStage waechst alle 50 Punkte`() {
        assertEquals(0, ScoreRules.treeStage(0))
        assertEquals(0, ScoreRules.treeStage(49))
        assertEquals(1, ScoreRules.treeStage(50))
        assertEquals(10, ScoreRules.treeStage(500))
    }

    @Test
    fun `treeStage erreicht bei knapp 1000 die Maximalstufe`() {
        assertEquals(19, ScoreRules.treeStage(999))
        assertEquals(20, ScoreRules.treeStage(1000))
    }

    @Test
    fun `treeStage clampt negativen Score auf 0`() {
        assertEquals(0, ScoreRules.treeStage(-200))
    }

    // --- windowSums: Bucketing in 10-Min-Fenster ---

    @Test
    fun `windowSums summiert pro 10-Min-Fenster ueber Apps und Ticks`() {
        val w = ScoreRules.WINDOW_MS
        val entries = listOf(
            (w * 0 + 1000L) to 2000L,  // Fenster 0
            (w * 0 + 2000L) to 3000L,  // Fenster 0
            (w * 1 + 500L) to 4000L    // Fenster 1
        )
        val sums = ScoreRules.windowSums(entries)
        assertEquals(5000L, sums[0L])
        assertEquals(4000L, sums[1L])
    }
}
