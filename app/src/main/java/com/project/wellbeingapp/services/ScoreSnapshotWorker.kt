package com.project.wellbeingapp.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.project.wellbeingapp.WellbeingApplication
import java.util.concurrent.TimeUnit

/**
 * Schreibt einmal täglich den Score des Vortags ins Archiv `score_history`
 * (bleibt dauerhaft erhalten — siehe Daten-Retention). Der heutige Live-Score
 * kommt weiterhin direkt aus [com.project.wellbeingapp.score.ScoreCalculator].
 */
class ScoreSnapshotWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as WellbeingApplication).container
        val yesterday = System.currentTimeMillis() - DAY_MS
        container.scoreCalculator.snapshotDay(yesterday)
        return Result.success()
    }

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val WORK_NAME = "daily-score-snapshot"

        /** Plant den täglichen Snapshot (idempotent — vorhandene Planung bleibt). */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ScoreSnapshotWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
