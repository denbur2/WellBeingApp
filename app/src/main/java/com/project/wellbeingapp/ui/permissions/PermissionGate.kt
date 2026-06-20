package com.project.wellbeingapp.ui.permissions

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.project.wellbeingapp.ui.theme.TerminalDim
import com.project.wellbeingapp.ui.theme.TerminalGreen

/**
 * Blendet [content] erst ein, wenn alle benötigten Rechte vorliegen
 * (Nutzungsdaten, Standort, Notifications). Andernfalls ein Terminal-Gate mit
 * Buttons. Wird bei jedem ON_RESUME neu geprüft (für die Usage-Access-Settings).
 */
@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refresh by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val usage = remember(refresh) { Permissions.hasUsageAccess(context) }
    val location = remember(refresh) { Permissions.hasLocation(context) }
    val notifications = remember(refresh) { Permissions.hasNotifications(context) }

    val locationLauncher = rememberLauncherForActivityResult(RequestMultiplePermissions()) {
        refresh++
    }
    val notifLauncher = rememberLauncherForActivityResult(RequestPermission()) { refresh++ }

    if (usage && location && notifications) {
        content()
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text(
            "WELLBEING // SETUP",
            color = TerminalGreen,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text("> berechtigungen werden zum tracken benoetigt", color = TerminalDim)
        Spacer(Modifier.height(24.dp))

        PermissionRow(
            label = "nutzungsdaten (bildschirmzeit)",
            granted = usage,
            onGrant = {
                context.startActivity(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )
        PermissionRow(
            label = "standort",
            granted = location,
            onGrant = {
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        )
        PermissionRow(
            label = "benachrichtigungen",
            granted = notifications,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onGrant: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = (if (granted) "[x] " else "[ ] ") + label,
            color = if (granted) TerminalGreen else TerminalDim,
            style = MaterialTheme.typography.bodyLarge
        )
        if (!granted) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "> GEWAEHREN",
                color = TerminalGreen,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .border(1.dp, TerminalGreen)
                    .clickable(onClick = onGrant)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}
