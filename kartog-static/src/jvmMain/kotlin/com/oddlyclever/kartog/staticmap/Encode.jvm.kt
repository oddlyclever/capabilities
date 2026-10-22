package com.oddlyclever.kartog.staticmap

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

// The same Skia encoder on the JVM, iOS and the web; each target needs its own copy of these lines because Skia is
// not visible from common code.
actual fun ImageBitmap.encodeToPng(): ByteArray = encode(EncodedImageFormat.PNG, 100)

private fun ImageBitmap.encode(format: EncodedImageFormat, quality: Int): ByteArray =
    checkNotNull(Image.makeFromBitmap(asSkiaBitmap()).encodeToData(format, quality)) {
        "Skia could not encode the image as $format."
    }.bytes
