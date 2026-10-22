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
