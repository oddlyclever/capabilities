package com.oddlyclever.kartog

import com.coderwise.libs.mapview.TileKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * Pins how a [TileSource] turns a map-view tile key into a URL: placeholders filled, rows flipped for TMS sources
 * and only for them, and templates that would fetch the wrong tile refused when the source is made.
 */
class TileSourceTest {

    private val xyz = TileSource(id = "streets", urlTemplate = "https://tiles.example.com/{z}/{x}/{y}.pbf")

    @Test
    fun fillsEveryPlaceholder() {
        val url = xyz.url(TileKey(z = 14, x = 4823, y = 6160))
        assertEquals("https://tiles.example.com/14/4823/6160.pbf", url, "XYZ url was $url")
    }

    @Test
    fun flipsTheRowForTms() {
        val tms = TileSource(id = "streets", urlTemplate = "https://tiles.example.com/{z}/{x}/{y}.png", tms = true)
        // At z2 there are four rows: row 0 from the top is row 3 from the bottom.
        val url = tms.url(TileKey(z = 2, x = 1, y = 0))
        assertEquals("https://tiles.example.com/2/1/3.png", url, "TMS url was $url")
    }

    @Test
    fun leavesTheColumnAndZoomAloneForTms() {
        val tms = TileSource(id = "streets", urlTemplate = "{z}-{x}-{y}", tms = true)
        val url = tms.url(TileKey(z = 0, x = 0, y = 0))
        assertEquals("0-0-0", url, "a single-tile world flips onto itself, but was $url")
    }

    @Test
    fun refusesATemplateMissingAPlaceholder() {
        val error = assertFailsWith<IllegalArgumentException> {
            TileSource(id = "streets", urlTemplate = "https://tiles.example.com/{z}/{x}.png")
        }
        assertEquals(true, error.message?.contains("{y}"), "message should name the missing {y}: ${error.message}")
    }

    @Test
    fun refusesABlankId() {
        assertFailsWith<IllegalArgumentException> { TileSource(id = " ", urlTemplate = "{z}/{x}/{y}") }
    }

    @Test
    fun refusesAnEmptyOrNegativeZoomRange() {
        assertFailsWith<IllegalArgumentException> { TileSource(id = "a", urlTemplate = "{z}/{x}/{y}", zoomRange = 5..4) }
        assertFailsWith<IllegalArgumentException> { TileSource(id = "a", urlTemplate = "{z}/{x}/{y}", zoomRange = -1..4) }
    }

    @Test
    fun isEqualWhenItWouldFetchTheSameTiles() {
        val same = TileSource(id = "streets", urlTemplate = "https://tiles.example.com/{z}/{x}/{y}.pbf")
        assertEquals(xyz, same, "identical sources should be equal")
        assertEquals(xyz.hashCode(), same.hashCode(), "equal sources should hash alike")
        val flipped = TileSource(id = "streets", urlTemplate = "https://tiles.example.com/{z}/{x}/{y}.pbf", tms = true)
        assertNotEquals(xyz, flipped, "a TMS source fetches different tiles, so it must not be equal")
    }
}
