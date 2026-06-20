package com.project.wellbeingapp.score

import com.project.wellbeingapp.database.dao.AppUsageDao
import com.project.wellbeingapp.database.dao.LocationDao
import com.project.wellbeingapp.database.dao.ScoreHistoryDao
import com.project.wellbeingapp.database.entity.AppUsageEntry
import com.project.wellbeingapp.database.entity.ScoreHistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.util.Calendar

/**
 * [ScoreCalculator] auf Basis der Roh-Nutzungsdaten ([AppUsageDao]) und der
 * reinen [ScoreRules]. Speichert selbst nichts außer beim [snapshotDay].
 *
 * @param ownPackage Paketname der eigenen App. Deren Nutzung wird beim Scoring
 *   ausgeklammert, damit das Betrachten der WellbeingApp keine Punkte kostet.
 */
class DefaultScoreCalculator(
    private val appUsageDao: AppUsageDao,
    private val scoreHistoryDao: ScoreHistoryDao,
    private val locationDao: LocationDao,
    private val bonusPoints: Flow<Int> = flowOf(0),
    private val ownPackage: String? = null,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ScoreCalculator {

    override fun observeCurrentScore(): Flow<ScoreData> {
        val queryStart = startOfDay(clock())
        val queryEnd = queryStart + DAY_MS
        return combine(
            appUsageDao.getByTimeRange(queryStart, queryEnd),
            bonusPoints,
            scoreHistoryDao.getAll(),
            locationDao.observeEarliestTimestamp(queryStart, queryEnd)
        ) { entries, bonus, history, earliest ->
            // Kumulativer Score: archivierte Vortage (Übertrag) + heutige Fenster bis
            // „jetzt". Leere (10-Min-)Fenster zählen mit, aber erst AB dem ersten
            // getrackten Eintrag heute (`earliest`) — nicht ab Mitternacht. So startet
            // der Score nach einem Daten-Reset bei ~0 statt rückwirkend hochzuzählen.
            val now = clock()
            val dayStart = startOfDay(now)
            val carryOver = history.filter { it.date < dayStart }.sumOf { it.score }
            val windowStart = earliest ?: now
            computeScore(entries, bonus, windowStart, now, baseRaw = carryOver)
        }
    }

    override fun observeWindowHistory(): Flow<List<ScoreWindow>> {
        val queryStart = startOfDay(clock())
        val queryEnd = queryStart + DAY_MS
        return combine(
            appUsageDao.getByTimeRange(queryStart, queryEnd),
            locationDao.observeEarliestTimestamp(queryStart, queryEnd)
        ) { entries, earliest ->
            val now = clock()
            val byWindow = scoringEntries(entries).groupBy { it.timestamp / ScoreRules.WINDOW_MS }
            // Fenster ebenfalls erst ab dem ersten getrackten Eintrag (Konsistenz mit Score).
            val firstWindow = (earliest ?: now) / ScoreRules.WINDOW_MS
            val lastWindow = now / ScoreRules.WINDOW_MS
            (firstWindow..lastWindow).map { window ->
                val windowEntries = byWindow[window].orEmpty()
                val usage = windowEntries.sumOf { it.duration }
                ScoreWindow(
                    startMillis = window * ScoreRules.WINDOW_MS,
                    usageMillis = usage,
                    delta = ScoreRules.windowDelta(usage),
                    apps = windowEntries
                        .groupBy { it.appName }
                        .map { (name, list) -> AppUsage(name, list.sumOf { it.duration }) }
                )
            }.reversed() // neueste Fenster zuerst
        }
    }

    override suspend fun snapshotDay(date: Long) {
        val dayStart = startOfDay(date)
        val dayEnd = dayStart + DAY_MS
        val entries = appUsageDao.getByTimeRange(dayStart, dayEnd).first()
        // Fenster erst ab dem ersten getrackten Eintrag des Tages zählen.
        val windowStart = locationDao.earliestTimestamp(dayStart, dayEnd) ?: dayStart
        val data = computeScore(entries, rangeStart = windowStart, rangeEnd = dayEnd)
        scoreHistoryDao.insert(
            ScoreHistoryEntry(
                date = dayStart,
                score = data.score,
                level = data.level,
                screenTimeMinutes = data.screenTimeMinutes
            )
        )
    }

    /**
     * Aggregiert App-Nutzungen im Zeitraum [rangeStart, rangeEnd] zu [ScoreData]
     * (plus Dev-Bonus und optionalem [baseRaw]-Übertrag aus Vortagen). Bewertet
     * werden ALLE 10-Min-Fenster des Zeitraums — auch leere (= keine Nutzung),
     * die laut Regel +1 geben.
     */
    private fun computeScore(
        entries: List<AppUsageEntry>,
        bonus: Int = 0,
        rangeStart: Long,
        rangeEnd: Long,
        baseRaw: Int = 0
    ): ScoreData {
        val scored = scoringEntries(entries)
        val windowSums = ScoreRules.windowSums(
            scored.map { it.timestamp to it.duration }
        )
        val firstWindow = rangeStart / ScoreRules.WINDOW_MS
        val lastWindow = rangeEnd / ScoreRules.WINDOW_MS
        var raw = bonus + baseRaw
        for (window in firstWindow..lastWindow) {
            raw += ScoreRules.windowDelta(windowSums[window] ?: 0L)
        }
        val totalMs = scored.sumOf { it.duration }
        return ScoreData(
            score = ScoreRules.displayScore(raw),
            level = ScoreRules.level(raw),
            screenTimeMinutes = (totalMs / 60_000L).toInt()
        )
    }

    /** Blendet die eigene App aus, damit ihr Betrachten keine Punkte kostet. */
    private fun scoringEntries(entries: List<AppUsageEntry>): List<AppUsageEntry> =
        if (ownPackage == null) entries
        else entries.filter { it.packageName != ownPackage }

    private fun startOfDay(timestamp: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
