package com.oddlyclever.wayfinder.headless

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.TileKey
import com.oddlyclever.wayfinder.DEFAULT_FIT_MAX_ZOOM
import com.oddlyclever.wayfinder.fit
import com.oddlyclever.wayfinder.frame.encodeToPng
import com.oddlyclever.wayfinder.track.TrackStyle
import java.util.Collections
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins wayfinder-headless's one call: WebP by default and PNG on request, at the size times the scale, the route's start
 * marked where it begins, and refusal of an image with nothing to fit. Pins too that the camera's closest zoom is apart
 * from the tile levels: a short route is fitted past the deepest tile level and drawn on magnified tiles, a single point
 * stops at street level, and a long route frames as it did when the tile levels capped the camera. Runs on the
 * machine's own Skia, which this module bundles.
 */
class HeadlessMapTest {

    private val route = listOf(listOf(LatLon(43.64, -79.40), LatLon(43.65, -79.38)), listOf(LatLon(43.66, -79.37), LatLon(43.67, -79.35)))

    /** An empty vector tile everywhere: open ground, so only the lines and markers show. */
    private val emptyTiles: suspend (com.coderwise.libs.mapview.TileKey) -> ByteArray? = { ByteArray(0) }

    @Test
    fun webpByDefaultAtTheScaledSize() = runTest {
        val bytes = renderMapImage(300, 200, lines = route, tiles = emptyTiles, scale = 2f)
        assertEquals("WEBP", bytes.copyOfRange(8, 12).decodeToString(), "WebP payload")
        val image = bytes.decodeToImageBitmap()
        assertEquals(600 to 400, image.width to image.height, "300 x 200 at scale 2")
    }

    @Test
    fun pngOnRequest() = runTest {
        val bytes = renderMapImage(300, 200, lines = route, tiles = emptyTiles, format = ImageFormat.Png)
        assertEquals(0x89, bytes[0].toInt() and 0xFF, "PNG signature")
        assertTrue(bytes.size > 100, "an image, ${bytes.size} bytes")
    }

    @Test
    fun theStartIsMarked() = runTest {
        // Lossless, so the marker's colour reads back exactly. The start is the first point of the first line.
        val bytes = renderMapImage(300, 200, lines = route, tiles = emptyTiles, format = ImageFormat.Png)
        val image = bytes.decodeToImageBitmap()
        val green = TrackStyle().startColor.toArgb()
        assertTrue(image.any { it == green }, "a start marker in its colour somewhere on the image")
    }

    @Test
    fun refusesNothingToFit() = runTest {
        assertFailsWith<IllegalArgumentException> { renderMapImage(300, 200, tiles = emptyTiles) }
    }

    /**
     * Under a kilometre, the kind of short route a tile-level cap used to leave small in the middle, and long enough
     * that its own extent, not the camera's closest zoom, decides the fit.
     */
    private val shortRoute = listOf(LatLon(43.6500, -79.3800), LatLon(43.6550, -79.3740))

    private val size = IntSize(600, 400)
    private val density = Density(1f)

    @Test
    fun aShortRouteIsFittedPastTheTileLevels() {
        val zoom = imageCamera(shortRoute, size, density, 48.dp, DEFAULT_FIT_MAX_ZOOM).zoom
        assertTrue(zoom > DEFAULT_ZOOM_RANGE.last, "a short route closer than the last tile level; got zoom $zoom")
        assertTrue(zoom < DEFAULT_FIT_MAX_ZOOM, "a short route fitted by its extent, not by the cap; got zoom $zoom")
    }

    @Test
    fun aSinglePointStopsAtStreetLevel() {
        val zoom = imageCamera(listOf(LatLon(43.65, -79.38)), size, density, 48.dp, DEFAULT_FIT_MAX_ZOOM).zoom
        assertEquals(DEFAULT_FIT_MAX_ZOOM, zoom, "a single point at the camera's closest zoom")
    }

    @Test
    fun aLongRouteFramesAsBefore() {
        val everything = route.flatten()
        val now = imageCamera(everything, size, density, 48.dp, DEFAULT_FIT_MAX_ZOOM)
        val before = fit(everything, size, density, 48.dp, maxZoom = DEFAULT_ZOOM_RANGE.last.toFloat())
        assertTrue(before.zoom < DEFAULT_ZOOM_RANGE.last, "the route is long enough never to reach the old cap; got zoom ${before.zoom}")
        assertEquals(before.zoom, now.zoom, "the same zoom as when the tile levels capped the camera")
        assertEquals(before.center, now.center, "the same centre as when the tile levels capped the camera")
    }

    @Test
    fun pastTheTileLevelsTheDeepestTilesAreMagnified() = runTest {
        val ground = Color(0xFF336699)
        val tile = ImageBitmap(256, 256).also { Canvas(it).drawRect(0f, 0f, 256f, 256f, Paint().apply { color = ground }) }.encodeToPng()
        val asked = Collections.synchronizedSet(mutableSetOf<TileKey>())
        val bytes = renderMapImage(600, 400, lines = listOf(shortRoute), tiles = { key -> asked += key; tile }, format = ImageFormat.Png)

        val levels = asked.map { it.z }.toSet()
        assertEquals(setOf(DEFAULT_ZOOM_RANGE.last), levels, "tiles asked for at the last tile level only; got $levels")
        val image = bytes.decodeToImageBitmap()
        val corners = listOf(2 to 2, image.width - 3 to 2, 2 to image.height - 3, image.width - 3 to image.height - 3)
        for ((x, y) in corners) {
            val pixel = image.pixel(x, y)
            assertEquals(ground.toArgb(), pixel, "the magnified tile at ($x, $y), not a blank; got ${pixel.toUInt().toString(16)}")
        }
    }

    @Test
    fun tellsImagesFromVectorTiles() {
        assertTrue(isImage(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)), "PNG")
        assertTrue(isImage(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())), "JPEG")
        assertFalse(isImage(byteArrayOf(0x1A, 0x05, 0x77)), "an MVT tile starts with a protobuf field, not an image signature")
    }

    private fun ImageBitmap.pixel(x: Int, y: Int): Int = IntArray(1).also { readPixels(it, x, y, 1, 1) }[0]

    private fun ImageBitmap.any(predicate: (Int) -> Boolean): Boolean {
        val pixels = IntArray(width * height)
        readPixels(pixels)
        return pixels.any(predicate)
    }
}
