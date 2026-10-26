package com.oddlyclever.wayfinder.vector

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.coderwise.libs.mapview.TileKey
import com.oddlyclever.wayfinder.MapFrameTile
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins that a vector frame layer draws a tile's names when it is given a text measurer, through map-view's own
 * decoding and layout, and draws none without one. JVM only: it needs a font family resolver without a context.
 */
class VectorFrameNamesTest {

    private val key = TileKey(10, 0, 0)
    private val density = Density(1f)
    private val tile = placeTile(name = "Acme", placeClass = "city")

    @Test
    fun aLayerGivenAMeasurerDrawsTheTilesNames() = runTest {
        val measurer = TextMeasurer(createFontFamilyResolver(), density, LayoutDirection.Ltr)
        val plain = pixels(vectorFrameLayer(0..14) { tile }.tile(key)!!)
        val named = pixels(vectorFrameLayer(0..14, names = measurer) { tile }.tile(key)!!)

        val changed = plain.indices.count { plain[it] != named[it] }
        assertTrue(changed > 20, "a named city should change the pixels where its name is drawn; $changed changed")
    }

    @Test
    fun aLayerGivenNoMeasurerDrawsNoNames() = runTest {
        val pixels = pixels(vectorFrameLayer(0..14) { tile }.tile(key)!!)
        assertEquals(1, pixels.toSet().size, "a tile of one named point and no ground should be one colour, not ${pixels.toSet().size}")
    }

    /** Draws both of [frameTile]'s passes over the whole tile, as a map frame does, and reads back its pixels. */
    private fun pixels(frameTile: MapFrameTile): IntArray {
        val side = 256
        val image = ImageBitmap(side, side)
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(side.toFloat(), side.toFloat())) {
            with(frameTile) {
                draw(WHOLE, key.z)
                drawLabels(WHOLE, key.z, bearing = 0f)
            }
        }
        return IntArray(side * side).also { image.readPixels(it) }
    }

    private companion object {
        val WHOLE = Rect(0f, 0f, 1f, 1f)

        /**
         * A Mapbox vector tile holding one feature: a point in the middle of the `place` layer, with a `class` and a
         * `name`, which is all map-view needs to name a place. Encoded by hand, field by field, as the format's
         * protocol buffer lays it out.
         */
        fun placeTile(name: String, placeClass: String): ByteArray {
            val extent = 4096
            val feature = message {
                packed(2, listOf(0, 0, 1, 1))                                  // tags: class = values[0], name = values[1]
                varint(3, 1)                                                   // type: point
                packed(4, listOf(9, zigzag(extent / 2), zigzag(extent / 2)))  // geometry: move to the middle
            }
            val layer = message {
                varint(15, 2)                                                  // version
                string(1, "place")
                bytes(2, feature)
                string(3, "class")
                string(3, "name")
                bytes(4, message { string(1, placeClass) })
                bytes(4, message { string(1, name) })
                varint(5, extent)
            }
            return message { bytes(3, layer) }
        }

        fun zigzag(n: Int) = (n shl 1) xor (n shr 31)

        fun message(build: Protobuf.() -> Unit): ByteArray = Protobuf().apply(build).out.toByteArray()

        class Protobuf {
            val out = ByteArrayOutputStream()

            fun raw(value: Long) {
                var v = value
                while (v >= 0x80) {
                    out.write(((v and 0x7F) or 0x80).toInt())
                    v = v ushr 7
                }
                out.write(v.toInt())
            }

            fun varint(field: Int, value: Int) {
                raw((field shl 3).toLong())
                raw(value.toLong())
            }

            fun bytes(field: Int, value: ByteArray) {
                raw(((field shl 3) or 2).toLong())
                raw(value.size.toLong())
                out.write(value)
            }

            fun string(field: Int, value: String) = bytes(field, value.encodeToByteArray())

            fun packed(field: Int, values: List<Int>) = bytes(field, message { values.forEach { raw(it.toLong()) } })
        }
    }
}
