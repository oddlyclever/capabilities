package com.oddlyclever.media.image

import com.oddlyclever.media.webp.GpsLocation
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Instant

/**
 * Pins that a photo stored sideways comes out upright, turned the way its EXIF says, and that the source's location
 * and capture time reach the result only when asked for, in every format. The sources carry EXIF built by hand, in
 * both byte orders, as cameras write it.
 */
class UprightAndMetadataTest {

    private val processor = ImageProcessor()

    private fun Result<ProcessedImage>.orFail(what: String): ProcessedImage = getOrElse { fail("$what failed: $it") }

    private suspend fun source(orientation: Int? = null, latitude: Double? = null, longitude: Double? = null,
                               original: String? = null, offset: String? = null, bigEndian: Boolean = true): String {
        val tiff = testTiff(bigEndian, orientation, latitude, longitude, original, offset)
        return writeSource(jpegWithExif(twoToneImage(40, 20, jpeg = true), tiff), jpeg = true)
    }

    private fun isRed(argb: Int) = (argb shr 16 and 0xff) > 160 && (argb and 0xff) < 100
    private fun isBlue(argb: Int) = (argb and 0xff) > 160 && (argb shr 16 and 0xff) < 100

    @Test
    fun aPhotoTurnedAQuarterClockwiseComesOutUpright() = runTest {
        awaitSkia()
        val source = source(orientation = 6)
        for (format in ImageFormat.entries) {
            val image = processor.compressImage(source, format).orFail("$format")
            assertEquals(20 to 40, image.width to image.height, "$format size")
            // Turned clockwise to stand up, the stored image's left half, red, becomes the top.
            pixelAt(image.bytes, 10, 5)?.let { assertTrue(isRed(it), "$format top is ${it.toString(16)}") }
            pixelAt(image.bytes, 10, 35)?.let { assertTrue(isBlue(it), "$format bottom is ${it.toString(16)}") }
        }
    }

    @Test
    fun aPhotoTurnedTheOtherWayComesOutUpright() = runTest {
        awaitSkia()
        val image = processor.compressImage(source(orientation = 8, bigEndian = false), ImageFormat.Png).orFail("PNG")
        assertEquals(20 to 40, image.width to image.height, "size")
        // Turned anticlockwise, the left half becomes the bottom.
        pixelAt(image.bytes, 10, 35)?.let { assertTrue(isRed(it), "bottom is ${it.toString(16)}") }
        pixelAt(image.bytes, 10, 5)?.let { assertTrue(isBlue(it), "top is ${it.toString(16)}") }
    }

    @Test
    fun anUpsideDownPhotoKeepsItsShape() = runTest {
        awaitSkia()
        val image = processor.compressImage(source(orientation = 3), ImageFormat.Png).orFail("PNG")
        assertEquals(40 to 20, image.width to image.height, "size")
        pixelAt(image.bytes, 35, 10)?.let { assertTrue(isRed(it), "right is ${it.toString(16)}") }
    }

    @Test
    fun locationAndCaptureTimeAreKeptWhenAskedInEveryFormat() = runTest {
        awaitSkia()
        val source = source(orientation = 1, latitude = 45.4215, longitude = -75.6972, original = "2026:03:09 10:05:07", offset = "-04:00")
        for (format in ImageFormat.entries) {
            val image = processor.compressImage(source, format, keepMetadata = true).orFail("$format")
            val kept = SourceExifReader.read(image.bytes)
            val location = kept.location ?: fail("$format kept no location")
            assertTrue(abs(location.latitude - 45.4215) < 1e-5 && abs(location.longitude + 75.6972) < 1e-5, "$format location $location")
            assertEquals(Instant.parse("2026-03-09T14:05:07Z"), kept.capturedAt, "$format capture time")
        }
    }

    @Test
    fun nothingIsKeptByDefault() = runTest {
        awaitSkia()
        val source = source(latitude = 45.4215, longitude = -75.6972, original = "2026:03:09 10:05:07", offset = "-04:00")
        for (format in ImageFormat.entries) {
            val image = processor.compressImage(source, format).orFail("$format")
            assertNull(SourceExifReader.read(image.bytes).metadata, "$format carried metadata")
        }
    }

    @Test
    fun aCaptureTimeWithNoOffsetIsLeftOut() = runTest {
        awaitSkia()
        val source = source(latitude = 51.5, longitude = -0.12, original = "2026:03:09 10:05:07")
        val kept = SourceExifReader.read(processor.compressImage(source, keepMetadata = true).orFail("WebP").bytes)
        assertEquals(51.5, kept.location?.latitude?.let { (it * 1e5).toLong() / 1e5 }, "latitude")
        assertNull(kept.capturedAt, "a local time with no offset names no moment")
    }

    @Test
    fun theReaderReadsBothByteOrdersAndEveryContainer() {
        for (bigEndian in listOf(true, false)) {
            val tiff = testTiff(bigEndian, orientation = 6, latitude = -33.8688, longitude = 151.2093, original = "2026:01:02 03:04:05", offset = "+11:00")
            val read = SourceExifReader.read(jpegWithExif(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()), tiff))
            assertEquals(6, read.orientation, "orientation, big-endian $bigEndian")
            assertTrue(read.location?.let { abs(it.latitude + 33.8688) < 1e-5 && abs(it.longitude - 151.2093) < 1e-5 } == true, "location ${read.location}")
            assertEquals(Instant.parse("2026-01-01T16:04:05Z"), read.capturedAt, "capture time, big-endian $bigEndian")
        }
        assertEquals(1, SourceExifReader.read("not an image".encodeToByteArray()).orientation, "garbage reads upright")
        assertEquals(1, SourceExifReader.read(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte(), 0x7F, 0x7F)).orientation, "a cut-off segment reads upright")
    }

    @Test
    fun keptMetadataIsWhatTheSourceSaidNotAGuess() {
        val written = embedMetadata(
            twoToneBytesForPng(),
            ImageFormat.Png,
            com.oddlyclever.media.webp.WebpMetadata(location = GpsLocation(10.0, 20.0)),
        )
        val read = SourceExifReader.read(written)
        assertEquals(10.0, read.location?.latitude, "latitude through a PNG eXIf chunk")
        assertNull(read.capturedAt, "no capture time was given")
    }

    /** A minimal valid PNG header and an empty IEND, enough for an eXIf chunk to go after the header. */
    private fun twoToneBytesForPng(): ByteArray {
        val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val ihdr = byteArrayOf(0, 0, 0, 13) + "IHDR".encodeToByteArray() + ByteArray(13) + ByteArray(4)
        val iend = byteArrayOf(0, 0, 0, 0) + "IEND".encodeToByteArray() + ByteArray(4)
        return signature + ihdr + iend
    }
}
