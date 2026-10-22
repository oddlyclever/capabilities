package com.oddlyclever.media.image

import com.oddlyclever.media.webp.isWebp
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The contract every target keeps: the format reported is the format produced, images only shrink and keep their
 * shape, JPEG is refused for an image with transparency and allowed for an opaque one, and each failure has its own
 * type, which the fallback acts on.
 */
class ImageProcessorTest {

    private val processor = ImageProcessor()

    private fun Result<ProcessedImage>.orFail(what: String): ProcessedImage =
        getOrElse { fail("$what failed: $it") }

    @Test
    fun reportsTheFormatItProduced() = runTest {
        awaitSkia()
        val source = writeTestImage(64, 32)
        for (format in ImageFormat.entries) {
            val image = processor.compressImage(source, format).orFail("$format")
            assertEquals(format.contentType, image.contentType, "$format content type")
            val looksRight = when (format) {
                ImageFormat.Webp -> image.bytes.isWebp()
                ImageFormat.Jpeg -> image.bytes[0] == 0xFF.toByte() && image.bytes[1] == 0xD8.toByte()
                ImageFormat.Png -> image.bytes.copyOfRange(1, 4).decodeToString() == "PNG"
            }
            assertTrue(looksRight, "$format bytes start ${image.bytes.take(4)}")
        }
    }

    @Test
    fun downsizesToMaxDimensionKeepingItsShape() = runTest {
        awaitSkia()
        val image = processor.compressImage(writeTestImage(400, 200), maxDimension = 100).orFail("downsizing")
        assertEquals(100 to 50, image.width to image.height, "size")
    }

    @Test
    fun neverUpscales() = runTest {
        awaitSkia()
        val image = processor.compressImage(writeTestImage(64, 32), maxDimension = 2048).orFail("a small image")
        assertEquals(64 to 32, image.width to image.height, "size")
    }

    @Test
    fun refusesJpegForAnImageWithTransparency() = runTest {
        val result = processor.compressImage(writeTestImage(32, 32, alpha = 128), ImageFormat.Jpeg)
        assertIs<ImageProcessingException.AlphaNotRepresentable>(result.exceptionOrNull(), "result $result")
    }

    @Test
    fun allowsJpegForAnOpaqueImageWithAnAlphaChannel() = runTest {
        processor.compressImage(writeTestImage(32, 32, alpha = 255), ImageFormat.Jpeg).orFail("opaque JPEG")
    }

    @Test
    fun aMissingSourceIsUnreadable() = runTest {
        val result = processor.compressImage(missingSource())
        assertIs<ImageProcessingException.SourceUnreadable>(result.exceptionOrNull(), "result $result")
    }

    @Test
    fun somethingThatIsNotAnImageFailsToDecode() = runTest {
        val failure = processor.compressImage(notAnImage()).exceptionOrNull()
        if (tellsUnreadableFromUndecodable) {
            assertIs<ImageProcessingException.DecodeFailed>(failure, "failure $failure")
        } else {
            assertTrue(failure is ImageProcessingException.SourceUnreadable || failure is ImageProcessingException.DecodeFailed, "failure $failure")
        }
    }

    @Test
    fun theFallbackSkipsJpegForTransparencyAndLandsOnPng() = runTest {
        val image = processor.compressImageWithFallback(writeTestImage(32, 32, alpha = 128), listOf(ImageFormat.Jpeg, ImageFormat.Png))
            .orFail("fallback")
        assertEquals(ImageFormat.Png.contentType, image.contentType, "content type")
    }

    @Test
    fun theFallbackTakesTheFirstThatSucceeds() = runTest {
        val image = processor.compressImageWithFallback(writeTestImage(32, 32), listOf(ImageFormat.Jpeg, ImageFormat.Png))
            .orFail("fallback")
        assertEquals(ImageFormat.Jpeg.contentType, image.contentType, "content type")
    }

    @Test
    fun theFallbackStopsOnAnUnreadableSource() = runTest {
        val result = processor.compressImageWithFallback(missingSource())
        assertIs<ImageProcessingException.SourceUnreadable>(result.exceptionOrNull(), "result $result")
    }

    @Test
    fun webpIsSmallerThanPng() = runTest {
        awaitSkia()
        val source = writeTestImage(256, 192)
        val webp = processor.compressImage(source, ImageFormat.Webp).orFail("WebP").bytes.size
        val png = processor.compressImage(source, ImageFormat.Png).orFail("PNG").bytes.size
        assertTrue(webp < png, "WebP $webp bytes, PNG $png bytes")
    }
}
