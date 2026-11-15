package com.oddlyclever.media.image

/**
 * The formats [ImageProcessor] encodes.
 *
 * [contentType] is the MIME type to record with the result, and [fileExtension] the extension to save it under.
 * Store or send those, not the member's name.
 */
enum class ImageFormat(val contentType: String, val fileExtension: String) {
    Webp("image/webp", "webp"),
    Jpeg("image/jpeg", "jpg"),
    Png("image/png", "png");

    /** JPEG has no alpha channel, so it cannot represent transparency. */
    val supportsAlpha: Boolean get() = this != Jpeg
}

/**
 * A downsized, encoded image plus the metadata a caller needs to store it.
 *
 * [contentType] reports the format actually produced. Callers must not infer the format
 * from the requested one or by inspecting [bytes].
 */
data class ProcessedImage(
    val bytes: ByteArray,
    val contentType: String,
    val width: Int,
    val height: Int,
) {
    // ByteArray uses identity equals/hashCode, so a data class holding one needs these
    // written out to behave as callers expect.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessedImage) return false
        return contentType == other.contentType &&
            width == other.width &&
            height == other.height &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}

/**
 * Failures returned by [ImageProcessor.compressImage], wrapped in [Result.failure].
 *
 * The distinctions matter to callers: [Unsupported] and [AlphaNotRepresentable] mean
 * "never retry this format", while [EncodeFailed] may be worth retrying with another.
 */
sealed class ImageProcessingException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** This platform cannot encode [format] at all. Do not retry it for other images. */
    class Unsupported(
        val format: ImageFormat,
        val platform: String,
    ) : ImageProcessingException("$platform cannot encode ${format.contentType}")

    /**
     * The source could not be opened or read.
     *
     * [uri] is the location that was asked for, or `"<bytes>"` when the image was handed over as bytes.
     */
    class SourceUnreadable(
        val uri: String,
        cause: Throwable? = null,
    ) : ImageProcessingException("Could not read image source: $uri", cause)

    /**
     * The source was readable but could not be decoded as an image.
     *
     * [uri] is the location that was asked for, or `"<bytes>"` when the image was handed over as bytes, an empty
     * array included.
     */
    class DecodeFailed(
        val uri: String,
        cause: Throwable? = null,
    ) : ImageProcessingException("Could not decode image: $uri", cause)

    /**
     * The image has a non-opaque alpha channel and [format] cannot represent it.
     * Encoding anyway would silently flatten transparency, so the caller should choose
     * an alpha-capable format instead.
     */
    class AlphaNotRepresentable(
        val format: ImageFormat,
    ) : ImageProcessingException("${format.contentType} cannot represent transparency")

    /** Decoding succeeded but the encoder failed or produced no data. */
    class EncodeFailed(
        val format: ImageFormat,
        cause: Throwable? = null,
    ) : ImageProcessingException("Could not encode to ${format.contentType}", cause)
}

/**
 * Downsizes an image and encodes it, for an upload or a file: a photo from the camera or the gallery made small
 * enough to send, in the format asked for.
 *
 * WebP is encoded by media-webp on every target, with libwebp built in on Android and iOS, so its output does not
 * depend on the device. JPEG and PNG use each platform's own encoder. Images are only ever made smaller, never larger.
 *
 * A photo is stood upright: one whose EXIF says the camera was held sideways comes out the way it is shown, with its
 * width and height swapped where that turns it a quarter. The EXIF itself is not copied, so the result carries no
 * metadata unless asked for with `keepMetadata`.
 *
 * The image is given either as a location or as its bytes, for a caller that already holds them. Either way it is
 * treated alike. Nothing is refused for what kind of image it is: an SVG is rasterized on the web and fails to decode
 * elsewhere, and an animated GIF keeps only its first frame. Call [imageKindOf] first to keep those as they are.
 *
 * Construct it directly. On Android it reads the application context from `AppContext`, which the platform library
 * captures at start-up, so there is nothing to pass and no dependency-injection container is needed.
 */
