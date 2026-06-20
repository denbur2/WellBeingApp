package com.project.wellbeingapp

import android.app.Application
import com.project.wellbeingapp.di.AppContainer
import com.project.wellbeingapp.services.ScoreSnapshotWorker
import org.osmdroid.config.Configuration

/**
 * Hält den App-weiten [AppContainer]. In der Manifest-`application`-Tag als
 * `android:name` registriert.
 */
class WellbeingApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // OSMDroid braucht einen User-Agent, sonst liefern die Tile-Server nichts.
        Configuration.getInstance().userAgentValue = packageName

        // Täglichen Score-Snapshot einplanen (idempotent).
        ScoreSnapshotWorker.schedule(this)

        // Billing-Verbindung früh aufbauen, damit Preis & Kaufstatus bereitstehen.
        container.billingManager.start()
    }
}
