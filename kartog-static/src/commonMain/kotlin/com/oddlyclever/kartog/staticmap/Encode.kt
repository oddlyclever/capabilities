package com.oddlyclever.kartog.staticmap

import androidx.compose.ui.graphics.ImageBitmap

/**
 * This image as PNG bytes, for a file, an upload or an email attachment. Lossless, so a map's thin lines and small
 * text stay crisp, at the cost of size.
 *
 * @throws IllegalStateException when the platform's encoder refuses the image.
 */
expect fun ImageBitmap.encodeToPng(): ByteArray

/**
 * This image as lossy WebP bytes at [quality], 0 to 100: a fraction of the PNG's size, for an image that is sent
 * rather than kept.
 *
 * @throws IllegalStateException when the platform's encoder refuses the image.
 */
expect fun ImageBitmap.encodeToWebp(quality: Int = DEFAULT_WEBP_QUALITY): ByteArray

/** High enough that roads and labels keep their edges; WebP's artefacts show first on thin lines. */
const val DEFAULT_WEBP_QUALITY = 90
