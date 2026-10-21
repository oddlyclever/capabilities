package com.oddlyclever.kartog.vector

import com.coderwise.libs.mapview.TileKey
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Pins that a static vector layer hands the renderer a tile for any bytes map-view's decoder reads (an empty tile is
 * a tile of open ground) and none when there are no bytes.
 */
class StaticVectorLayerTest {

    private val key = TileKey(3, 2, 1)

    @Test
    fun bytesAreATile() = runTest {
        assertNotNull(staticVectorLayer(0..14) { ByteArray(0) }.tile(key), "an empty tile is still ground to draw")
    }

    @Test
    fun noBytesAreNoTile() = runTest {
        assertNull(staticVectorLayer(0..14) { null }.tile(key), "no bytes, no tile")
    }
}
