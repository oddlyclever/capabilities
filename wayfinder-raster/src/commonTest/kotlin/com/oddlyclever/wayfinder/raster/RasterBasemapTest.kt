package com.oddlyclever.wayfinder.raster

import com.oddlyclever.wayfinder.TileClient
import com.oddlyclever.wayfinder.TileSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Pins that [rasterBasemap] layers made from equal arguments are equal, since the map keys a layer's tiles on the layer
 * and a layer made again in a recomposition must keep them; and that a change of argument is a different layer.
 */
class RasterBasemapTest {

    private val source = TileSource("imagery", "https://tiles.example.com/{z}/{x}/{y}.png")
    private val client = TileClient { _, _ -> null }

    @Test
    fun equalArgumentsMakeEqualLayers() {
        val first = rasterBasemap(source, client, alpha = 0.5f)
        val again = rasterBasemap(TileSource("imagery", "https://tiles.example.com/{z}/{x}/{y}.png"), client, alpha = 0.5f)
        assertEquals(first, again, "layers from equal arguments were $first and $again")
        assertEquals(first.hashCode(), again.hashCode(), "equal layers must hash alike")
    }

    @Test
    fun aDifferentArgumentIsADifferentLayer() {
        val layer = rasterBasemap(source, client)
        val other = TileSource("shade", "https://tiles.example.com/shade/{z}/{x}/{y}.png")
        assertNotEquals(layer, rasterBasemap(other, client), "another source")
        assertNotEquals(layer, rasterBasemap(source, TileClient { _, _ -> null }), "another client")
        assertNotEquals(layer, rasterBasemap(source, client, alpha = 0.5f), "another alpha")
    }
}
