package com.project.wellbeingapp.ui.heatmap

import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RadialGradient
import android.graphics.Shader
import com.project.wellbeingapp.heatmap.HeatColor
import com.project.wellbeingapp.heatmap.HeatmapCell
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.cos
import kotlin.math.pow

/**
 * Zeichnet die Heatmap als weiche Farbkreise über die Karte. Die Farbe kommt aus
 * der festen Skala [HeatColor]; geringere Intensität wird zuerst gezeichnet, damit
 * kräftige Stellen oben liegen.
 *
 * „Soßig"/breiig: Alle Blobs werden in einen Offscreen-Layer gezeichnet und beim
 * Zurückschreiben mit einem Alpha-Schwellwert-Filter („Gooey"-Filter) verschmolzen.
 * Dadurch fließen nah beieinander liegende Punkte zu einer zusammenhängenden Masse
 * zusammen, statt als getrennte Kreise zu erscheinen.
 *
 * Dynamisch beim Zoomen: Der Blob-Radius wird in Metern angegeben und je Zoomstufe
 * in Pixel umgerechnet (geclampt), damit die Heat beim Rein-/Rauszoomen mitatmet.
 */
class HeatmapOverlay : Overlay() {

    private var cells: List<HeatmapCell> = emptyList()
    private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val point = Point()

    /** Ziel-Radius eines Heat-Blobs in Metern (wird je Zoom in Pixel umgerechnet). */
    var radiusMeters: Float = 75f

    /** Pixel-Grenzen, damit Blobs beim Rein-/Rauszoomen nicht verschwinden/explodieren. */
    var minRadiusPx: Float = 18f
    var maxRadiusPx: Float = 130f

    // „Gooey"-Filter: hebt die Alpha-Kontur stark an. Weiche, überlappende Blobs
    // werden so zu einer durchgehenden „Soße" verschmolzen, mit glatter Außenkante.
    private val gooFilter = ColorMatrixColorFilter(
        ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 22f, -8f * 255f
            )
        )
    )
    // Nur der Gooey-Filter, KEIN Alpha hier: sonst wird das Alpha vor dem Filter
    // gesenkt, fällt unter dessen Schwelle und alle Blobs verschwinden.
    private val layerPaint = Paint().apply { colorFilter = gooFilter }

    /**
     * Gesamt-Transparenz der Heatmap (0 = unsichtbar, 255 = voll deckend).
     * Wird über eine eigene Ebene NACH dem Gooey-Filter angewendet.
     */
    var overlayAlpha: Int = 90
        set(value) { field = value.coerceIn(0, 255) }

    fun setCells(newCells: List<HeatmapCell>) {
        cells = newCells.sortedBy { it.intensity }
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || cells.isEmpty()) return
        val projection = mapView.projection
        val radiusPx = radiusForZoom(mapView)

        // Äußere Ebene: senkt die Deckkraft der FERTIGEN (gegooten) Heatmap gleichmäßig.
        val alphaSave = canvas.saveLayerAlpha(null, overlayAlpha)
        // Offscreen-Layer: erst alle Blobs sammeln, dann verschmolzen zurückschreiben.
        val save = canvas.saveLayer(null, layerPaint)
        for (cell in cells) {
            projection.toPixels(GeoPoint(cell.latitude, cell.longitude), point)
            val center = HeatColor.argb(cell.intensity)
            val edge = center and 0x00FFFFFF // gleiche Farbe, Alpha 0 → weicher Rand
            // Solider Kern + weicher Abfall: liefert dem Gooey-Filter die nötige
            // Alpha-Rampe, damit benachbarte Blobs eine Brücke bilden (verschmelzen).
            blobPaint.shader = RadialGradient(
                point.x.toFloat(), point.y.toFloat(), radiusPx,
                intArrayOf(center, center, edge),
                floatArrayOf(0f, 0.35f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radiusPx, blobPaint)
        }
        blobPaint.shader = null
        canvas.restoreToCount(save)
        canvas.restoreToCount(alphaSave)
    }

    /** Rechnet [radiusMeters] für die aktuelle Zoomstufe in Pixel um (geclampt). */
    private fun radiusForZoom(mapView: MapView): Float {
        val lat = mapView.mapCenter.latitude
        val metersPerPixel =
            156543.03392 * cos(Math.toRadians(lat)) / 2.0.pow(mapView.zoomLevelDouble)
        return (radiusMeters / metersPerPixel).toFloat().coerceIn(minRadiusPx, maxRadiusPx)
    }
}
