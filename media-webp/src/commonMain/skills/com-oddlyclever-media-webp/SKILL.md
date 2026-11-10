---
name: com-oddlyclever-media-webp
description: >-
  Encode pixels already in memory to WebP in Kotlin Multiplatform: an Android
  Bitmap, an iOS UIImage, a JVM BufferedImage or raw RGBA bytes, lossy at a
  quality or lossless, with libwebp built in on Android and iOS so the output
  does not depend on the Android version or the device maker. Use instead of
  Bitmap.compress with WEBP or WEBP_LOSSY, which varies by release and can
  return nothing, instead of binding libwebp through JNI or a CocoaPod
  yourself, or where iOS or Safari cannot be relied on to write WebP. Also
  writes metadata into a WebP as EXIF: a geotag (GPS latitude, longitude, altitude), the
  capture date and time, and the software name; adds, replaces or strips the
  EXIF of an existing WebP from any encoder, such as a server or Skia; and
  gives the same tags as a bare EXIF block for a JPEG or PNG. For opening a
  photo file or URI, or an encoded JPEG or PNG already in memory, downsizing it
  or choosing JPEG or PNG, see com.oddlyclever:media-image.
license: Apache-2.0
metadata:
  version: "0.0.8"
  repository: https://github.com/oddlyclever/capabilities
---

# media-webp

`com.oddlyclever:media-webp`, package `com.oddlyclever.media.webp`. Called from Kotlin only: there is no Swift or JavaScript surface.

## What it solves

Turning an image an app already holds into a small WebP file, and getting the same encoder on every device: Android's own WebP output depends on the release and the device maker's build, iOS has no WebP encoder an app can rely on, and Safari's canvas cannot write WebP at all. It also stamps where and when an image was made into the file, as EXIF GPS and date tags, and can add, replace or remove that EXIF on a WebP that some other encoder produced, without re-encoding the image.

How each target encodes: Android and iOS use the libwebp compiled into this library for both lossy and lossless. The JVM and the browser (JS and wasmJs) encode lossy with Skia's libwebp, and lossless with a small encoder written in Kotlin. The metadata is written by common Kotlin code, so it is identical everywhere.

## How it is meant to be used

Every call is an extension function that returns the file's bytes. On Android and iOS, encode the platform image directly; elsewhere, make an `RgbaPixels` first.

```kotlin
import com.oddlyclever.media.webp.GpsLocation
import com.oddlyclever.media.webp.RgbaPixels
import com.oddlyclever.media.webp.WebpCompression
import com.oddlyclever.media.webp.WebpMetadata
import com.oddlyclever.media.webp.encodeToWebp
import kotlin.time.Instant

// Android Bitmap or iOS UIImage
val photo: ByteArray = bitmap.encodeToWebp(
    quality = 80,
    metadata = WebpMetadata(
        location = GpsLocation(latitude = 51.5007, longitude = -0.1246, altitude = 12.0),
        capturedAt = Instant.parse("2026-03-09T14:05:07Z"),
        software = "Acme Camera",
    ),
)

// Any target: four bytes a pixel, R, G, B, A, alpha not premultiplied
val pixels = RgbaPixels(width = 640, height = 480, bytes = rgba)
val screenshot: ByteArray = pixels.encodeToWebp(compression = WebpCompression.Lossless)
```

On the JVM, `bufferedImage.toRgbaPixels().encodeToWebp()`; there is no `encodeToWebp` on `BufferedImage` itself. `Bitmap.toRgbaPixels()` and `UIImage.toRgbaPixels()` exist too, for when the pixels are wanted before encoding. A canvas's `getImageData` in the browser is already straight-alpha RGBA in this order.

For a WebP from somewhere else, `webp.withWebpMetadata(metadata)` rewrites only the RIFF container, and `webp.isWebp()` checks the header. `metadata.toExif()` gives the same tags as a bare TIFF-structured EXIF block, with no `Exif\0\0` prefix, for writing into a JPEG APP1 segment (which wants the prefix) or a PNG `eXIf` chunk.

`DEFAULT_WEBP_QUALITY` is 80. Encoding is blocking and CPU-bound, tens of milliseconds for a photo, so call it off the main thread, for instance inside `withContext(Dispatchers.Default)`. In the browser there is no other thread, so a large image holds up the page while it encodes.

## Invariants and traps

**Pixels must be straight alpha, in R, G, B, A byte order.** Bytes copied straight out of an Android `ARGB_8888` bitmap with `copyPixelsToBuffer`, or out of a Core Graphics or Skia context, are premultiplied, and their translucent pixels come out darker than they should. Use `Bitmap.toRgbaPixels()` or `UIImage.toRgbaPixels()`, which convert. `RgbaPixels` does not copy `bytes`, so changing the array before encoding changes the image.

