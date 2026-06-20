package com.project.wellbeingapp.ui.heatmap

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RadialGradient
import android.graphics.Shader
import com.project.wellbeingapp.heatmap.HeatColor
import com.project.wellbeingapp.heatmap.HeatmapCell
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Zeichnet die Heatmap als weiche Farbkreise über die Karte. Die Farbe kommt aus
 * der festen Skala [HeatColor]; geringere Intensität wird zuerst gezeichnet, damit
 * kräftige Stellen oben liegen.
 */
class HeatmapOverlay : Overlay() {

    private var cells: List<HeatmapCell> = emptyList()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val point = Point()

    /** Radius eines Heat-Punkts in Pixeln. */
    var radiusPx: Float = 44f

    fun setCells(newCells: List<HeatmapCell>) {
        cells = newCells.sortedBy { it.intensity }
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || cells.isEmpty()) return
        val projection = mapView.projection

        // Wichtig: volles Alpha. Der Shader trägt sein Alpha selbst — würde das
        // Paint-Alpha < 255 bleiben, multipliziert es die Farbe herunter (früher
        // Bug: ein Zurücksetzen auf TRANSPARENT machte ab Frame 2 alles unsichtbar).
        paint.alpha = 255

        for (cell in cells) {
            projection.toPixels(GeoPoint(cell.latitude, cell.longitude), point)
            val center = HeatColor.argb(cell.intensity)
            val edge = center and 0x00FFFFFF // gleiche Farbe, Alpha 0 → weicher Rand
            paint.shader = RadialGradient(
                point.x.toFloat(), point.y.toFloat(), radiusPx,
                center, edge, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radiusPx, paint)
        }
        paint.shader = null
    }
}
