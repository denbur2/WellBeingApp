package com.project.wellbeingapp.ui.paywall

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.project.wellbeingapp.billing.BillingManager
import com.project.wellbeingapp.billing.BillingStatus
import com.project.wellbeingapp.di.AppContainer
import com.project.wellbeingapp.ui.theme.TerminalDim
import com.project.wellbeingapp.ui.theme.TerminalError
import com.project.wellbeingapp.ui.theme.TerminalGreen

/**
 * Hart blockendes Paywall-Gate: Sobald der Score Level 2 erreicht
 * (`level > 1`) und die Premium-Freischaltung nicht vorliegt, ersetzt der
 * [PaywallScreen] die gesamte App, bis der Kauf getätigt wurde. Bei jedem
 * ON_RESUME wird der Kaufstatus neu abgefragt (z. B. nach externem Kauf).
 */
@Composable
fun PaywallGate(container: AppContainer, content: @Composable () -> Unit) {
    val billing = container.billingManager
    val isPremium by billing.isPremium.collectAsState()
    val score by container.scoreCalculator.observeCurrentScore().collectAsState(initial = null)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) billing.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val level = score?.level ?: 1
    if (!isPremium && level > 1) {
        PaywallScreen(billing)
    } else {
        content()
    }
}

/** Vollbild-Paywall im Terminal-Stil mit Kauf-Button. */
@Composable
fun PaywallScreen(billing: BillingManager) {
    val context = LocalContext.current
    val price by billing.price.collectAsState()
    val status by billing.status.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "WELLBEING // PREMIUM",
            color = TerminalGreen,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "> level 1 abgeschlossen",
            color = TerminalDim,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "weitere nutzung erfordert die premium-freischaltung.",
            color = TerminalDim,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        when (status) {
            BillingStatus.ERROR -> {
                Text(
                    text = "> store nicht erreichbar",
                    color = TerminalError,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(16.dp))
                TerminalButton(label = "> ERNEUT VERSUCHEN") { billing.start() }
            }

            else -> {
                val label = price?.let { "> FREISCHALTEN ($it)" } ?: "> FREISCHALTEN"
                TerminalButton(label = label) {
                    context.findActivity()?.let { billing.launchPurchase(it) }
                }
            }
        }
    }
}

@Composable
private fun TerminalButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = TerminalGreen,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TerminalGreen)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        textAlign = TextAlign.Center
    )
}

/** Findet die umschließende [Activity] aus einem Compose-[Context]. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
