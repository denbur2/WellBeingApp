package com.project.wellbeingapp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.project.wellbeingapp.di.AppContainer
import com.project.wellbeingapp.di.AppViewModelFactory
import com.project.wellbeingapp.ui.heatmap.HeatmapScreen
import com.project.wellbeingapp.ui.heatmap.HeatmapViewModel
import com.project.wellbeingapp.ui.history.HistoryScreen
import com.project.wellbeingapp.ui.history.HistoryViewModel
import com.project.wellbeingapp.ui.score.ScoreScreen
import com.project.wellbeingapp.ui.score.ScoreViewModel
import com.project.wellbeingapp.ui.settings.SettingsScreen
import com.project.wellbeingapp.ui.stats.StatsScreen
import com.project.wellbeingapp.ui.stats.StatsViewModel
import com.project.wellbeingapp.ui.theme.TerminalBackground
import com.project.wellbeingapp.ui.theme.TerminalDim
import com.project.wellbeingapp.ui.theme.TerminalGreen
import kotlinx.coroutines.launch

/** Navigationsziele mit Route und Menü-Label. */
enum class Dest(val route: String, val label: String) {
    Score("score", "SCORE"),
    History("history", "VERLAUF"),
    Stats("stats", "STATS"),
    Heatmap("heatmap", "HEATMAP"),
    Settings("settings", "SETTINGS")
}

/**
 * Wurzel-Composable: ein [ModalNavigationDrawer] (Terminal-Stil), der über das
 * Hamburger-Symbol oben links geöffnet wird, plus der [NavHost] mit allen Screens.
 */
@Composable
fun WellbeingNav(container: AppContainer) {
    val factory = remember { AppViewModelFactory(container) }
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Öffnen nur per Tipp auf das ≡-Symbol — kein Wischen von links.
        // Im geöffneten Zustand bleibt Wischen zum Schließen erlaubt.
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            TerminalDrawer(
                current = currentRoute,
                onSelect = { dest ->
                    scope.launch { drawerState.close() }
                    if (dest.route != currentRoute) {
                        nav.navigate(dest.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            TerminalTopBar(
                title = Dest.entries.firstOrNull { it.route == currentRoute }?.label ?: "",
                onMenuClick = { scope.launch { drawerState.open() } }
            )
            NavHost(
                navController = nav,
                startDestination = Dest.Score.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Dest.Score.route) {
                    val vm: ScoreViewModel = viewModel(factory = factory)
                    val state by vm.uiState.collectAsState()
                    ScoreScreen(state)
                }
                composable(Dest.History.route) {
                    val vm: HistoryViewModel = viewModel(factory = factory)
                    val state by vm.uiState.collectAsState()
                    HistoryScreen(state)
                }
                composable(Dest.Stats.route) {
                    val vm: StatsViewModel = viewModel(factory = factory)
                    val state by vm.uiState.collectAsState()
                    StatsScreen(state, onSelectRange = vm::selectRange)
                }
                composable(Dest.Heatmap.route) {
                    val vm: HeatmapViewModel = viewModel(factory = factory)
                    val state by vm.uiState.collectAsState()
                    HeatmapScreen(
                        state,
                        onSelectApp = vm::selectApp,
                        onSelectRange = vm::selectRange
                    )
                }
                composable(Dest.Settings.route) {
                    val trackingEnabled by container.appPreferences.trackingEnabled.collectAsState()
                    SettingsScreen(
                        trackingEnabled = trackingEnabled,
                        onSetTracking = container.appPreferences::setTrackingEnabled,
                        onResetData = { scope.launch { container.clearAllData() } },
                        onAddPoints = container.devSettings::add
                    )
                }
            }
        }
    }
}

/** Schlanke Kopfzeile mit Hamburger-Symbol (≡) links und Sektionstitel. */
@Composable
private fun TerminalTopBar(title: String, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "≡",
            color = TerminalGreen,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .clickable(onClick = onMenuClick)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            color = TerminalDim,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** Drawer-Inhalt im Terminal-Stil: Kopfzeile + anklickbare Ziel-Liste. */
@Composable
private fun TerminalDrawer(current: String?, onSelect: (Dest) -> Unit) {
    ModalDrawerSheet(
        drawerContainerColor = TerminalBackground,
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(TerminalBackground)
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            Text(
                text = "WELLBEING",
                color = TerminalGreen,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "> menue",
                color = TerminalDim,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(24.dp))

            Dest.entries.forEach { dest ->
                val active = dest.route == current
                Text(
                    text = if (active) "[ ${dest.label} ]" else "  ${dest.label}",
                    color = if (active) TerminalGreen else TerminalDim,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(dest) }
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}
