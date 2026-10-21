package com.oddlyclever.webp

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Pins what each target's encoder produces: a WebP of the right size and kind, lossy or lossless, that quality
 * changes, and that metadata arrives in an encoded file. Runs on the JVM, iOS and the web; the Android encoder loads
 * only on a device.
 */
class EncodeTest {

    /** A gradient with a translucent band, so both the colour and the alpha have to be encoded. */
    private fun pixels(width: Int = 96, height: Int = 64): RgbaPixels {
        val bytes = ByteArray(width * height * 4)
        for (y in 0 until height) for (x in 0 until width) {
            val i = (y * width + x) * 4
            bytes[i] = (x * 255 / width).toByte()
            bytes[i + 1] = (y * 255 / height).toByte()
            bytes[i + 2] = ((x + y) % 256).toByte()
            bytes[i + 3] = (if (y < height / 4) 128 else 255).toByte()
        }
        return RgbaPixels(width, height, bytes)
    }

    @Test
    fun lossyIsAWebpOfLossyData() = runTest {
        awaitSkia()
        val webp = pixels().encodeToWebp()
        assertTrue(webp.isWebp(), "RIFF/WEBP header")
        assertTrue("VP8 " in webp.chunkIds(), "lossy data in ${webp.chunkIds()}")
        assertTrue("ALPH" in webp.chunkIds(), "alpha in ${webp.chunkIds()}")
    }

    @Test
    fun losslessIsAWebpOfLosslessData() = runTest {
        awaitSkia()
        val webp = pixels(40, 30).encodeToWebp(compression = WebpCompression.Lossless)
        val chunks = webp.chunkIds()
        assertTrue("VP8L" in chunks, "lossless data in $chunks, ${webp.size} bytes")
        val bits = ExifReader.u32(webp.chunk("VP8L")!!, 1)
        assertEquals(40, (bits and 0x3fff) + 1, "width")
        assertEquals(30, ((bits shr 14) and 0x3fff) + 1, "height")
    }

    @Test
    fun lowerQualityIsSmaller() = runTest {
        awaitSkia()
        val low = pixels(256, 256).encodeToWebp(quality = 10).size
        val high = pixels(256, 256).encodeToWebp(quality = 95).size
        assertTrue(low < high, "quality 10 is $low bytes, quality 95 is $high")
    }

    @Test
    fun metadataArrivesInTheEncodedFile() = runTest {
        awaitSkia()
        val webp = pixels().encodeToWebp(metadata = WebpMetadata(location = GpsLocation(51.5, -0.12), software = "Acme"))
        assertEquals("VP8X", webp.chunkIds().first(), "extended header")
        assertEquals("EXIF", webp.chunkIds().last(), "EXIF last in ${webp.chunkIds()}")
        val exif = ExifReader(WebpContainer.exifOf(webp)!!)
        assertEquals("Acme", ExifReader.ascii(exif.primary.getValue(0x0131)), "software")
        assertEquals("N", ExifReader.ascii(exif.gps.getValue(1)), "latitude ref")
    }

    @Test
    fun aRowStrideWiderThanTheImageIsHonoured() = runTest {
        awaitSkia()
        val width = 10
        val height = 6
        val stride = width * 4 + 8
        val webp = RgbaPixels(width, height, ByteArray(stride * height) { 0x7f }, stride).encodeToWebp(compression = WebpCompression.Lossless)
        assertTrue(webp.isWebp(), "padded rows")
    }

    @Test
    fun badInputIsRefused() {
        assertFailsWith<IllegalArgumentException> { RgbaPixels(0, 10, ByteArray(0)) }
        assertFailsWith<IllegalArgumentException> { RgbaPixels(10, 10, ByteArray(10)) }
        assertFailsWith<IllegalArgumentException> { pixels().encodeToWebp(quality = 101) }
    }
}
