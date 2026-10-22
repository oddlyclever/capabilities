package com.oddlyclever.kartog.staticmap

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.LayoutDirection
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
import com.oddlyclever.media.webp.WebpMetadata
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Pins kartog-static: that a rendered map is covered by its layers' tiles, falls back to ancestors as a live map does
 * and to the background where there are none; that a prepared map scales as a whole, overlay included; and that the
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
    fun aPreparedMapScalesAsAWhole() = runTest {
        awaitSkia()
        val map = prepareMap(size, density, toronto, listOf(solid(0..19, Color.Red)), background = Color.Blue)
        // Drawn at twice its size, through the painter, with a dot at the projected centre.
        val image = ImageBitmap(size.width * 2, size.height * 2)
        val painter = StaticMapPainter(map) { projection -> drawCircle(Color.Green, radius = 3f, center = projection.project(toronto.center)) }
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(image.width.toFloat(), image.height.toFloat())) {
            with(painter) { draw(this@draw.size) }
        }
        assertEquals(Color.Red.toArgb(), image.pixel(10, 10), "tiles fill the scaled image")
        assertEquals(Color.Green.toArgb(), image.pixel(200, 120), "the overlay scales with the map: centre at (200, 120)")
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
    fun aTranslucentImageKeepsItsColourInWebp() = runTest {
        awaitSkia()
        // Half-transparent red: straight alpha keeps it red; premultiplied pixels handed over as straight would darken it.
        val image = ImageBitmap(32, 32)
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(32f, 32f)) {
            drawRect(Color.Red.copy(alpha = 0.5f))
        }
        val decoded = image.encodeToWebp().decodeToImageBitmap().pixel(16, 16)
        val alpha = decoded ushr 24
        val red = (decoded shr 16) and 0xff
        assertTrue(abs(alpha - 128) <= 2, "alpha $alpha")
        assertTrue(red >= 240, "red $red: the colour was darkened")
    }

    @Test
    fun webpCarriesMetadata() = runTest {
        awaitSkia()
        val webp = renderMap(size, density, toronto, listOf(solid(0..19, Color.Red))).encodeToWebp(metadata = WebpMetadata(software = "Acme"))
        assertTrue("EXIF" in webp.decodeToString(), "an EXIF chunk")
        assertTrue("Acme" in webp.decodeToString(), "the software tag")
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
