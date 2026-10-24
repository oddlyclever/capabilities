package com.oddlyclever.wayfinder.vector

import androidx.compose.ui.graphics.toArgb
import com.coderwise.libs.mapview.tiles.vector.VectorStyle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Pins wayfinder-vector: that a palette recolours map-view's style without touching a width, a zoom gate or what goes
 * undrawn, that each road tier and line takes its own colour, and that a vector tile drawn to an image is the size a
 * tile is shown at and painted in the style's ground colour.
 */
class VectorTest {

    private val base = VectorStyle()
    private val dark = MapStyles.Dark
    private val palette = MapPalette.Dark

    @Test
    fun aRoadTakesItsTiersColoursAndKeepsItsWidths() {
        val before = assertNotNull(base.road("motorway"), "map-view paints motorways")
        val after = assertNotNull(dark.road("motorway"), "the dark style must too")
        assertEquals(palette.motorway.core, after.stroke, "motorway core")
        assertEquals(palette.motorway.casing, after.casing, "motorway casing")
        assertEquals(before.strokeWidth, after.strokeWidth, "width ramp must be map-view's")
        assertEquals(before.casingWidth, after.casingWidth, "casing ramp must be map-view's")
        assertEquals(before.casingMinZoom, after.casingMinZoom, "casing gate must be map-view's")
    }

    @Test
    fun aFootpathIsALineNotARoad() {
        val path = assertNotNull(dark.road("footway"), "footways are drawn")
        assertEquals(palette.path, path.stroke, "footway colour")
        assertNull(path.casing, "a footway has no casing, was ${path.casing}")
    }

    @Test
    fun whatMapViewLeavesUnpaintedStaysUnpainted() {
        val water = assertNotNull(dark.water, "water is drawn")
        assertEquals(palette.water, water.fill, "water fill")
        assertNull(water.stroke, "water has no outline in map-view, so none here")
        assertEquals(base.boundary("8"), dark.boundary("8"), "a local border map-view does not draw stays undrawn")
    }

    @Test
    fun groundIsGreenOrBareByWhatItIs() {
        assertEquals(palette.greenery, dark.landcover("wood")?.fill, "wood")
        assertEquals(palette.bareGround, dark.landcover("sand")?.fill, "sand")
        assertEquals(palette.builtUp, dark.landuse("residential")?.fill, "residential")
    }

    @Test
    fun theGroundAndLabelsTakeThePalette() {
        assertEquals(palette.land, dark.background, "background")
        assertEquals(palette.ink, dark.ink, "ink")
        assertEquals(palette.halo, dark.halo, "halo")
    }

    @Test
    fun theBuiltStylesAreBuiltOnce() {
        assertSame(MapStyles.Dark, MapStyles.Dark, "a style rebuilt on each read would never compare equal")
        assertEquals(VectorStyle(), MapStyles.Light, "the light style is map-view's own default")
    }

    @Test
    fun aRasterizedTileIsATileSizedImageOfTheGround() = runTest {
        awaitSkia()
        // An empty tile: no layers, so nothing but the style's ground.
        val image = assertNotNull(rasterizeVectorTile(ByteArray(0), zoom = 12, density = 2f, style = dark), "an empty tile still draws")
        assertEquals(512, image.width, "256 dp at density 2, was ${image.width}")
        assertEquals(512, image.height, "square, was ${image.height}")
        val pixels = IntArray(1)
        image.readPixels(pixels, startX = 256, startY = 256, width = 1, height = 1)
        assertEquals(palette.land.toArgb(), pixels[0], "the middle should be the dark land colour")
    }
}
