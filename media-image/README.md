# media-image

Images downsized and encoded for an upload or a file: a photo from the camera or the gallery made small enough to send, as WebP, JPEG or PNG, with the same call on every target.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:media-image:0.0.3")
```

It depends on [media-webp](../media-webp), which encodes the WebP, and on Android on [platform](../platform), which supplies the application context.

## Use

```kotlin
val processor = ImageProcessor()

val image: ProcessedImage = processor.compressImage(uri, format = ImageFormat.Webp, maxDimension = 2048, quality = 80)
    .getOrElse { failure -> return handle(failure) }

upload(image.bytes, contentType = image.contentType)            // image.width and image.height are the size it came out at
```

| Parameter | Default | |
|---|---|---|
| `uri` | required | Android: a `content://` or `file://` URI or a path. iOS and JVM: a `file://` URI or a path. Web: anything an `<img>` loads from this origin (`blob:`, `data:` or a same-origin URL) |
| `format` | `ImageFormat.Webp` | `Webp`, `Jpeg` or `Png` |
| `maxDimension` | 2048 | the longest side, in pixels; the image keeps its shape |
| `quality` | 80 | 0 to 100 for WebP and JPEG; PNG is lossless and ignores it |

- **Images only get smaller.** One already inside `maxDimension` keeps its size.
- **The format reported is the format produced.** `contentType` and `fileExtension` come from the format that was written; it never substitutes another.
- **Failures are typed, in a `Result`**, so a caller can tell what to do next:

| `ImageProcessingException` | Means | Worth trying another format? |
|---|---|---|
| `SourceUnreadable` | the source could not be opened | no |
| `DecodeFailed` | it opened but is not an image | no |
| `AlphaNotRepresentable` | the image has transparency and the format cannot hold it (JPEG) | yes |
| `Unsupported` | this platform cannot write the format | yes |
| `EncodeFailed` | the encoder failed or wrote nothing | yes |

On the web an `<img>` reports a missing source and one that is not an image as the same error, so both arrive as `SourceUnreadable`.

### Falling back

```kotlin
processor.compressImageWithFallback(uri, candidates = listOf(ImageFormat.Webp, ImageFormat.Jpeg, ImageFormat.Png))
```

`compressImageWithFallback` tries each format in turn and returns the first that succeeds. A format the image or the platform cannot take is skipped. A source that cannot be read or decoded stops it at once, since every format would fail the same way.

## How each target works

| Target | Decodes and resizes with | WebP | JPEG and PNG |
|---|---|---|---|
| Android | `BitmapFactory`, subsampling while decoding so a large photo never sits in memory whole | media-webp, libwebp built in | `Bitmap.compress` |
| iOS | UIKit and Core Graphics | media-webp, libwebp built in | UIKit |
| JVM | Skia, which also reads WebP sources | media-webp, Skia | Skia |
| JS, wasmJs | an `<img>` and an offscreen `<canvas>` | media-webp, Skia | the canvas |

- **WebP is the same encoder everywhere a target allows,** rather than whatever each platform ships: Android's own WebP output varies by release and device, iOS's ImageIO cannot be relied on to write it, and Safari's canvas cannot write it at all.
- **Transparency is checked pixel by pixel** before JPEG is refused, so an opaque image that merely carries an alpha channel, as most PNGs do, still encodes as JPEG.

### Android

`ImageProcessor()` reads the application context from `AppContext`, which the platform library captures at start-up, so there is nothing to pass and no dependency-injection container is involved. `ImageProcessor(context)` takes one explicitly.

### JVM

Skia's native library must be on the classpath at run time. A Compose desktop app has it already; anything else adds `org.jetbrains.skiko:skiko-awt-runtime-<os>-<arch>` at the Skiko version its Compose brings. On Linux, Skia also needs the `libgl1`, `libx11-6` and `libegl1` packages.

### Web

The source must come from this origin: a cross-origin image taints the canvas, and the browser then refuses to read it. Skia's WebAssembly must have loaded before WebP is encoded, as it has once a Compose app is showing.

## Not yet

- **Orientation.** A photo whose EXIF says it was taken sideways is encoded as stored, not turned upright, on Android and iOS.
- **Metadata.** The source's EXIF, such as where and when a photo was taken, is not carried into the result. [media-webp](../media-webp) can write it into a WebP with `withWebpMetadata`.
