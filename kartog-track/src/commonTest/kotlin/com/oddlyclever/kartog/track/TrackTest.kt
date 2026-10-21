package com.oddlyclever.kartog.track

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.coderwise.libs.mapview.LatLon
import com.oddlyclever.kartog.MapProjection
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins how a track is marked: its start and end in their own colours, a loop's two ends as one marker with the start
 * inside the end, and an accuracy circle drawn only when the last fix carries one.
 */
class TrackTest {

    /** Longitude is x and latitude is y, a pixel a unit, at 10 metres a pixel, north up. */
    private val flat = object : MapProjection {
        override fun project(point: LatLon) = Offset(point.lon.toFloat(), point.lat.toFloat())
        override val metresPerPixel = 10.0
        override val bearing = 0f
    }
    private val plain = TrackStyle(line = LineStyle.None, speed = SpeedStyle.None, heading = HeadingMarker.None)

    @Test
    fun aTrackMarksItsStartAndEnd() = runTest {
        awaitSkia()
        val image = draw(listOf(TrackPoint(LatLon(20.0, 20.0)), TrackPoint(LatLon(60.0, 150.0))), plain)
        assertEquals(plain.startColor.toArgb(), image.pixel(20, 20), "start")
        assertEquals(plain.endColor.toArgb(), image.pixel(150, 60), "end")
    }

    @Test
    fun aLoopMarksBothEndsInOne() = runTest {
        awaitSkia()
        val here = LatLon(50.0, 100.0)
        val image = draw(listOf(TrackPoint(here), TrackPoint(LatLon(70.0, 140.0)), TrackPoint(here)), plain)
        assertEquals(plain.startColor.toArgb(), image.pixel(100, 50), "a loop's start shows inside its end")
        assertEquals(plain.endColor.toArgb(), image.pixel(105, 50), "and the end shows round it")
    }

    @Test
    fun anAccuracyCircleOnlyWhenKnown() = runTest {
        awaitSkia()
        val style = plain.copy(showAccuracy = true)
        // 300 m at 10 m a pixel is a 30 px radius; 20 px out from the last fix is inside it.
        val known = draw(listOf(TrackPoint(LatLon(10.0, 10.0)), TrackPoint(LatLon(50.0, 100.0), accuracy = 300.0)), style)
        // Translucent, so read back through premultiplied alpha: compare the alpha, which survives exactly.
        assertEquals(style.accuracyColor.toArgb() ushr 24, known.pixel(120, 50) ushr 24, "inside a known accuracy circle")
        val unknown = draw(listOf(TrackPoint(LatLon(10.0, 10.0)), TrackPoint(LatLon(50.0, 100.0))), style)
        assertEquals(0, unknown.pixel(120, 50), "no circle when the accuracy is unknown, so nothing guessed")
    }

    private fun draw(points: List<TrackPoint>, style: TrackStyle): ImageBitmap {
        val image = ImageBitmap(200, 100)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(200f, 100f)) {
            drawTrack(points, flat, style)
        }
        return image
    }

    private fun ImageBitmap.pixel(x: Int, y: Int): Int = IntArray(1).also { readPixels(it, x, y, 1, 1) }[0]
}
