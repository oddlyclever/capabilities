package com.oddlyclever.webp

import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Pins the Kotlin lossless encoder: every pixel comes back exactly from a real decoder, whatever the image's colour
 * spread, including one colour, two, and a spread skewed enough that Huffman codes must be shortened to fit. Decoded
 * by Skia on the JVM and the web; on iOS only the encoding is exercised.
 */
class LosslessTest {

    private fun image(width: Int, height: Int, pixel: (x: Int, y: Int) -> Int): RgbaPixels {
        val bytes = ByteArray(width * height * 4)
        for (y in 0 until height) for (x in 0 until width) {
            val argb = pixel(x, y)
            val i = (y * width + x) * 4
            bytes[i] = (argb shr 16).toByte(); bytes[i + 1] = (argb shr 8).toByte()
            bytes[i + 2] = argb.toByte(); bytes[i + 3] = (argb ushr 24).toByte()
        }
        return RgbaPixels(width, height, bytes)
    }

    private suspend fun assertRoundTrip(name: String, pixels: RgbaPixels) {
        awaitSkia()
        val webp = Vp8lEncoder.encode(pixels)
        assertTrue(webp.isWebp(), "$name: RIFF/WEBP header")
        assertEquals(listOf("VP8L"), webp.chunkIds(), "$name: chunks")
        val decoded = decodeForTest(webp) ?: return
        assertEquals(pixels.width to pixels.height, decoded.width to decoded.height, "$name: size")
        for (i in 0 until pixels.width * pixels.height * 4) {
            if (pixels.bytes[i] != decoded.bytes[i]) {
                fail("$name: byte $i (pixel ${i / 4}) is ${decoded.bytes[i].toInt() and 0xff}, expected ${pixels.bytes[i].toInt() and 0xff}")
            }
        }
    }

    @Test
    fun noise() = runTest {
        val random = Random(7)
        assertRoundTrip("noise", image(61, 37) { _, _ -> random.nextInt() or (0xff shl 24) })
    }

    @Test
    fun gradient() = runTest {
        assertRoundTrip("gradient", image(200, 120) { x, y -> (0xff shl 24) or (x shl 16) or ((y * 2) shl 8) or ((x + y) and 0xff) })
    }

    @Test
    fun oneColour() = runTest {
        assertRoundTrip("one colour", image(17, 9) { _, _ -> 0xff336699.toInt() })
    }

    @Test
    fun twoColours() = runTest {
        assertRoundTrip("two colours", image(16, 16) { x, y -> if ((x + y) % 2 == 0) 0xff000000.toInt() else 0xffffffff.toInt() })
    }

    @Test
    fun aSkewedSpreadNeedsShortenedCodes() = runTest {
        // Fibonacci counts make the deepest Huffman tree there is: 22 symbols would need codes 21 bits long.
        val fib = generateSequence(1 to 1) { (a, b) -> b to a + b }.map { it.first }.take(22).toList()
        val values = fib.flatMapIndexed { symbol, count -> List(count) { symbol * 11 } }
        val width = 256
        val height = (values.size + width - 1) / width
        assertRoundTrip("skewed", image(width, height) { x, y ->
            val v = values.getOrElse(y * width + x) { 0 }
            (0xff shl 24) or (v shl 16) or (v shl 8) or v
        })
    }

    @Test
    fun opaqueAndBinaryAlphaComeBackExactly() = runTest {
        // Only fully transparent black and opaque pixels: a decoder may premultiply on the way, which changes the
        // colour of any other pixel that is not opaque.
        assertRoundTrip("alpha", image(40, 40) { x, y -> if (x < 20 && y % 2 == 0) 0 else (0xff shl 24) or (x * 6 shl 16) or (y * 6) })
    }
}
