package com.project.wellbeingapp.stats

import com.project.wellbeingapp.database.entity.AppUsageEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUsageStatsTest {

    private fun entry(ts: Long, pkg: String, app: String, durationMs: Long) =
        AppUsageEntry(timestamp = ts, packageName = pkg, appName = app, duration = durationMs)

    @Test
    fun `summiert je App und sortiert absteigend`() {
        val entries = listOf(
            entry(1, "a", "App A", 60_000L),
            entry(2, "a", "App A", 120_000L), // A gesamt: 180s
            entry(3, "b", "App B", 300_000L)  // B gesamt: 300s
        )
        val ranking = AppUsageStats.ranking(entries)
        assertEquals(listOf("App B", "App A"), ranking.map { it.appName })
        assertEquals(180_000L, ranking.first { it.packageName == "a" }.totalMs)
    }

    @Test
    fun `minutes rechnet Millisekunden auf ganze Minuten um`() {
        val ranking = AppUsageStats.ranking(listOf(entry(1, "a", "App A", 150_000L)))
        assertEquals(2L, ranking.first().minutes) // 150s = 2 min (abgerundet)
    }

    @Test
    fun `Apps ohne Nutzung werden weggelassen`() {
        val ranking = AppUsageStats.ranking(listOf(entry(1, "a", "App A", 0L)))
        assertTrue(ranking.isEmpty())
    }

    @Test
    fun `leere Eingabe ergibt leere Liste`() {
        assertTrue(AppUsageStats.ranking(emptyList()).isEmpty())
    }
}
