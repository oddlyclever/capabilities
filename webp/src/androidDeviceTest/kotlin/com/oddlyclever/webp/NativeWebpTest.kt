package com.oddlyclever.webp

import android.graphics.Bitmap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pins that the library loads the libwebp it was built from, and that a Bitmap encodes through it. */
class NativeWebpTest {

    @Test
    fun theBundledLibwebpIsTheVendoredRelease() {
        // native/PROVENANCE: v1.6.0.
        assertEquals("1.6.0", NativeWebp.encoderVersion().let { "${it shr 16}.${(it shr 8) and 0xff}.${it and 0xff}" })
    }

    @Test
    fun aBitmapEncodes() {
        val bitmap = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0x80336699.toInt()) }
        val webp = bitmap.encodeToWebp(metadata = WebpMetadata(software = "Acme"))
        assertTrue(webp.isWebp(), "RIFF/WEBP header")
        assertEquals(listOf("VP8X", "ALPH", "VP8 ", "EXIF"), webp.chunkIds(), "chunks")
    }
}
