package com.project.wellbeingapp.services

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [AppUsageProvider] auf Basis des Android [UsageStatsManager].
 *
 * Fragt die Vordergrund-Nutzung pro App im Fenster [startTime, jetzt] ab.
 * Setzt die Special Permission PACKAGE_USAGE_STATS voraus (siehe Permission-Flow);
 * ohne sie liefert das System eine leere Liste, kein Crash.
 */
class AndroidAppUsageProvider(
    context: Context
) : AppUsageProvider {

    private val appContext = context.applicationContext
    private val usageStatsManager =
        appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager: PackageManager = appContext.packageManager

    /** Eigenes Paket: wird gar nicht erst erfasst (kein Selbst-Tracking). */
    private val ownPackage: String = appContext.packageName

    override suspend fun getUsageSince(startTime: Long): List<AppUsageSample> =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            // queryEvents statt queryUsageStats: liefert EXAKT die Vordergrund-Zeit im
            // Fenster [startTime, now]. queryUsageStats würde über INTERVAL_BEST den
            // ganzen Tages-Bucket zurückgeben und bei Minuten-Ticks massiv überzählen.
            val events = usageStatsManager.queryEvents(startTime, now)

            val foregroundSince = HashMap<String, Long>() // pkg -> resume-Zeitpunkt
            val totals = HashMap<String, Long>()
            val event = UsageEvents.Event()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                @Suppress("DEPRECATION") // MOVE_TO_* funktioniert ab API 21 (minSdk 24)
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND ->
                        foregroundSince[pkg] = event.timeStamp

                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        val start = foregroundSince.remove(pkg) ?: startTime
                        totals[pkg] = (totals[pkg] ?: 0L) + (event.timeStamp - start).coerceAtLeast(0)
                    }
                }
            }
            // Apps, die am Fensterende noch im Vordergrund sind (kein Background-Event).
            for ((pkg, start) in foregroundSince) {
                totals[pkg] = (totals[pkg] ?: 0L) + (now - start).coerceAtLeast(0)
            }

            totals
                .filterKeys { it != ownPackage }
                .filterValues { it > 0 }
                .map { (pkg, dur) -> AppUsageSample(pkg, resolveAppName(pkg), dur) }
        }

    override suspend fun getScreenOffMillis(start: Long, end: Long): Long =
        withContext(Dispatchers.IO) {
            // Screen on/off-Events gibt es erst ab Android 9 (API 28).
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
                return@withContext 0L
            }
            val events = usageStatsManager.queryEvents(start, end)
            val event = UsageEvents.Event()
            var offSince: Long? = null
            var total = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE ->
                        if (offSince == null) offSince = event.timeStamp

                    UsageEvents.Event.SCREEN_INTERACTIVE -> {
                        offSince?.let { total += (event.timeStamp - it).coerceAtLeast(0) }
                        offSince = null
                    }
                }
            }
            // War der Bildschirm am Fensterende noch aus, bis zum Ende mitzählen.
            offSince?.let { total += (end - it).coerceAtLeast(0) }
            total
        }

    /** Menschlich lesbarer App-Name; fällt auf den Paketnamen zurück. */
    private fun resolveAppName(packageName: String): String =
        try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
}
