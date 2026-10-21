package com.oddlyclever.webp

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Pins how metadata is written: the EXIF tags and their values, and how the RIFF container is rewritten around them
 * for simple and extended files. Works on hand-built files, so it needs no encoder.
 */
class MetadataTest {

    private val metadata = WebpMetadata(
        location = GpsLocation(latitude = 45.4215, longitude = -75.6972, altitude = -12.5),
        capturedAt = Instant.parse("2026-03-09T14:05:07.250Z"),
        software = "Acme Maps",
    )

    @Test
    fun locationIsWrittenAsGpsTags() {
        val gps = ExifReader(Exif.write(metadata)).gps
        assertEquals("N", ExifReader.ascii(gps.getValue(1)), "latitude ref")
        assertEquals("W", ExifReader.ascii(gps.getValue(3)), "longitude ref")
        val latitude = ExifReader.degrees(gps.getValue(2))
        val longitude = ExifReader.degrees(gps.getValue(4))
        assertTrue(abs(latitude - 45.4215) < 1e-7, "latitude $latitude")
        assertTrue(abs(longitude - 75.6972) < 1e-7, "longitude $longitude")
        assertEquals(1, gps.getValue(5).value[0].toInt(), "altitude ref for below sea level")
        assertEquals(12.5, ExifReader.rationals(gps.getValue(6)).single(), "altitude")
        assertContentEquals(byteArrayOf(2, 3, 0, 0), gps.getValue(0).value, "GPS version")
    }

    @Test
    fun captureTimeIsWrittenInUtc() {
        val exif = ExifReader(Exif.write(metadata))
        assertEquals("2026:03:09 14:05:07", ExifReader.ascii(exif.exif.getValue(0x9003)), "original date and time")
        assertEquals("+00:00", ExifReader.ascii(exif.exif.getValue(0x9011)), "offset")
        assertEquals("250", ExifReader.ascii(exif.exif.getValue(0x9291)), "milliseconds")
        assertEquals("2026:03:09", ExifReader.ascii(exif.gps.getValue(0x1D)), "GPS date")
        assertEquals(listOf(14.0, 5.0, 7.25), ExifReader.rationals(exif.gps.getValue(7)), "GPS time")
        assertEquals("Acme Maps", ExifReader.ascii(exif.primary.getValue(0x0131)), "software")
    }

    @Test
    fun datesBeforeTheEpochAndOnLeapDaysAreRight() {
        fun original(instant: String) =
            ExifReader.ascii(ExifReader(Exif.write(WebpMetadata(capturedAt = Instant.parse(instant)))).exif.getValue(0x9003))
        assertEquals("1969:12:31 23:59:59", original("1969-12-31T23:59:59Z"))
        assertEquals("2024:02:29 00:00:00", original("2024-02-29T00:00:00Z"))
        assertEquals("2000:12:31 12:00:00", original("2000-12-31T12:00:00Z"))
    }

    @Test
    fun onlyWhatIsSetIsWritten() {
        val exif = ExifReader(Exif.write(WebpMetadata(software = "Acme")))
        assertEquals(setOf(0x0131), exif.primary.keys, "primary tags")
        assertTrue(exif.gps.isEmpty() && exif.exif.isEmpty(), "no sub-IFDs")
        val noTime = ExifReader(Exif.write(WebpMetadata(location = GpsLocation(0.0, 0.0))))
        assertTrue(7 !in noTime.gps && 0x1D !in noTime.gps && 6 !in noTime.gps, "GPS tags ${noTime.gps.keys}")
    }

    @Test
    fun aSimpleLosslessFileBecomesExtendedWithItsSizeAndAlpha() {
        val out = simpleLossless(width = 300, height = 200, alpha = true).withWebpMetadata(metadata)
        assertEquals(listOf("VP8X", "VP8L", "EXIF"), out.chunkIds(), "chunks")
        val vp8x = out.copyOfRange(20, 30)
        assertEquals(0x08 or 0x10, vp8x[0].toInt(), "flags: EXIF and alpha")
        assertEquals(299, ExifReader.u32(vp8x, 4) and 0xffffff, "canvas width - 1")
        assertEquals(199, ExifReader.u32(out, 27) and 0xffffff, "canvas height - 1")
        assertEquals(out.size - 8, ExifReader.u32(out, 4), "RIFF size")
        assertNotNullExif(out)
    }

