# Compressing an Image

DOC-0013 · 2026-11-04
Keywords: compressImage, shrink a photo for upload, resize image, downscale, thumbnail, maxDimension, quality, ImageFormat Webp Jpeg Png, ProcessedImage, contentType, file extension, fileExtension, Result failure, ImageProcessingException, SourceUnreadable, DecodeFailed, AlphaNotRepresentable, Unsupported, EncodeFailed, compressImageWithFallback, fallback from WebP to JPEG, content:// URI, https URL fails, photo sideways, image upscaled, ImageCompressor does not exist, compress from bytes, ByteArray overload, image already in memory, readSource, imageKindOf, ImageKind, keep an SVG as it is, animated GIF loses its frames, don't convert SVG

`ImageProcessor.compressImage` is the module's call: an image's location or its bytes in, a downsized and encoded image out, in a `Result`. This page covers each parameter, what the result promises, how failures arrive, the fallback, and how to tell an image that should not be converted.

## The call

```kotlin
val processor = ImageProcessor()

val image: ProcessedImage = processor.compressImage(uri, format = ImageFormat.Webp, maxDimension = 2048, quality = 80)
    .getOrElse { failure -> return handle(failure) }

upload(image.bytes, contentType = image.contentType)            // image.width and image.height are the size it came out at
```

It is a `suspend` function. On Android and the JVM it moves its work to `Dispatchers.IO`, and on iOS to `Dispatchers.Default`, so it can be called from the main thread there. In the browser there is no other thread, and it runs where it is called.

## Parameters