expect class ImageProcessor() {

    /**
     * Loads the image at [uri], resizes it so its largest dimension is at most
     * [maxDimension], and encodes it as [format].
     *
     * Encodes exactly the requested format or fails — it never silently substitutes
     * another. Callers wanting a fallback chain should use [compressImageWithFallback],
     * or drive the retries themselves off the returned [ImageProcessingException].
     *
     * It converts whatever decodes: an SVG is rasterized on the web, where an `<img>` draws it, and fails with
     * [ImageProcessingException.DecodeFailed] on the other targets; an animated GIF keeps only its first frame on every
     * target. To keep either as it is, read the source with [readSource] and check it with [imageKindOf] first.
     *
     * @param uri Where the image is: on Android a `content://` or `file://` URI or a path, on iOS and the JVM a
     *   `file://` URI or a path, and on the web anything an `<img>` can load from this origin (`blob:`, `data:` or a
     *   same-origin URL).
     * @param format The encoding target.
     * @param maxDimension Maximum edge size in pixels.
     * @param quality Compression quality (0-100). Ignored for [ImageFormat.Png], which is lossless.
     * @param keepMetadata Whether to write the source's location and capture time into the result, as EXIF, in every
     *   format. Off by default: a photo's location is personal, and most images are uploaded to be seen by others.
     *   Only a capture time the source records exactly is kept, one with its offset from UTC or a GPS time stamp;
     *   nothing else of the source's EXIF is.
     */
    suspend fun compressImage(
        uri: String,
        format: ImageFormat = ImageFormat.Webp,
        maxDimension: Int = 2048,
        quality: Int = 80,
        keepMetadata: Boolean = false,
    ): Result<ProcessedImage>

    /**
     * Resizes and encodes the image in [bytes], exactly as the `uri` form does with the image at a location: the same
     * formats, sizing, upright orientation, metadata and failures. It is for an image the caller already holds, so it
     * need not be written to a file or, on the web, made into a `blob:` URL first.
     *
     * A failure names its source as `"<bytes>"`. Bytes that are not an image, an empty array among them, fail with
     * [ImageProcessingException.DecodeFailed].
     */
    suspend fun compressImage(
        bytes: ByteArray,
        format: ImageFormat = ImageFormat.Webp,
        maxDimension: Int = 2048,
        quality: Int = 80,
        keepMetadata: Boolean = false,
    ): Result<ProcessedImage>

    /**
     * The bytes at [uri], unchanged, from exactly the locations [compressImage] reads on this target, or
     * [ImageProcessingException.SourceUnreadable] when they cannot be read. It is the way to look at a source before
     * deciding whether to convert it, with [imageKindOf], and then hand the same bytes to [compressImage] without
     * reading it twice.
     *
     * The whole file is read into memory, which the `uri` form of [compressImage] avoids on Android and iOS for a large
     * photo; read a source only when the bytes themselves are wanted.
     */
    suspend fun readSource(uri: String): Result<ByteArray>
}

/** What a failure from the bytes form of [ImageProcessor.compressImage] names as its source. */
internal const val BytesSource = "<bytes>"

/**
 * Tries each format in [candidates] in order and returns the first success.
 *
 * Formats that fail with [ImageProcessingException.Unsupported] or
 * [ImageProcessingException.AlphaNotRepresentable] are skipped rather than retried, since
 * those failures are properties of the platform or the image rather than transient.
 *
 * The default order prefers WebP and keeps PNG last as the alpha-safe backstop. If every
 * candidate fails, the last failure is returned. [keepMetadata] is passed to each attempt; see [ImageProcessor.compressImage].
 */
suspend fun ImageProcessor.compressImageWithFallback(
    uri: String,
    candidates: List<ImageFormat> = listOf(ImageFormat.Webp, ImageFormat.Jpeg, ImageFormat.Png),
    maxDimension: Int = 2048,
    quality: Int = 80,
    keepMetadata: Boolean = false,
): Result<ProcessedImage> = firstThatSucceeds(candidates) { format -> compressImage(uri, format, maxDimension, quality, keepMetadata) }

/**
 * The bytes form of [compressImageWithFallback]: tries each format in [candidates] on the image in [bytes], with the
 * same order, skipping and stopping rules. A failure names its source as `"<bytes>"`.
 */
suspend fun ImageProcessor.compressImageWithFallback(
    bytes: ByteArray,
    candidates: List<ImageFormat> = listOf(ImageFormat.Webp, ImageFormat.Jpeg, ImageFormat.Png),
    maxDimension: Int = 2048,
    quality: Int = 80,
    keepMetadata: Boolean = false,
): Result<ProcessedImage> = firstThatSucceeds(candidates) { format -> compressImage(bytes, format, maxDimension, quality, keepMetadata) }

private suspend fun firstThatSucceeds(
    candidates: List<ImageFormat>,
    attempt: suspend (ImageFormat) -> Result<ProcessedImage>,
): Result<ProcessedImage> {
    require(candidates.isNotEmpty()) { "candidates must not be empty" }

    var last: Result<ProcessedImage>? = null
    for (format in candidates) {
        val result = attempt(format)
        if (result.isSuccess) return result
        last = result

        // A source that cannot be read or decoded will fail identically for every
        // remaining format, so stop rather than burn a decode attempt per candidate.
        when (result.exceptionOrNull()) {
            is ImageProcessingException.SourceUnreadable,
            is ImageProcessingException.DecodeFailed -> return result
            else -> Unit
        }
    }
    return last!!
}
