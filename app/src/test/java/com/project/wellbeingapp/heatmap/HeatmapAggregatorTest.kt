package com.project.wellbeingapp.heatmap

import com.project.wellbeingapp.database.TrackingEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapAggregatorTest {

    private fun entry(
        ts: Long, lat: Double?, lon: Double?, pkg: String, app: String, durationMs: Long
    ) = TrackingEntry(ts, lat, lon, 1f, pkg, app, durationMs)

    @Test
    fun `gleiche Rasterzelle wird aufsummiert in Minuten`() {
        val entries = listOf(
            entry(1, 52.5000, 13.4000, "a", "App A", 60_000L),
            entry(2, 52.50001, 13.40001, "a", "App A", 120_000L) // gleiche Zelle
        )
        val cells = HeatmapAggregator.cells(entries, packageName = null)
        assertEquals(1, cells.size)
        assertEquals(3.0f, cells.first().intensity, 0.001f) // 180s = 3 min
    }

    @Test
    fun `App-Filter beruecksichtigt nur die gewaehlte App`() {
        val entries = listOf(
            entry(1, 52.5, 13.4, "a", "App A", 60_000L),
            entry(2, 52.5, 13.4, "b", "App B", 60_000L)
        )
        val onlyA = HeatmapAggregator.cells(entries, packageName = "a")
        assertEquals(1.0f, onlyA.sumOf { it.intensity.toDouble() }.toFloat(), 0.001f)
    }

    @Test
    fun `Gesamt kombiniert alle Apps`() {
        val entries = listOf(
            entry(1, 52.5, 13.4, "a", "App A", 60_000L),
            entry(2, 52.5, 13.4, "b", "App B", 60_000L)
        )
        val all = HeatmapAggregator.cells(entries, packageName = null)
        assertEquals(2.0f, all.sumOf { it.intensity.toDouble() }.toFloat(), 0.001f)
    }

    @Test
    fun `Eintraege ohne Standort werden ignoriert`() {
        val entries = listOf(entry(1, null, null, "a", "App A", 60_000L))
        assertTrue(HeatmapAggregator.cells(entries, null).isEmpty())
    }

    @Test
    fun `availableApps ist distinct und nach Haeufigkeit sortiert`() {
        val entries = listOf(
            entry(1, 52.5, 13.4, "z", "Zeta", 60_000L),   // Zeta gesamt: 60s
            entry(2, 52.5, 13.4, "a", "Alpha", 120_000L), // Alpha gesamt: 240s -> häufiger
            entry(3, 52.5, 13.4, "a", "Alpha", 120_000L)
        )
        val apps = HeatmapAggregator.availableApps(entries)
        assertEquals(listOf("Alpha", "Zeta"), apps.map { it.appName })
    }
}
