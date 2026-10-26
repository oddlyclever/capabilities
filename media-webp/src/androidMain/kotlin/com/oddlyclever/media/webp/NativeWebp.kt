package com.oddlyclever.media.webp

/**
 * The JNI binding to the libwebp built into this library (`libwebp_jni.so`).
 *
 * Its methods are registered by name when the library loads, against this class's name, which `webp_jni.c` holds as
 * `WEBP_JNI_CLASS`. Renaming or moving this class means changing that name and rebuilding the libraries; the
 * library then refuses to load rather than failing on the first call. Consumer keep rules stop R8 renaming it.
 */
internal object NativeWebp {
    init {
        System.loadLibrary("webp_jni")
    }

    /** libwebp's simple encoding API: [rgba] as a WebP, or null when libwebp refuses the image. */
    @JvmStatic
    external fun encodeRgba(rgba: ByteArray, width: Int, height: Int, stride: Int, quality: Float, lossless: Boolean): ByteArray?

    /** libwebp's encoder version as `0xMMmmpp`. */
    @JvmStatic
    external fun encoderVersion(): Int
}
