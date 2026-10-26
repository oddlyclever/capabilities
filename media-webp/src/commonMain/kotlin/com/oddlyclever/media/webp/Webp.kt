package com.oddlyclever.media.webp

/** How a WebP is compressed. */
enum class WebpCompression {
    /** Smaller, and loses detail as quality falls; for photos and maps. */
    Lossy,

    /** Every pixel kept exactly; larger, for screenshots, line art and anything re-encoded later. */
    Lossless,
}

/** The quality [encodeToWebp] uses unless told otherwise: small files with no visible loss in photos or maps. */
const val DEFAULT_WEBP_QUALITY: Int = 80

/** An image libwebp could not encode, or bytes that are not a WebP. */
class WebpException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * These pixels as a WebP file.
 *
 * Encoding is done by libwebp built into this library on Android and iOS, and by Skia's libwebp on the JVM and the
 * web, so the output does not depend on the version of Android or the device maker's build. A failure throws; it
 * never returns an empty or truncated file, which is what Android's own encoder has been seen to do.
 *
 * It is CPU-bound and can take tens of milliseconds for a photo, so call it off the main thread.
 *
 * @param quality 0 to 100 for [WebpCompression.Lossy], higher being larger and closer to the original. For
 *   [WebpCompression.Lossless] it has no effect on the pixels.
 * @param metadata written into the file as EXIF; nothing is written when it is null or empty.
 * @throws WebpException when the encoder refuses the image.
 * @throws IllegalArgumentException when [quality] is outside 0..100.
 */
fun RgbaPixels.encodeToWebp(
    quality: Int = DEFAULT_WEBP_QUALITY,
    compression: WebpCompression = WebpCompression.Lossy,
    metadata: WebpMetadata? = null,
): ByteArray {
    require(quality in 0..100) { "quality $quality is outside 0..100" }
    val encoded = encodeRgba(this, quality, compression == WebpCompression.Lossless)
        ?: throw WebpException("libwebp could not encode a $width × $height image")
    if (!encoded.isWebp()) throw WebpException("the encoder returned ${encoded.size} bytes that are not a WebP")
    return if (metadata == null || metadata.isEmpty) encoded else encoded.withWebpMetadata(metadata)
}

/**
 * This WebP with [metadata] written into it as EXIF, replacing any EXIF it had. The image data is not touched, so
 * this adds metadata to a WebP from any encoder, such as one Skia or a server produced.
 *
 * An empty [metadata] removes the EXIF.
 *
 * @throws WebpException when these bytes are not a WebP this can read.
 */
fun ByteArray.withWebpMetadata(metadata: WebpMetadata): ByteArray =
    WebpContainer.withExif(this, if (metadata.isEmpty) null else Exif.write(metadata))

/** Whether these bytes start the way every WebP file does. It reads the header only. */
fun ByteArray.isWebp(): Boolean =
    size >= 16 && this[0] == 'R'.code.toByte() && this[1] == 'I'.code.toByte() && this[2] == 'F'.code.toByte() &&
        this[3] == 'F'.code.toByte() && this[8] == 'W'.code.toByte() && this[9] == 'E'.code.toByte() &&
        this[10] == 'B'.code.toByte() && this[11] == 'P'.code.toByte()

/** The platform's encoder: [pixels] as a WebP with no metadata, or null when it refuses them. */
internal expect fun encodeRgba(pixels: RgbaPixels, quality: Int, lossless: Boolean): ByteArray?
