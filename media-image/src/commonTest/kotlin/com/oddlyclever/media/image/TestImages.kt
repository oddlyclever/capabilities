package com.oddlyclever.media.image

/** Waits until Skia can be used: at once except on the web, where its WebAssembly loads asynchronously. */
expect suspend fun awaitSkia()

/**
 * A PNG with an alpha channel, [width] by [height], a colour gradient at [alpha], written where this platform's
 * [ImageProcessor] reads from; returns what to pass it as the source.
 */
expect suspend fun writeTestImage(width: Int, height: Int, alpha: Int = 255): String

/** A source that does not exist. */
expect fun missingSource(): String

/** A source that exists but is not an image. */
expect suspend fun notAnImage(): String

/**
 * Whether this platform can tell a source it cannot read from one that is not an image. The web cannot: an `<img>`
 * reports both as the same error.
 */
expect val tellsUnreadableFromUndecodable: Boolean

/** An opaque image as encoded bytes, JPEG or PNG: its left half red and its right half blue, so a turn shows which way it went. */
expect suspend fun twoToneImage(width: Int, height: Int, jpeg: Boolean): ByteArray

/** Writes [bytes] where this platform's [ImageProcessor] reads from, and returns what to pass it as the source. */
expect suspend fun writeSource(bytes: ByteArray, jpeg: Boolean): String

/** The ARGB colour at ([x], [y]) of an encoded image, or null on a platform with no decoder these tests can call. */
expect fun pixelAt(encoded: ByteArray, x: Int, y: Int): Int?
