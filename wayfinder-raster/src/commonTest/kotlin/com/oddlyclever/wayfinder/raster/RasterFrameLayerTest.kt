package com.oddlyclever.wayfinder.raster

import com.coderwise.libs.mapview.TileKey
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Pins that a raster frame layer hands the renderer a tile only for bytes that decode, and none for the rest. */
class RasterFrameLayerTest {

    private val key = TileKey(3, 2, 1)

    @Test
    fun aDecodableTileIsATile() = runTest {
        awaitSkia()
        val layer = rasterFrameLayer(0..19) { PNG }
        assertNotNull(layer.tile(key), "a PNG should become a tile")
    }

    @Test
    fun missingOrBrokenBytesAreNoTile() = runTest {
        awaitSkia()
        assertNull(rasterFrameLayer(0..19) { null }.tile(key), "no bytes, no tile")
        assertNull(rasterFrameLayer(0..19) { "not an image".encodeToByteArray() }.tile(key), "not an image, no tile")
    }

    private companion object {
        /** A 2x1 RGBA PNG. */
        val PNG = byteArrayOf(
            -119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 2, 0, 0, 0, 1, 8, 6, 0, 0, 0,
            -12, 34, 127, -118, 0, 0, 0, 14, 73, 68, 65, 84, 120, -100, 99, -8, -49, -64, 0, 66, -1, 1, 15, -7, 3,
            -3, -123, 17, -103, 118, 0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126,
        )
    }
}
