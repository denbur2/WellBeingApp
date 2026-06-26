package com.project.wellbeingapp.ui.heatmap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
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
import androidx.compose.runtime.rememberUpdatedState
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
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

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

    // Immer aktueller State für Callbacks (First-Layout-Listener läuft außerhalb der Recomposition).
    val latestState = rememberUpdatedState(state)

    // MapView + Overlay über Recompositions hinweg stabil halten.
    val overlay = remember { HeatmapOverlay() }
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            // Keine +/–-Zoom-Buttons; Zoom nur per Geste.
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(15.0)
            // Karte dezent: entsättigt + abgedunkelt, damit der Heat heraussticht.
            val cm = ColorMatrix().apply { setSaturation(0.25f) }
            cm.postConcat(dimMatrix())
            overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(cm))
            overlays.add(overlay)
            // Kamera positionieren, sobald die Karte ausgelegt ist — sonst bleibt sie
            // bis zur nächsten Recomposition (z. B. Button-Druck) uninitialisiert.
            addOnFirstLayoutListener { _, _, _, _, _ ->
                overlay.setCells(latestState.value.cells)
                fitOnce(this, latestState.value)
                invalidate()
            }
        }
    }

    // Eigener Standort nur zum „zu mir springen"; GPS erst bei Button-Druck aktiv.
    // Standort als grüner Ring (App-Grün, ohne Füllung); Richtungspfeil ausgeblendet.
    val myLocation = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context.applicationContext), mapView).also {
            val blank = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            it.setPersonIcon(greenRingIcon(context.resources.displayMetrics.density))
            it.setPersonAnchor(0.5f, 0.5f) // Ring auf den Standort zentrieren
            it.setDirectionIcon(blank)
            it.isDrawAccuracyEnabled = false // GPS-Genauigkeitskreis ausblenden
            mapView.overlays.add(it)
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            myLocation.disableMyLocation()
            mapView.onPause(); mapView.onDetach()
        }
    }

    // Zentriert die Karte auf die aktuelle Position (aktiviert dafür das GPS-Overlay).
    fun centerOnUser() {
        myLocation.enableMyLocation()
        val fix = myLocation.myLocation
        if (fix != null) {
            mapView.controller.animateTo(fix)
            mapView.controller.setZoom(17.0)
        } else {
            // Noch kein Fix → auf den ersten warten und dann springen (Callback off-thread).
            myLocation.runOnFirstFix {
                mapView.post {
                    myLocation.myLocation?.let {
                        mapView.controller.animateTo(it)
                        mapView.controller.setZoom(17.0)
                    }
                }
            }
        }
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

            // Ortungs-Button: immer verfügbar, auch auf leerer Karte.
            LocateButton(
                onClick = { centerOnUser() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            )
        }
    }
}

/** Farb-Legende: Nutzungs-Skala (Cyan = wenig → Tiefblau → Rot = sehr viel) je Rasterzelle. */
@Composable
private fun BoxScope.HeatLegend() {
    val low = Color(HeatColor.argb(HeatColor.MIN_MINUTES)).copy(alpha = 1f)
    val mid = Color(HeatColor.argb(HeatColor.MAX_MINUTES / 2f)).copy(alpha = 1f)
    val high = Color(HeatColor.argb(HeatColor.MAX_MINUTES)).copy(alpha = 1f)
    val hot = Color(HeatColor.argb(HeatColor.HOT_MINUTES)).copy(alpha = 1f)

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
                .background(Brush.horizontalGradient(listOf(low, mid, high, hot)))
        )
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.width(140.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("1 min", color = TerminalDim, style = MaterialTheme.typography.labelSmall)
            Text("2h+", color = TerminalDim, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Button zum Zentrieren der Karte auf die aktuelle Position. */
@Composable
private fun LocateButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(TerminalBackground.copy(alpha = 0.75f))
            .border(1.dp, TerminalGreen)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = "> ORTUNG",
            color = TerminalGreen,
            style = MaterialTheme.typography.labelLarge
        )
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

/**
 * Zeichnet das Standort-Icon als grünen Kreis ohne Füllung (App-Grün, nur Kontur).
 * Größe/Strichstärke skalieren mit der Display-Dichte.
 */
private fun greenRingIcon(density: Float): Bitmap {
    val size = (22f * density).toInt().coerceAtLeast(14)
    val stroke = 1.5f * density
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = 0xFF33FF66.toInt() // TerminalGreen
    }
    val r = size / 2f - stroke
    canvas.drawCircle(size / 2f, size / 2f, r, paint)
    return bmp
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
