package com.project.wellbeingapp.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.project.wellbeingapp.R
import com.project.wellbeingapp.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Foreground-Service, der den [TrackingCoordinator] hostet. Läuft dauerhaft mit
 * persistenter Notification weiter, auch wenn die App im Hintergrund ist.
 *
 * Start/Stop über [start] / [stop] (statische Helfer).
 */
class TrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob())
    private lateinit var coordinator: TrackingCoordinator

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(applicationContext)
        coordinator = DefaultTrackingCoordinator(
            scope = scope,
            locationProvider = AndroidLocationProvider(applicationContext),
            appUsageProvider = AndroidAppUsageProvider(applicationContext),
            locationDao = db.locationDao(),
            appUsageDao = db.appUsageDao()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        coordinator.start()
        // Bei System-Kill neu starten, damit das Tracking weiterläuft.
        return START_STICKY
    }

    override fun onDestroy() {
        coordinator.stop()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        createChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.tracking_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.tracking_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "tracking"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TrackingService::class.java))
        }
    }
}