    @Test
    fun aSimpleLossyFileTakesItsSizeFromTheFrameHeader() {
        val out = simpleLossy(width = 640, height = 480).withWebpMetadata(metadata)
        assertEquals(listOf("VP8X", "VP8 ", "EXIF"), out.chunkIds(), "chunks")
        assertEquals(0x08, out[20].toInt(), "flags: EXIF only")
        assertEquals(639, ExifReader.u32(out, 24) and 0xffffff, "canvas width - 1")
        assertEquals(479, ExifReader.u32(out, 27) and 0xffffff, "canvas height - 1")
    }

    @Test
    fun exifGoesBeforeXmpAndReplacesAnyThatWasThere() {
        val extended = riff(
            chunk("VP8X", ByteArray(10).also { it[0] = (0x08 or 0x04).toByte() }),
            chunk("VP8L", vp8l(10, 10, false)),
            chunk("EXIF", byteArrayOf(1, 2, 3)),
            chunk("XMP ", "<x/>".encodeToByteArray()),
        )
        val out = extended.withWebpMetadata(metadata)
        assertEquals(listOf("VP8X", "VP8L", "EXIF", "XMP "), out.chunkIds(), "chunks")
        assertEquals(0x08 or 0x04, out[20].toInt(), "flags keep XMP")
        assertEquals("Acme Maps", ExifReader.ascii(ExifReader(WebpContainer.exifOf(out)!!).primary.getValue(0x0131)))
    }

    @Test
    fun emptyMetadataRemovesExif() {
        val withExif = simpleLossless(4, 4, false).withWebpMetadata(metadata)
        val without = withExif.withWebpMetadata(WebpMetadata())
        assertNull(WebpContainer.exifOf(without), "EXIF left behind")
        assertEquals(0, without[20].toInt() and 0x08, "EXIF flag left set")
        val simple = simpleLossless(4, 4, false)
        assertContentEquals(simple, simple.withWebpMetadata(WebpMetadata()), "a simple file with nothing to add")
    }

    @Test
    fun anOddLengthChunkIsPadded() {
        val out = simpleLossless(4, 4, false).withWebpMetadata(WebpMetadata(software = "ab"))
        assertEquals(0, out.size % 2, "file length ${out.size}")
        assertEquals(listOf("VP8X", "VP8L", "EXIF"), out.chunkIds())
    }

    @Test
    fun bytesThatAreNotAWebpAreRefused() {
        assertFailsWith<WebpException> { "not a webp at all".encodeToByteArray().withWebpMetadata(metadata) }
        assertFailsWith<IllegalArgumentException> { GpsLocation(91.0, 0.0) }
    }

    private fun assertNotNullExif(webp: ByteArray) =
        assertTrue(WebpContainer.exifOf(webp)?.let { ExifReader(it).gps.isNotEmpty() } == true, "EXIF with GPS")

    private fun simpleLossless(width: Int, height: Int, alpha: Boolean) = riff(chunk("VP8L", vp8l(width, height, alpha)))

    private fun simpleLossy(width: Int, height: Int): ByteArray {
        val frame = byteArrayOf(0, 0, 0, 0x9d.toByte(), 0x01, 0x2a, width.toByte(), (width shr 8).toByte(), height.toByte(), (height shr 8).toByte(), 0)
        return riff(chunk("VP8 ", frame))
    }

    private fun vp8l(width: Int, height: Int, alpha: Boolean): ByteArray {
        val bits = (width - 1) or ((height - 1) shl 14) or ((if (alpha) 1 else 0) shl 28)
        return byteArrayOf(0x2f, bits.toByte(), (bits shr 8).toByte(), (bits shr 16).toByte(), (bits shr 24).toByte(), 0, 0)
    }

    private fun chunk(id: String, payload: ByteArray): ByteArray {
        val size = payload.size
        val header = id.encodeToByteArray() + byteArrayOf(size.toByte(), (size shr 8).toByte(), (size shr 16).toByte(), (size shr 24).toByte())
        return header + payload + if (size % 2 == 1) byteArrayOf(0) else byteArrayOf()
    }

    private fun riff(vararg chunks: ByteArray): ByteArray {
        val body = "WEBP".encodeToByteArray() + chunks.fold(byteArrayOf()) { a, b -> a + b }
        val size = body.size
        return "RIFF".encodeToByteArray() + byteArrayOf(size.toByte(), (size shr 8).toByte(), (size shr 16).toByte(), (size shr 24).toByte()) + body
    }
}