**Failure throws; it never returns empty bytes.** The encoder refusing an image is a `WebpException`, a `quality` outside 0..100 is an `IllegalArgumentException`, and so is an `RgbaPixels` whose `bytes` are too short for `width`, `height` and `rowBytes`. On iOS, `UIImage.toRgbaPixels()` throws `WebpException` for an image with no bitmap behind it, such as one made from a `CIImage`. Code that checks the result for `null` or `isEmpty()`, as one would after `Bitmap.compress`, never sees the failure: catch the exception.

**A WebP is at most 16383 pixels a side.** A larger image is refused with an `IllegalArgumentException` when the `RgbaPixels` is made, which happens inside `Bitmap.encodeToWebp` and `UIImage.encodeToWebp` too. Downsize first.

**`quality` does nothing for lossless,** on any target, though it is still checked to be in 0..100. Lossless on the JVM and the browser comes from the Kotlin encoder, which has no prediction or backward references, so the same image is larger there than lossless from Android or iOS. Every pixel still comes back exactly.

**On the JVM and the web, lossy needs Skia.** On the JVM it uses Skia's native library for the machine at run time, which Skiko's `skiko-awt-runtime-<os>-<arch>` artifacts carry and a Compose desktop app already has; on Linux it needs `libGL.so.1`, `libX11.so.6` and `libEGL.so.1`. In the browser, Skia's WebAssembly must have loaded before the first lossy encode, as it has once a Compose app is showing. Lossless uses no Skia on either.

**Orientation is not written, and pixels are encoded as given.** A `Bitmap` decoded from a camera JPEG holds the sensor's orientation, so a photo taken with the phone held upright encodes sideways. `UIImage.toRgbaPixels()` does turn an image upright from its `imageOrientation`, at its full pixel size, not its point size. Reading a photo's EXIF orientation from a file and standing it upright is what `com.oddlyclever:media-image` does.

**`withWebpMetadata` replaces the whole EXIF chunk.** A camera's WebP keeps its pixels and any XMP, but its own EXIF tags, such as the camera model or orientation, are dropped and only the fields of the new `WebpMetadata` remain. `WebpMetadata()` with nothing set removes the EXIF. Bytes that are not a WebP throw `WebpException`.

**The capture time is `kotlin.time.Instant`, written in UTC.** Not `kotlinx.datetime.Instant`. It is written as the EXIF original date and time with a `+00:00` offset and milliseconds, plus the GPS date and time stamp when a location is set; the photographer's local time zone is not recorded. Only fields that are set are written, and an empty `software` string counts as unset.

**`GpsLocation` validates on construction.** A latitude outside -90..90, a longitude outside -180..180, `NaN` in either, or an altitude that is not finite throws `IllegalArgumentException`. Latitude is degrees north and longitude degrees east, so the western and southern hemispheres are negative numbers, not a separate reference letter.

**On iOS, a second static libwebp in the same binary can clash at link time,** for instance one carried by an image library's WebP coder. On Android this library's native code exports only its JNI entry point, and its keep rule for R8 is published with it, so a second libwebp in an Android app does not clash and minification needs nothing from the app.

## What moved, and what it used to be called

`media-webp` was first released in 0.0.3, and nothing in its API has changed since, so no earlier published versions exist. Before that it was built only from source, as the `:webp` module with package `com.oddlyclever.webp`, and never published under that name; imports of `com.oddlyclever.webp` are now `com.oddlyclever.media.webp`.

The shapes an agent is likely to guess instead do not exist here:

- **There is no encoder object.** No `WebpEncoder`, `WebP.encode(bitmap)` or builder: the call is the extension `encodeToWebp(quality, compression, metadata)` on `RgbaPixels`, `Bitmap` or `UIImage`.
- **Lossless is `compression = WebpCompression.Lossless`,** not `lossless = true` and not `quality = 100`, which on Android's own `Bitmap.compress` once meant lossless on some releases and here is simply high-quality lossy.
- **The result is a `ByteArray`,** not a `Result`, not nullable and not suspending.
- **Metadata is `WebpMetadata`**, not an `ExifInterface` or a map of tag names; there is no way to set arbitrary EXIF tags.

## What it is not for

It encodes; it does not decode WebP, read a file's EXIF back, resize, or crop. Opening an image from a file, a URI or a URL, or from its encoded bytes, downsizing it, standing it upright and choosing between WebP, JPEG and PNG is `com.oddlyclever:media-image`, package `com.oddlyclever.media.image`. Animated WebP and XMP metadata are not written.
