---
name: com-oddlyclever-media-image
description: >-
  Shrink a photo for upload in Kotlin Multiplatform: open an image file, a
  content:// URI, a blob URL or encoded bytes already in memory, downsize it
  to a maximum width and height (resize, scale down, compress, make a
  thumbnail), stand it upright from its
  EXIF orientation, and encode it as WebP, JPEG or PNG, with the same suspend
  call on Android, iOS, the JVM and the browser. Use instead of writing
  BitmapFactory inSampleSize, ImageIO thumbnail or canvas resizing code for
  each platform. Returns the bytes, the MIME content type and the final size
  in a Result with typed failures, refuses JPEG for an image with
  transparency, falls back from one format to the next, and strips EXIF by
  default for privacy, removing the geotag, or keeps only the GPS location and
  capture time when asked. Tells an SVG or an animated GIF from its bytes, to
  upload it as it is rather than rasterize it or lose its frames. For encoding
  a Bitmap, UIImage or raw pixels to WebP, see com.oddlyclever:media-webp.
license: Apache-2.0
metadata:
  version: "0.0.8"
  repository: https://github.com/oddlyclever/capabilities
---

# media-image

`com.oddlyclever:media-image`, package `com.oddlyclever.media.image`. Called from Kotlin only: there is no Swift or JavaScript surface.

## What it solves

A photo from the camera or the gallery is too big to send: tens of megapixels, stored sideways with an EXIF flag saying how to turn it, and carrying where it was taken. This takes the image's location or its bytes, decodes it without holding the full-size photo in memory where the platform allows, scales it down so its longest side fits, turns it the way it is meant to be seen, and encodes it in the format asked for, reporting the format it actually wrote. Each target uses its own decoder: `BitmapFactory` with subsampling on Android, ImageIO on iOS, Skia on the JVM, and an `<img>` and a `<canvas>` in the browser. WebP is encoded by the same library everywhere, with libwebp built in on Android and iOS; JPEG and PNG use each platform's own encoder.

## How it is meant to be used

Construct an `ImageProcessor` directly and call `compressImage`, which is a suspend function returning a `Result`. It moves its work off the calling thread on Android, iOS and the JVM.

```kotlin
import com.oddlyclever.media.image.ImageFormat
import com.oddlyclever.media.image.ImageProcessingException
import com.oddlyclever.media.image.ImageProcessor

val processor = ImageProcessor()

processor.compressImage(uri, format = ImageFormat.Webp, maxDimension = 2048, quality = 80)
    .onSuccess { image -> upload(image.bytes, contentType = image.contentType) } // image.width, image.height
    .onFailure { failure ->
        when (failure) {
            is ImageProcessingException.SourceUnreadable, is ImageProcessingException.DecodeFailed -> showNotAnImage()
            else -> showTryAgain()
        }
    }
```

To take whichever format works, use the fallback, which tries each candidate in order and skips a format the image or the platform cannot take:

```kotlin
import com.oddlyclever.media.image.compressImageWithFallback

val image = processor.compressImageWithFallback(uri, candidates = listOf(ImageFormat.Webp, ImageFormat.Jpeg, ImageFormat.Png))
    .getOrElse { return }
```

It stops at once on `SourceUnreadable` or `DecodeFailed`, since every format would fail the same way, and otherwise returns the last failure. `keepMetadata = true` on either call writes the source's location and capture time into the result.

An image the app already holds as encoded bytes, drawn, downloaded or handed over by a browser's file input, goes in as a `ByteArray`, with the same parameters, results and failures, and the fallback takes bytes too:

```kotlin
processor.compressImage(bytes, format = ImageFormat.Webp)
processor.compressImageWithFallback(bytes)
```

To keep an image that converting would spoil, read the source and look at it first. `imageKindOf` reads the bytes without decoding them and returns `ImageKind.Svg`, `AnimatedGif`, `Gif`, `Webp`, `Png`, `Jpeg`, or null:

```kotlin
import com.oddlyclever.media.image.ImageKind
import com.oddlyclever.media.image.imageKindOf

val original = processor.readSource(uri).getOrElse { return }
when (val kind = imageKindOf(original)) {
    ImageKind.Svg, ImageKind.AnimatedGif -> upload(original, contentType = kind.contentType) // as it is
    else -> processor.compressImage(original).onSuccess { upload(it.bytes, it.contentType) }
}
```

What `uri` may be differs by target. Android: a `content://` URI, a `file://` URI, or an absolute path starting with `/`. iOS and the JVM: a `file://` URI or a path. The browser: anything an `<img>` loads from this origin, such as a `blob:` URL from a file input, a `data:` URL or a same-origin URL.

## Invariants and traps

**Failures come back in the `Result`; they are not thrown.** A `try`/`catch` around `compressImage` catches nothing useful, and `result.getOrNull() ?: …` loses the reason. Read `exceptionOrNull()` as an `ImageProcessingException`: `SourceUnreadable` (could not open), `DecodeFailed` (not an image), `AlphaNotRepresentable` (transparent pixels, JPEG asked for), `EncodeFailed` (the encoder failed, worth another format) and `Unsupported`, which only the browser returns, when its canvas wrote a different type than the one asked for. Two things throw by design: `compressImageWithFallback` with an empty `candidates` list, and on Android the no-argument constructor when the application context was never captured.

**Store `contentType`, not the format you asked for.** After a fallback the bytes may be JPEG or PNG though WebP came first. `ProcessedImage` has `bytes`, `contentType`, `width` and `height` only: there is no `format` or `fileExtension` on it. The extension is on `ImageFormat`: `ImageFormat.entries.first { it.contentType == image.contentType }.fileExtension` gives `webp`, `jpg` or `png`.

