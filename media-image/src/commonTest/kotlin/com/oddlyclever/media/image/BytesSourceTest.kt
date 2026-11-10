package com.oddlyclever.media.image

import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Instant

/**
 * Pins that an image handed over as bytes is treated exactly as the same image at a location: the same size and
 * format, stood upright, metadata only when asked, the same fallback, and failures that name `"<bytes>"` as their
 * source. Also pins that [ImageProcessor.readSource] returns a source's bytes unchanged.
 */
class BytesSourceTest {

    private val processor = ImageProcessor()

    private fun Result<ProcessedImage>.orFail(what: String): ProcessedImage = getOrElse { fail("$what failed: $it") }

    private fun isRed(argb: Int) = (argb shr 16 and 0xff) > 160 && (argb and 0xff) < 100
    private fun isBlue(argb: Int) = (argb and 0xff) > 160 && (argb shr 16 and 0xff) < 100

    @Test
    fun theSameImageFromItsFileAndFromItsBytesComesOutTheSame() = runTest {
        awaitSkia()
        for (jpeg in listOf(false, true)) {
            val bytes = twoToneImage(80, 40, jpeg)
            val source = writeSource(bytes, jpeg)
            for (format in ImageFormat.entries) {
                val fromFile = processor.compressImage(source, format, maxDimension = 60).orFail("$format from the file")
                val fromBytes = processor.compressImage(bytes, format, maxDimension = 60).orFail("$format from bytes")
                assertEquals(
                    Triple(fromFile.width, fromFile.height, fromFile.contentType),
                    Triple(fromBytes.width, fromBytes.height, fromBytes.contentType),
                    "$format from a ${if (jpeg) "JPEG" else "PNG"}",
                )
            }
        }
    }

    @Test
    fun aSidewaysPhotoGivenAsBytesComesOutUpright() = runTest {
        awaitSkia()
        val bytes = jpegWithExif(twoToneImage(40, 20, jpeg = true), testTiff(bigEndian = true, orientation = 6))
        for (format in ImageFormat.entries) {
            val image = processor.compressImage(bytes, format).orFail("$format")
            assertEquals(20 to 40, image.width to image.height, "$format size")
            pixelAt(image.bytes, 10, 5)?.let { assertTrue(isRed(it), "$format top is ${it.toString(16)}") }
            pixelAt(image.bytes, 10, 35)?.let { assertTrue(isBlue(it), "$format bottom is ${it.toString(16)}") }
        }
    }

    @Test
    fun metadataFromBytesIsKeptOnlyWhenAsked() = runTest {
        awaitSkia()
        val tiff = testTiff(bigEndian = false, latitude = 45.4215, longitude = -75.6972, original = "2026:03:09 10:05:07", offset = "-04:00")
        val bytes = jpegWithExif(twoToneImage(40, 20, jpeg = true), tiff)
        for (format in ImageFormat.entries) {
            val kept = SourceExifReader.read(processor.compressImage(bytes, format, keepMetadata = true).orFail("$format kept").bytes)
            val location = kept.location ?: fail("$format kept no location")
            assertTrue(abs(location.latitude - 45.4215) < 1e-5 && abs(location.longitude + 75.6972) < 1e-5, "$format location $location")
            assertEquals(Instant.parse("2026-03-09T14:05:07Z"), kept.capturedAt, "$format capture time")

            val plain = processor.compressImage(bytes, format).orFail("$format plain")
            assertNull(SourceExifReader.read(plain.bytes).metadata, "$format carried metadata by default")
        }
    }

    @Test
    fun bytesThatAreNotAnImageFailToDecodeNamingBytes() = runTest {
        for ((what, bytes) in listOf("text" to "not an image".encodeToByteArray(), "an empty array" to ByteArray(0))) {
            val failure = processor.compressImage(bytes).exceptionOrNull()
            assertIs<ImageProcessingException.DecodeFailed>(failure, "$what: $failure")
            assertEquals("<bytes>", failure.uri, "$what names its source")
        }
    }

    @Test
    fun theFallbackFromBytesSkipsJpegForTransparencyAndStopsOnGarbage() = runTest {
        val transparent = processor.readSource(writeTestImage(32, 32, alpha = 128)).getOrElse { fail("reading the test image: $it") }
        val image = processor.compressImageWithFallback(transparent, listOf(ImageFormat.Jpeg, ImageFormat.Png)).orFail("fallback")
        assertEquals(ImageFormat.Png.contentType, image.contentType, "content type")

        val garbage = processor.compressImageWithFallback("not an image".encodeToByteArray()).exceptionOrNull()
        assertIs<ImageProcessingException.DecodeFailed>(garbage, "garbage: $garbage")
    }

    @Test
    fun readSourceReturnsTheSourcesBytesUnchanged() = runTest {
        val bytes = twoToneImage(16, 8, jpeg = false)
        val read = processor.readSource(writeSource(bytes, jpeg = false)).getOrElse { fail("reading: $it") }
        assertContentEquals(bytes, read, "bytes read back (${read.size} of ${bytes.size})")
    }

    @Test
    fun readSourceOfAMissingSourceIsUnreadable() = runTest {
        val missing = missingSource()
        val failure = processor.readSource(missing).exceptionOrNull()
        assertIs<ImageProcessingException.SourceUnreadable>(failure, "failure $failure")
        assertEquals(missing, failure.uri, "names the location")
    }
}
