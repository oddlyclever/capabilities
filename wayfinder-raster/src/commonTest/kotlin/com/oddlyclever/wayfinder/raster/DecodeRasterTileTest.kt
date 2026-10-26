package com.oddlyclever.wayfinder.raster

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Pins what [decodeRasterTile] makes of a tile server's answer: a real image decodes at its own size, and anything
 * that is not an image comes back null rather than throwing, so a bad tile is a missing tile.
 */
class DecodeRasterTileTest {

    @Test
    fun decodesAPng() = runTest {
        awaitSkia()
        val image = decodeRasterTile(TWO_BY_ONE_PNG)
        assertNotNull(image, "a valid PNG should decode")
        assertEquals(2, image.width, "width was ${image.width}")
        assertEquals(1, image.height, "height was ${image.height}")
    }

    @Test
    fun nothingDecodesToNull() = runTest {
        assertNull(decodeRasterTile(null), "no bytes")
        assertNull(decodeRasterTile(ByteArray(0)), "empty bytes")
    }

    @Test
    fun anErrorPageDecodesToNull() = runTest {
        awaitSkia()
        val page = "<html><body>502 Bad Gateway</body></html>".encodeToByteArray()
        assertNull(decodeRasterTile(page), "an HTML error page is not a tile")
    }

    @Test
    fun aTruncatedImageDecodesToNull() = runTest {
        awaitSkia()
        assertNull(decodeRasterTile(TWO_BY_ONE_PNG.copyOf(20)), "a PNG cut off inside its header is not a tile")
    }

    private companion object {
        /** A 2x1 RGBA PNG, one red pixel and one blue. */
        val TWO_BY_ONE_PNG = byteArrayOf(
            -119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 2, 0, 0, 0, 1, 8, 6, 0, 0, 0,
            -12, 34, 127, -118, 0, 0, 0, 14, 73, 68, 65, 84, 120, -100, 99, -8, -49, -64, 0, 66, -1, 1, 15, -7, 3,
            -3, -123, 17, -103, 118, 0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126,
        )
    }
}
