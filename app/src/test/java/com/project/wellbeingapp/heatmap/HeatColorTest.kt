package com.project.wellbeingapp.heatmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatColorTest {

    @Test
    fun `fraction ist 0 bei 1 Minute und 1 bei 60 Minuten`() {
        assertEquals(0f, HeatColor.fraction(1f), 0.001f)
        assertEquals(1f, HeatColor.fraction(60f), 0.001f)
    }

    @Test
    fun `fraction clampt ausserhalb des Bereichs`() {
        assertEquals(0f, HeatColor.fraction(0f), 0.001f)
        assertEquals(1f, HeatColor.fraction(120f), 0.001f)
    }

    @Test
    fun `wenig Nutzung ist hell und transparent, viel ist dunkelblau und kraeftig`() {
        val light = HeatColor.argb(1f)
        val dark = HeatColor.argb(60f)

        val alphaLight = (light ushr 24) and 0xFF
        val alphaDark = (dark ushr 24) and 0xFF
        assertTrue("dunkel kräftiger als hell", alphaDark > alphaLight)

        // hell: alle Kanäle hoch; dunkel: Blau dominiert, Rot/Grün niedrig
        val rDark = (dark ushr 16) and 0xFF
        val bDark = dark and 0xFF
        assertTrue("Blau dominiert im Dunkeln", bDark > rDark)
    }
}