| Parameter | Default | |
|---|---|---|
| `uri` or `bytes` | required | where the image is, see below, or the image itself as a `ByteArray`; see [From bytes](#from-bytes) |
| `format` | `ImageFormat.Webp` | `Webp`, `Jpeg` or `Png` |
| `maxDimension` | 2048 | the longest side, in pixels; the image keeps its shape |
| `quality` | 80 | 0 to 100 for WebP and JPEG; PNG is lossless and ignores it |
| `keepMetadata` | `false` | write the source's location and capture time into the result; see [Keeping Location and Capture Time](DOC-0014-keeping-location-and-capture-time.md) |

What `uri` may be depends on the target:

| Target | `uri` |
|---|---|
| Android | a `content://` URI, a `file://` URI, or an absolute path starting with `/` |
| iOS, JVM | a `file://` URI or a path |
| JS, wasmJs | anything an `<img>` loads from this origin: a `blob:` URL from a file input, a `data:` URL, or a same-origin URL |

A location is a `String`. There is no overload taking a `Bitmap`, a `UIImage` or an Android `Uri` object; pass `uri.toString()`. Only a local file, or a URI the platform opens itself, works: an `https://` URL on Android, iOS or the JVM is not downloaded, and fails as `SourceUnreadable`; download it with your own client and pass the bytes. On iOS, a photo-library asset or a picker's item provider is not a path; pass its data as bytes. Encoding pixels already decoded, a `Bitmap`, a `UIImage` or raw RGBA, is [media-webp](../media-webp/README.md).

**Neither `quality` nor `maxDimension` is checked up front.** A `quality` outside 0..100 makes WebP fail as `EncodeFailed` on every target, so the fallback silently moves on to JPEG or PNG. A `maxDimension` of zero or less gives a 1 × 1 image on the JVM and the web, and a failure on Android. Pass a quality in 0..100 and a positive size.

## What the result promises

`ProcessedImage` holds `bytes`, `contentType`, `width` and `height`.

- **Images only get smaller.** One already inside `maxDimension` keeps its size. `maxDimension` bounds the longest side, not both.
- **Photos come out upright.** One whose EXIF says the camera was held sideways or upside down is turned the way it is shown, with its width and height swapped when it is turned a quarter. The `width` and `height` reported are the upright ones: a 4000 × 3000 sensor image taken in portrait comes back as 1536 × 2048 at the default size. The orientation tag is not written into the result, since the pixels are already turned.
- **The format reported is the format produced.** `contentType` is the MIME type of the format that was written, never the one asked for when a fallback moved on. Store it with the bytes rather than inferring the format from the request or the bytes.
- **There is no extension on the result.** The extension is on `ImageFormat`: `ImageFormat.entries.first { it.contentType == image.contentType }.fileExtension` gives `webp`, `jpg` or `png`. Store `contentType` or `fileExtension`, not the enum member's name.
- **No metadata, by default.** None of the source's EXIF is copied unless `keepMetadata = true`.
- **One frame.** Only a single still image is written: an animated GIF keeps its first frame. Nothing is refused for what kind of image it is, so an SVG is rasterized in the browser and fails as `DecodeFailed` elsewhere; [Telling what an image is](#telling-what-an-image-is) is how to keep either as it is.

## Failures

Failures come back in the `Result`; they are not thrown. A `try`/`catch` around the call catches nothing useful, and `result.getOrNull() ?: …` loses the reason. Read `exceptionOrNull()` as an `ImageProcessingException`:

| `ImageProcessingException` | Means | Worth trying another format? |
|---|---|---|
| `SourceUnreadable` | the source could not be opened | no |
| `DecodeFailed` | it opened but is not an image | no |
| `AlphaNotRepresentable` | the image has transparency and the format cannot hold it (JPEG) | yes |
| `Unsupported` | this platform cannot write the format | yes |
| `EncodeFailed` | the encoder failed or wrote nothing | yes |

- **Transparency is checked pixel by pixel** before JPEG is refused, so an opaque image that merely carries an alpha channel, as most PNGs do, still encodes as JPEG. `ImageFormat.supportsAlpha` says which formats hold transparency.
- **Only the browser returns `Unsupported`,** when its canvas writes a different type from the one asked for.
- **On Android, anything thrown while decoding or encoding,** an out-of-memory error included, arrives as `EncodeFailed` with the cause attached.
- **In the browser, a missing source and one that is not an image** are reported by the `<img>` as the same error, so both arrive as `SourceUnreadable`. A cross-origin image arrives as `EncodeFailed` or `AlphaNotRepresentable`; [How Each Target Works](DOC-0015-how-each-target-works.md) explains why.

A failure from the bytes form names its source as `"<bytes>"`, in `SourceUnreadable.uri` and `DecodeFailed.uri`.

Two things do throw: `compressImageWithFallback` with an empty `candidates` list, and on Android the no-argument constructor when the application context was never captured.

## Falling back

```kotlin
processor.compressImageWithFallback(uri, candidates = listOf(ImageFormat.Webp, ImageFormat.Jpeg, ImageFormat.Png))
```

`compressImageWithFallback` tries each format in turn, with the same `maxDimension`, `quality` and `keepMetadata`, and returns the first that succeeds. The default order is WebP, JPEG, PNG, with PNG last as the format that always holds transparency.

- **A format the image or the platform cannot take is skipped,** and so is one whose encoder failed.
- **A source that cannot be read or decoded stops it at once,** with that failure, since every format would fail the same way.
- **When every candidate fails,** the last failure is returned.

`compressImage` itself never substitutes another format; why is in [The Format Asked For, or a Typed Failure](ADR-0008-the-format-asked-for-or-a-typed-failure.md).

## From bytes

```kotlin
val image = processor.compressImage(bytes, format = ImageFormat.Webp)       // bytes: ByteArray, an encoded image
val any = processor.compressImageWithFallback(bytes)
```

Both calls take an encoded image as a `ByteArray` in place of its location, for an image the caller already holds: one an app drew, one a browser's file input handed over, one that came off the network. The parameters, sizing, orientation, metadata, failures and fallback are the same as for a location, and the same image gives the same result either way. Bytes that are not an image, an empty array among them, fail as `DecodeFailed`, in the browser too, where a location that does not decode arrives as `SourceUnreadable`.

`readSource(uri)` goes the other way: the bytes at a location, unchanged, from exactly the locations `compressImage` reads on that target, or `SourceUnreadable`. Read a source when the bytes themselves are wanted, to look at them or to store the original; it holds the whole file in memory, which the location form of `compressImage` avoids on Android and iOS for a large photo.

## Telling what an image is

```kotlin
val original = processor.readSource(uri).getOrElse { return handle(it) }
when (val kind = imageKindOf(original)) {
    ImageKind.Svg, ImageKind.AnimatedGif -> upload(original, contentType = kind.contentType)   // as it is
    else -> processor.compressImage(original).onSuccess { upload(it.bytes, it.contentType) }
}
```

`imageKindOf(bytes)` says what an image is from its bytes, without decoding it: `Svg`, `AnimatedGif`, `Gif`, `Webp`, `Png` or `Jpeg`, or null for anything else. Each `ImageKind` carries the `contentType` to store it under unconverted. Two kinds are usually better left as they are: rasterizing an SVG makes it larger and stops it scaling, and converting an animated GIF keeps only its first frame. `compressImage` does not refuse either, so the choice is the caller's.

- **A GIF is animated when it holds a second frame.** The file is walked block by block, without decompressing anything, so a large one may be read to its second frame.
- **An SVG is known by its root element,** `<svg`, after an optional byte-order mark, whitespace, XML declaration, comments and doctype, within the first 64 KiB. HTML with an SVG inside is not an SVG. A compressed `.svgz`, or an SVG written in UTF-16, is not recognised.
- **The kinds are sources, not targets.** `ImageFormat` is what `compressImage` writes; an `ImageKind` is what was handed in.

## What it does not do

It downsizes, turns upright and encodes. It does not crop, rotate on request, filter, read an image's metadata back to the caller, or upload, and it writes only WebP, JPEG and PNG. It does not convert an SVG to a scalable format or keep a GIF's animation.
