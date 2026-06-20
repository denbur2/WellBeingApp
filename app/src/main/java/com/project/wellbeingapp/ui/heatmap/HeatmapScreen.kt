package com.project.wellbeingapp.ui.heatmap

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.project.wellbeingapp.heatmap.HeatColor
import com.project.wellbeingapp.ui.common.RangeToggle
import com.project.wellbeingapp.ui.common.TimeRange
import com.project.wellbeingapp.ui.theme.TerminalBackground
import com.project.wellbeingapp.ui.theme.TerminalDim
import com.project.wellbeingapp.ui.theme.TerminalGreen
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * Heatmap-Screen: oben die App-Auswahl (Chips), darunter eine dezente OSMDroid-Karte
 * mit Heat-Overlay. Beim App-Wechsel ändern sich nur die Heat-Zellen — die Karte
 * (Kameraposition/Zoom) bleibt erhalten, weil [MapView] über Recompositions gehalten wird.
 */
@Composable
fun HeatmapScreen(
    state: HeatmapUiState,
    onSelectApp: (String?) -> Unit,
    onSelectRange: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // MapView + Overlay über Recompositions hinweg stabil halten.
    val overlay = remember { HeatmapOverlay() }
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            // Karte dezent: entsättigt + abgedunkelt, damit der Heat heraussticht.
            val cm = ColorMatrix().apply { setSaturation(0.25f) }
            cm.postConcat(dimMatrix())
            overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(cm))
            overlays.add(overlay)
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause(); mapView.onDetach() }
    }

    Column(modifier = modifier.fillMaxSize().background(TerminalBackground)) {
        Text(
            text = "WELLBEING // HEATMAP",
            color = TerminalGreen,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        RangeToggle(
            selected = state.range,
            onSelect = onSelectRange,
            modifier = Modifier.padding(horizontal = 16.dp),
            label = { if (it == TimeRange.DAY) "24H" else it.label }
        )
        Spacer(Modifier.height(8.dp))

        AppSelector(
            apps = state.availableApps.map { it.appName to it.packageName },
            selected = state.selectedApp,
            onSelect = onSelectApp
        )

        // Karte nimmt nur den verbleibenden Platz ein. clipToBounds verhindert, dass
        // die OSMDroid-MapView über ihren Bereich hinaus nach oben zeichnet und so
        // Titel + App-Auswahl verdeckt.
        Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { map ->
                    overlay.setCells(state.cells)
                    // Kamera nur einmal sinnvoll setzen, danach Nutzer-Steuerung lassen.
                    fitOnce(map, state)
                    map.invalidate()
                }
            )
            if (state.cells.isEmpty() && !state.isLoading) {
                Text(
                    "> keine standortdaten im zeitraum",
                    color = TerminalDim,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.cells.isNotEmpty()) {
                HeatLegend()
            }
        }
    }
}

/** Farb-Legende: zeigt die Nutzungs-Skala (Cyan = wenig → Tiefblau = viel) je Rasterzelle. */
@Composable
private fun BoxScope.HeatLegend() {
    val low = Color(HeatColor.argb(HeatColor.MIN_MINUTES)).copy(alpha = 1f)
    val mid = Color(HeatColor.argb((HeatColor.MIN_MINUTES + HeatColor.MAX_MINUTES) / 2f)).copy(alpha = 1f)
    val high = Color(HeatColor.argb(HeatColor.MAX_MINUTES)).copy(alpha = 1f)

    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(12.dp)
            .background(TerminalBackground.copy(alpha = 0.75f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text("NUTZUNG / ZELLE", color = TerminalDim, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .width(140.dp)
                .height(10.dp)
                .background(Brush.horizontalGradient(listOf(low, mid, high)))
        )
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.width(140.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("1 min", color = TerminalDim, style = MaterialTheme.typography.labelSmall)
            Text("60+ min", color = TerminalDim, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AppSelector(
    apps: List<Pair<String, String>>, // (appName, packageName)
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalBackground)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Chip(label = "GESAMT", active = selected == null) { onSelect(null) }
        apps.forEach { (name, pkg) ->
            Spacer(Modifier.width(8.dp))
            Chip(label = name, active = selected == pkg) { onSelect(pkg) }
        }
    }
}

@Composable
private fun Chip(label: String, active: Boolean, onClick: () -> Unit) {
    val color = if (active) TerminalGreen else TerminalDim
    Box(
        Modifier
            .border(1.dp, color)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (active) "[$label]" else label,
            color = color,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** Dunkelt die Tiles ab (Faktor < 1 auf RGB). */
private fun dimMatrix(): ColorMatrix {
    val f = 0.55f
    return ColorMatrix(
        floatArrayOf(
            f, 0f, 0f, 0f, 0f,
            0f, f, 0f, 0f, 0f,
            0f, 0f, f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

/**
 * Setzt die Kamera einmalig so, dass möglichst alle Heat-Zellen ins Bild passen
 * (markiert via Tag). Bei einer einzelnen Zelle wird zentriert + sinnvoll gezoomt.
 */
private fun fitOnce(map: MapView, state: HeatmapUiState) {
    if (map.tag == true) return
    val cells = state.cells
    if (cells.isEmpty() || map.width == 0 || map.height == 0) return

    val lats = cells.map { it.latitude }
    val lons = cells.map { it.longitude }
    if (cells.size == 1) {
        map.controller.setZoom(16.0)
        map.controller.setCenter(GeoPoint(lats.first(), lons.first()))
    } else {
        val box = BoundingBox(lats.max(), lons.max(), lats.min(), lons.min())
            .increaseByScale(1.3f) // etwas Rand um die Punkte
        map.zoomToBoundingBox(box, false, 80)
    }
    map.tag = true
}
