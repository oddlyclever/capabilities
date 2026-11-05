# Compressing an Image

DOC-0013 · 2026-11-04
Keywords: compressImage, shrink a photo for upload, resize image, downscale, thumbnail, maxDimension, quality, ImageFormat Webp Jpeg Png, ProcessedImage, contentType, file extension, fileExtension, Result failure, ImageProcessingException, SourceUnreadable, DecodeFailed, AlphaNotRepresentable, Unsupported, EncodeFailed, compressImageWithFallback, fallback from WebP to JPEG, content:// URI, https URL fails, photo sideways, image upscaled, ImageCompressor does not exist

`ImageProcessor.compressImage` is the module's call: an image's location in, a downsized and encoded image out, in a `Result`. This page covers each parameter, what the result promises, how failures arrive, and the fallback.

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
| `uri` | required | where the image is; see below |
| `format` | `ImageFormat.Webp` | `Webp`, `Jpeg` or `Png` |
| `maxDimension` | 2048 | the longest side, in pixels; the image keeps its shape |
| `quality` | 80 | 0 to 100 for WebP and JPEG; PNG is lossless and ignores it |
| `keepMetadata` | `false` | write the source's location and capture time into the result; see [Keeping Location and Capture Time](Media-Image-Keeping-Location-And-Capture-Time) |

What `uri` may be depends on the target:

| Target | `uri` |
|---|---|
| Android | a `content://` URI, a `file://` URI, or an absolute path starting with `/` |
| iOS, JVM | a `file://` URI or a path |
| JS, wasmJs | anything an `<img>` loads from this origin: a `blob:` URL from a file input, a `data:` URL, or a same-origin URL |

The input is always a `String`. There is no overload taking a `Bitmap`, a `UIImage`, a `ByteArray` or an Android `Uri` object; pass `uri.toString()`. Only a local file, or a URI the platform opens itself, works: an `https://` URL on Android, iOS or the JVM is not downloaded, and fails as `SourceUnreadable`. On iOS, a photo-library asset or a picker's item provider is not a path; write its data to a file and pass that file's path. Encoding pixels already in memory is [media-webp](Media-Webp).

**Neither `quality` nor `maxDimension` is checked up front.** A `quality` outside 0..100 makes WebP fail as `EncodeFailed` on every target, so the fallback silently moves on to JPEG or PNG. A `maxDimension` of zero or less gives a 1 × 1 image on the JVM and the web, and a failure on Android. Pass a quality in 0..100 and a positive size.

## What the result promises

`ProcessedImage` holds `bytes`, `contentType`, `width` and `height`.

- **Images only get smaller.** One already inside `maxDimension` keeps its size. `maxDimension` bounds the longest side, not both.
- **Photos come out upright.** One whose EXIF says the camera was held sideways or upside down is turned the way it is shown, with its width and height swapped when it is turned a quarter. The `width` and `height` reported are the upright ones: a 4000 × 3000 sensor image taken in portrait comes back as 1536 × 2048 at the default size. The orientation tag is not written into the result, since the pixels are already turned.
- **The format reported is the format produced.** `contentType` is the MIME type of the format that was written, never the one asked for when a fallback moved on. Store it with the bytes rather than inferring the format from the request or the bytes.
- **There is no extension on the result.** The extension is on `ImageFormat`: `ImageFormat.entries.first { it.contentType == image.contentType }.fileExtension` gives `webp`, `jpg` or `png`. Store `contentType` or `fileExtension`, not the enum member's name.
- **No metadata, by default.** None of the source's EXIF is copied unless `keepMetadata = true`.
- **One frame.** Only a single still image is written.

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
- **In the browser, a missing source and one that is not an image** are reported by the `<img>` as the same error, so both arrive as `SourceUnreadable`. A cross-origin image arrives as `EncodeFailed` or `AlphaNotRepresentable`; [How Each Target Works](Media-Image-How-Each-Target-Works) explains why.

Two things do throw: `compressImageWithFallback` with an empty `candidates` list, and on Android the no-argument constructor when the application context was never captured.

## Falling back

```kotlin
processor.compressImageWithFallback(uri, candidates = listOf(ImageFormat.Webp, ImageFormat.Jpeg, ImageFormat.Png))
```

`compressImageWithFallback` tries each format in turn, with the same `maxDimension`, `quality` and `keepMetadata`, and returns the first that succeeds. The default order is WebP, JPEG, PNG, with PNG last as the format that always holds transparency.

- **A format the image or the platform cannot take is skipped,** and so is one whose encoder failed.
- **A source that cannot be read or decoded stops it at once,** with that failure, since every format would fail the same way.
- **When every candidate fails,** the last failure is returned.

`compressImage` itself never substitutes another format; why is in [The Format Asked For, or a Typed Failure](Media-Image-The-Format-Asked-For-Or-A-Typed-Failure).

## What it does not do

It downsizes, turns upright and encodes. It does not crop, rotate on request, filter, read an image's metadata back to the caller, or upload, and it writes only WebP, JPEG and PNG.
