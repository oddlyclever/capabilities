package com.oddlyclever.kartog.staticmap

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.TileKey
import com.oddlyclever.kartog.CameraPosition
import com.oddlyclever.kartog.StaticLayer
import com.oddlyclever.kartog.StaticTile
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Pins kartog-static: that a rendered map is covered by its layers' tiles, falls back to ancestors as a live map does
 * and to the background where there are none; that a track marks its ends, and a loop's ends as one; and that the
 * image encodes as PNG and WebP.
 */
class RenderMapTest {

    private val size = IntSize(200, 120)
    private val density = Density(1f)
    private val toronto = CameraPosition(LatLon(43.65, -79.38), zoom = 12.4f)

    @Test
    fun layersCoverTheImage() = runTest {
        awaitSkia()
        val image = renderMap(size, density, toronto, listOf(solid(0..19, Color.Red)), background = Color.Blue)
        for ((x, y) in listOf(0 to 0, 100 to 60, 199 to 119)) {
            assertEquals(Color.Red.toArgb(), image.pixel(x, y), "pixel ($x, $y) should be covered by a tile")
        }
    }

    @Test
    fun aTurnedMapIsStillCovered() = runTest {
        awaitSkia()
        val turned = CameraPosition(toronto.center, toronto.zoom, bearing = 37f)
        val image = renderMap(size, density, turned, listOf(solid(0..19, Color.Red)), background = Color.Blue)
        for ((x, y) in listOf(0 to 0, 199 to 0, 0 to 119, 199 to 119)) {
            assertEquals(Color.Red.toArgb(), image.pixel(x, y), "corner ($x, $y) of a turned map should be covered")
        }
    }

    @Test
    fun aMissingTileIsDrawnFromItsAncestor() = runTest {
        awaitSkia()
        // The layer has tiles down to z10 only, but the map is at z12: every cell is drawn from a z10 ancestor.
        val shallow = solid(0..19, Color.Red, deepest = 10)
        val image = renderMap(size, density, toronto, listOf(shallow), background = Color.Blue)
        assertEquals(Color.Red.toArgb(), image.pixel(100, 60), "the middle should come from an ancestor")
    }

    @Test
    fun theBackgroundShowsWhereThereAreNoTiles() = runTest {
        awaitSkia()
        val empty = solid(0..19, Color.Red, deepest = -1)
        val image = renderMap(size, density, toronto, listOf(empty), background = Color.Blue)
        assertEquals(Color.Blue.toArgb(), image.pixel(100, 60), "no tiles anywhere, so the background")
    }

    @Test
    fun theProjectionPutsTheCentreInTheMiddle() {
        val at = StaticProjection(toronto, size, density).project(toronto.center)
        assertTrue(abs(at.x - 100f) < 0.01f && abs(at.y - 60f) < 0.01f, "centre at $at")
    }

    @Test
    fun aTrackMarksItsStartAndEnd() = runTest {
        awaitSkia()
        val track = listOf(TrackPoint(LatLon(43.64, -79.40)), TrackPoint(LatLon(43.66, -79.36)))
        val style = TrackStyle(line = LineStyle.None, speed = SpeedStyle.None)
        var start = androidx.compose.ui.geometry.Offset.Zero
        var end = androidx.compose.ui.geometry.Offset.Zero
        val image = renderMap(size, density, toronto, emptyList()) { projection ->
            start = projection.project(track.first().position)
            end = projection.project(track.last().position)
            drawTrack(track, projection, style)
        }
        assertEquals(style.startColor.toArgb(), image.pixel(start.x.toInt(), start.y.toInt()), "start at $start")
        assertEquals(style.endColor.toArgb(), image.pixel(end.x.toInt(), end.y.toInt()), "end at $end")
    }

    @Test
    fun aLoopMarksBothEndsInOne() = runTest {
        awaitSkia()
        val here = LatLon(43.65, -79.38)
        val loop = listOf(TrackPoint(here), TrackPoint(LatLon(43.66, -79.37)), TrackPoint(here))
        val style = TrackStyle(line = LineStyle.None, speed = SpeedStyle.None)
        val image = renderMap(size, density, toronto, emptyList()) { drawTrack(loop, it, style) }
        // The end is drawn as a ring with the start inside it: the middle shows the start's colour.
        assertEquals(style.startColor.toArgb(), image.pixel(100, 60), "a loop's start shows inside its end")
        assertEquals(style.endColor.toArgb(), image.pixel(100 + 5, 60), "and the end shows round it, between the inner dot (3.3 px) and the ring edge (7 px)")
    }

    @Test
    fun encodesAsPngAndBack() = runTest {
        awaitSkia()
        val image = renderMap(size, density, toronto, listOf(solid(0..19, Color.Red)))
        val png = image.encodeToPng()
        assertEquals(listOf(0x89, 0x50, 0x4E, 0x47), png.take(4).map { it.toInt() and 0xFF }, "PNG signature")
        val decoded = png.decodeToImageBitmap()
        assertEquals(size.width to size.height, decoded.width to decoded.height, "the PNG should keep the image's size")
    }

    @Test
    fun encodesAsWebp() = runTest {
        awaitSkia()
        val webp = renderMap(size, density, toronto, listOf(solid(0..19, Color.Red))).encodeToWebp()
        assertEquals("RIFF", webp.copyOfRange(0, 4).decodeToString(), "RIFF container")
        assertEquals("WEBP", webp.copyOfRange(8, 12).decodeToString(), "WebP payload")
    }

    @Test
    fun refusesAnEmptySize() = runTest {
        assertFailsWith<IllegalArgumentException> { renderMap(IntSize(0, 10), density, toronto, emptyList()) }
    }

    /** A layer whose every tile down to [deepest] is one solid colour. */
    private fun solid(zoomRange: IntRange, color: Color, deepest: Int = zoomRange.last) = object : StaticLayer {
        override val zoomRange = zoomRange
        override suspend fun tile(key: TileKey): StaticTile? =
            if (key.z <= deepest) StaticTile { _, _ -> drawRect(color) } else null
    }

    private fun ImageBitmap.pixel(x: Int, y: Int): Int = IntArray(1).also { readPixels(it, x, y, 1, 1) }[0]
}
