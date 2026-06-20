package com.project.wellbeingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.project.wellbeingapp.services.TrackingService
import com.project.wellbeingapp.ui.navigation.WellbeingNav
import com.project.wellbeingapp.ui.permissions.PermissionGate
import com.project.wellbeingapp.ui.theme.WellbeingAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as WellbeingApplication).container

        setContent {
            WellbeingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionGate {
                        // Rechte liegen vor → Tracking starten und App anzeigen.
                        LaunchedEffect(Unit) { TrackingService.start(this@MainActivity) }
                        WellbeingNav(container)
                    }
                }
            }
        }
    }
}
