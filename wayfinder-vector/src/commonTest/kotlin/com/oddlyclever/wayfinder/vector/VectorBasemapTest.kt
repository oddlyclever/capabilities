package com.oddlyclever.wayfinder.vector

import com.oddlyclever.wayfinder.TileClient
import com.oddlyclever.wayfinder.TileSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Pins that [vectorBasemap] layers made from equal arguments are equal, since the map keys a layer's tiles on the layer
 * and a layer made again in a recomposition must keep them; and that a change of argument is a different layer.
 */
class VectorBasemapTest {

    private val source = TileSource("streets", "https://tiles.example.com/{z}/{x}/{y}.pbf")
    private val client = TileClient { _, _ -> null }

    @Test
    fun equalArgumentsMakeEqualLayers() {
        val first = vectorBasemap(source, client, style = MapStyles.Dark)
        val again = vectorBasemap(TileSource("streets", "https://tiles.example.com/{z}/{x}/{y}.pbf"), client, style = MapStyles.Dark)
        assertEquals(first, again, "layers from equal arguments were $first and $again")
        assertEquals(first.hashCode(), again.hashCode(), "equal layers must hash alike")
    }

    @Test
    fun aDifferentArgumentIsADifferentLayer() {
        val layer = vectorBasemap(source, client)
        assertNotEquals(layer, vectorBasemap(source, TileClient { _, _ -> null }), "another client")
        assertNotEquals(layer, vectorBasemap(source, client, style = MapStyles.Dark), "another style")
        assertNotEquals(layer, vectorBasemap(source, client, labels = false), "labels off")
        assertNotEquals(layer, vectorBasemap(source, client, rasterize = true), "rasterized")
    }
}
