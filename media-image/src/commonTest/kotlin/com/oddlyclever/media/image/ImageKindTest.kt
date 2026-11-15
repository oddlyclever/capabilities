package com.oddlyclever.media.image

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

/**
 * Pins what [imageKindOf] recognises from bytes alone: each raster format by its signature, a GIF as animated only
 * when it holds a second frame, an SVG by its root element past whatever an XML file may put first, and nothing for
 * bytes that are none of these, including ones that merely look close.
 */
class ImageKindTest {

    @Test
    fun rasterFormatsAreKnownByTheirSignatures() = runTest {
        awaitSkia()
        assertEquals(ImageKind.Png, imageKindOf(twoToneImage(8, 8, jpeg = false)), "PNG")
        assertEquals(ImageKind.Jpeg, imageKindOf(twoToneImage(8, 8, jpeg = true)), "JPEG")
        val webp = ImageProcessor().compressImage(twoToneImage(8, 8, jpeg = false), ImageFormat.Webp).getOrElse { fail("WebP: $it") }
        assertEquals(ImageKind.Webp, imageKindOf(webp.bytes), "WebP")
    }

    @Test
    fun aGifIsAnimatedOnlyWithASecondFrame() {
        assertEquals(ImageKind.Gif, imageKindOf(gif(frames = 1)), "one frame")
        assertEquals(ImageKind.AnimatedGif, imageKindOf(gif(frames = 2)), "two frames")
        assertEquals(ImageKind.AnimatedGif, imageKindOf(gif(frames = 3, localColourTables = true)), "three frames with local colour tables")
        assertEquals(ImageKind.Gif, imageKindOf(gif(frames = 2).copyOf(60)), "a GIF cut off inside its first frame")
        assertEquals(ImageKind.Gif, imageKindOf("GIF87a".encodeToByteArray()), "a bare GIF87a header")
    }

    @Test
    fun anSvgIsKnownByItsRootElement() {
        val svgs = mapOf(
            "bare" to "<svg xmlns=\"http://www.w3.org/2000/svg\"/>",
            "after an XML declaration" to "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<svg width=\"10\">",
            "after a byte-order mark" to "﻿<svg>",
            "after whitespace and a comment" to "\n  <!-- Generator: a drawing app -->\n<svg\tviewBox=\"0 0 1 1\">",
            "after a doctype" to "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\"><svg>",
            "after a doctype with an internal subset" to "<!DOCTYPE svg [ <!ENTITY a \"<b>\"> ]>\n<svg>",
        )
        for ((what, text) in svgs) assertEquals(ImageKind.Svg, imageKindOf(text.encodeToByteArray()), what)
    }

    @Test
    fun otherBytesAreNoKind() {
        val others = mapOf(
            "text" to "not an image".encodeToByteArray(),
            "an empty array" to ByteArray(0),
            "HTML" to "<!DOCTYPE html><html><body><svg></svg></body></html>".encodeToByteArray(),
            "an element named like svg" to "<svgfoo/>".encodeToByteArray(),
            "an unclosed comment" to "<!-- <svg>".encodeToByteArray(),
            "a RIFF that is not WebP" to "RIFF\u0000\u0000\u0000\u0000WAVE".encodeToByteArray(),
            "half a PNG signature" to byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47),
        )
        for ((what, bytes) in others) assertNull(imageKindOf(bytes), what)
    }

    @Test
    fun eachKindNamesTheContentTypeToStoreItUnder() {
        assertEquals("image/svg+xml", ImageKind.Svg.contentType, "SVG")
        assertEquals("image/gif", ImageKind.AnimatedGif.contentType, "animated GIF")
    }

    /**
     * A 1 by 1 GIF89a of [frames] frames, each behind a graphic control extension, after a looping application
     * extension when there is more than one, with a global colour table, and a local one per frame when asked.
     */
    private fun gif(frames: Int, localColourTables: Boolean = false): ByteArray {
        val out = mutableListOf<Int>()
        out += "GIF89a".encodeToByteArray().map { it.toInt() }
        out += listOf(1, 0, 1, 0, 0x80, 0, 0) // 1x1, a global colour table of 2 entries
        out += listOf(0, 0, 0, 255, 255, 255)
        if (frames > 1) out += listOf(0x21, 0xFF, 11) + "NETSCAPE2.0".encodeToByteArray().map { it.toInt() } + listOf(3, 1, 0, 0, 0)
        repeat(frames) {
            out += listOf(0x21, 0xF9, 4, 0, 10, 0, 0, 0) // graphic control: a tenth of a second
            out += listOf(0x2C, 0, 0, 0, 0, 1, 0, 1, 0, if (localColourTables) 0x80 else 0)
            if (localColourTables) out += listOf(255, 0, 0, 0, 0, 255)
            out += listOf(2, 2, 0x44, 0x01, 0) // LZW minimum code size, one data sub-block, terminator
        }
        out += 0x3B
        return ByteArray(out.size) { out[it].toByte() }
    }
}