**Only a local file or a URI the platform opens itself works as a location.** An `https://` URL on Android, iOS or the JVM is not downloaded and fails as `SourceUnreadable`, and so does a relative path on Android; download it with your own client and pass the bytes. On iOS, a photo-library asset or a picker's item provider is not a path; pass its data as bytes.

**In the browser, a cross-origin image fails as an encoding error, not as unreadable.** It loads, but taints the canvas, so the browser refuses to give its pixels back: WebP and PNG fail as `EncodeFailed`, and JPEG as `AlphaNotRepresentable`, because pixels that cannot be checked are treated as possibly transparent. Load it from this origin, or as a `blob:` URL. A missing source and one that is not an image both arrive as `SourceUnreadable` there, not as `DecodeFailed`.

**`compressImage` converts whatever decodes; it never refuses an SVG or an animated GIF.** In the browser an SVG is rasterized, losing its scaling and usually growing larger; on the other targets it fails as `DecodeFailed`. An animated GIF keeps only its first frame everywhere. There is no failure type for either; check `imageKindOf` first.

**A failure from bytes names `"<bytes>"`.** `SourceUnreadable.uri` and `DecodeFailed.uri` hold `"<bytes>"` for the bytes form, never null. Bytes that are not an image, an empty array included, are `DecodeFailed` on every target, the browser too.

**`imageKindOf` knows an SVG only by its root element.** It looks for `<svg` within the first 64 KiB, past a byte-order mark, an XML declaration, comments and a doctype. HTML with an SVG inside, a gzipped `.svgz` and an SVG in UTF-16 all return null, and so does any format not in `ImageKind`, such as HEIC or BMP, even where `compressImage` decodes it. Null means "not one of these", not "not an image".

**`readSource` holds the whole file in memory.** The location form of `compressImage` streams a large photo on Android and iOS; reading it first and passing the bytes gives the same image but keeps the whole encoded file in memory. Read a source when the bytes are wanted, not as a routine first step.

**WebP in the browser needs Skia's WebAssembly loaded,** as it is once a Compose app is showing. Before that, WebP fails as `EncodeFailed`, and `compressImageWithFallback` quietly moves on to JPEG. **On the JVM, everything decodes through Skia's native library** for the machine, which Skiko's `skiko-awt-runtime-<os>-<arch>` artifacts carry and a Compose desktop app already has; without it nothing decodes. Skia there reads JPEG, PNG, WebP, GIF and BMP, so a HEIC file fails as `DecodeFailed` on the JVM, while iOS's ImageIO decodes it.

**Neither `quality` nor `maxDimension` is checked up front.** A `quality` outside 0..100 makes WebP fail as `EncodeFailed` on every target, so the fallback silently moves on to JPEG or PNG. A `maxDimension` of zero or less does different wrong things per target, such as a 1-by-1 image on the JVM and the web. Pass a quality in 0..100 and a positive size. Images are only made smaller: one already inside `maxDimension` keeps its size, and `maxDimension` bounds the longest side, not both.

**Width and height are the upright ones.** A photo stored sideways (EXIF orientation 5 to 8) comes back with its sides swapped, so a 4000 × 3000 sensor image taken in portrait reports as 1536 × 2048 at the default size. The orientation tag itself is not written into the result, since the pixels are already turned.

**No metadata by default.** None of the source's EXIF is copied unless `keepMetadata = true`, so a result carries no location. With it, only the GPS location, with its altitude where recorded, and the capture time are written, in every format. A capture time is kept only when the source records the moment exactly, as an original date and time with its UTC offset, or as a GPS date and time stamp; a camera that writes a local time with no offset gives a result with a location but no time. In the browser a location is fetched a second time to read its EXIF, and if that fetch fails the metadata is silently left out; bytes are read as they are. There is no parameter for writing metadata of your own.

**On Android, `ImageProcessor()` reads the application context** that `com.oddlyclever:platform` captures at start-up through App Startup. An app that removes App Startup's `InitializationProvider` from its merged manifest gets an `IllegalStateException` from the constructor saying so; `ImageProcessor(context)`, which exists only in Android code, takes the context instead.

## What moved, and what it used to be called

`media-image` was first released in 0.0.3, and its API has only grown since, so code written for an earlier version still compiles.

- **Since 0.0.9, the image can be given as bytes,** to `compressImage` and `compressImageWithFallback`, and `readSource`, `imageKindOf` and `ImageKind` exist. Before it the input was a location only, and code that wrote bytes to a temporary file, or made a `blob:` URL in the browser, just to pass them in can hand them over directly.
- **The input is a location or encoded bytes, not pixels.** There is no overload taking a `Bitmap`, a `UIImage` or an Android `Uri` object; pass `uri.toString()`. Encoding in-memory pixels to WebP is `com.oddlyclever:media-webp`, package `com.oddlyclever.media.webp`, which this module uses but does not expose.
- **The result is `Result<ProcessedImage>`,** not a `ByteArray` and not a thrown exception, and the call suspends.
- **Formats are `ImageFormat.Webp`, `ImageFormat.Jpeg` and `ImageFormat.Png`,** not `WEBP`, `JPEG`, Android's `Bitmap.CompressFormat` or a MIME string.
- **There is no `ImageCompressor`, `compress(…)` or builder,** and no dependency-injection module: construct `ImageProcessor()`.

## What it is not for

It downsizes, turns upright and encodes; it does not crop, rotate on request, filter, read an image's metadata back to the caller, or upload. It writes only WebP, JPEG and PNG, a single frame, and keeps only location and capture time from the source. It recognises an SVG but does not convert it to anything scalable, and it does not keep a GIF's animation. Encoding pixels the app has already decoded, a `Bitmap`, a `UIImage` or raw RGBA, or writing chosen metadata into a WebP, is `com.oddlyclever:media-webp`.
