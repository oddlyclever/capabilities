package com.oddlyclever.wayfinder.headless

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.coderwise.libs.mapview.LatLon
import com.oddlyclever.wayfinder.track.TrackStyle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins wayfinder-headless's one call: WebP by default and PNG on request, at the size times the scale, the route's start
 * marked where it begins, and refusal of an image with nothing to fit. Runs on the machine's own Skia, which this
 * module bundles.
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

    @Test
    fun tellsImagesFromVectorTiles() {
        assertTrue(isImage(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)), "PNG")
        assertTrue(isImage(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())), "JPEG")
        assertFalse(isImage(byteArrayOf(0x1A, 0x05, 0x77)), "an MVT tile starts with a protobuf field, not an image signature")
    }

    private fun ImageBitmap.any(predicate: (Int) -> Boolean): Boolean {
        val pixels = IntArray(width * height)
        readPixels(pixels)
        return pixels.any(predicate)
    }
}
