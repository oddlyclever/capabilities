# Encoding a WebP

DOC-0016 · 2026-11-04
Keywords: encodeToWebp, Bitmap to WebP, UIImage to WebP, BufferedImage to WebP, RgbaPixels, raw RGBA pixels, premultiplied alpha, lossy or lossless, WebpCompression, quality 0 to 100, DEFAULT_WEBP_QUALITY, WebpException, IllegalArgumentException, empty WebP, Bitmap.compress returns nothing, 16383 pixels, image too large, rowBytes stride, main thread, slow encode, lossless file larger on the JVM, WebpEncoder does not exist

Every encode is an extension function that returns the file's bytes. This page covers what each call takes, what the parameters do, how each target encodes, and how a failure arrives.

## The calls

| Call | Where | What it encodes |
|---|---|---|
| `RgbaPixels(width, height, bytes, rowBytes).encodeToWebp(…)` | every target | raw pixels |
| `Bitmap.encodeToWebp(…)` / `Bitmap.toRgbaPixels()` | Android | a bitmap of any config, hardware bitmaps included |
| `UIImage.encodeToWebp(…)` / `UIImage.toRgbaPixels()` | iOS | an image at its full pixel size, turned upright as it is shown |
| `BufferedImage.toRgbaPixels()` | JVM | an AWT image of any type |
| `webp.withWebpMetadata(metadata)` | every target | no pixels: it rewrites the metadata of an existing WebP, see [Writing Metadata](DOC-0017-writing-metadata.md) |
| `bytes.isWebp()` | every target | nothing: it checks the `RIFF`/`WEBP` header |

There is no `encodeToWebp` on `BufferedImage` itself: on the JVM the call is `image.toRgbaPixels().encodeToWebp()`. There is no encoder object or builder either.

## Raw pixels

`RgbaPixels` is `height` rows of `width` pixels, four bytes each in red, green, blue, alpha order, with alpha **not** premultiplied. Row `y` starts at `y * rowBytes`, and `rowBytes` defaults to `width * 4`; a wider stride, with padding at the end of each row, is honoured.

Raw pixels rather than a platform image type keep the encoder the same call on every target, with no UI toolkit needed. The platform conversions exist to get there:

- **`Bitmap.toRgbaPixels()`** reads through `getPixels`, which gives straight alpha from any config. A hardware bitmap, which cannot be read directly, is copied to memory first.
- **`UIImage.toRgbaPixels()`** draws into an sRGB Core Graphics context at the image's full pixel size, not its point size, and unpremultiplies the result. An image whose orientation is not "up" is redrawn upright first, so a photo taken sideways comes out the way it is shown. An image with no bitmap behind it, such as one made from a `CIImage`, throws `WebpException`.
- **`BufferedImage.toRgbaPixels()`** reads through `getRGB`, which gives straight alpha from any image type.
- **In the browser,** a canvas's `getImageData` is already straight-alpha RGBA in this order.

The bytes are not copied, so changing the array after constructing `RgbaPixels` changes what is encoded. Bytes copied straight out of an Android bitmap with `copyPixelsToBuffer`, or out of a Core Graphics or Skia context, are premultiplied; encoded as they are, translucent pixels come out darker than they should.

## Parameters

| Parameter | Default | |
|---|---|---|
| `quality` | `DEFAULT_WEBP_QUALITY`, 80 | 0 to 100 for lossy; higher is larger and closer to the original |
| `compression` | `WebpCompression.Lossy` | `Lossless` keeps every pixel exactly, for screenshots, line art and anything re-encoded later |
| `metadata` | none | written as EXIF; nothing is written when it is null or empty |

`quality` has no effect on the pixels of a lossless encode, but it is still checked to be in 0..100. `quality = 100` is high-quality lossy here, not lossless; lossless is only `compression = WebpCompression.Lossless`.

## How each target encodes

| Target | Lossy | Lossless |
|---|---|---|
| Android | libwebp 1.6.0, built into this library | libwebp |
| iOS | libwebp 1.6.0, built into this library | libwebp |
| JVM | Skia's libwebp | the Kotlin encoder below |
| JS, wasmJs | Skia's libwebp | the Kotlin encoder below |

On the JVM and the web the lossy encoder needs Skia at run time; [Platform Notes](DOC-0018-platform-notes.md) says what that takes. Why the targets differ is in [libwebp Built In on Android and iOS](ADR-0010-libwebp-built-in-on-android-and-ios.md).

**Lossless on the JVM and the web** is written by a small encoder in Kotlin, because Skia offers no lossless WebP. Its output is a standard VP8L file that any decoder reads, and every pixel comes back exactly. It uses the subtract-green transform and one Huffman code for each of green, red, blue and alpha, but no spatial prediction, no colour cache and no backward references, so its files are larger than libwebp's lossless files. It needs no Skia at all.

## Failures

A failure throws. The call never returns an empty or cut-short file, which is how Android's own WebP encoder has been seen to fail, so code that checks the result for `null` or `isEmpty()` never sees a failure; catch the exception instead.

| Thrown | When |
|---|---|
| `IllegalArgumentException` | `quality` outside 0..100 |
| `IllegalArgumentException` | an `RgbaPixels` that is 0 or more than 16383 pixels a side, a `rowBytes` under four bytes a pixel, or `bytes` too short for its rows |
| `WebpException` | the encoder refused the image, or returned bytes that are not a WebP |
| `WebpException` | on iOS, a `UIImage` with no bitmap behind it |

**A WebP is at most 16383 pixels a side.** A larger image is refused when the `RgbaPixels` is made, which happens inside `Bitmap.encodeToWebp` and `UIImage.encodeToWebp` too. Downsize first; [media-image](../media-image/README.md) does that from a file or from the encoded bytes.

## Threading

Encoding is blocking and CPU-bound, tens of milliseconds for a photo, so call it off the main thread, for instance inside `withContext(Dispatchers.Default)`. In the browser there is no other thread, so a large image holds up the page while it encodes.

## What it does not do

It encodes; it does not decode a WebP, read a file's EXIF back, resize, crop, or write animated WebP or XMP. Pixels are encoded as given: a `Bitmap` decoded from a camera JPEG holds the sensor's orientation, so it encodes sideways unless it is turned first. Opening a photo from a file, a URI or its encoded bytes, downsizing it and standing it upright is [media-image](../media-image/README.md).
