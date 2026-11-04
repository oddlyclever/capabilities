# media-image

Images downsized and encoded for an upload or a file: a photo from the camera or the gallery made small enough to send, as WebP, JPEG or PNG, with the same call on every target.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:media-image:0.0.4")
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
| `keepMetadata` | `false` | write the source's location and capture time into the result; see [Metadata](#metadata) |

- **Images only get smaller.** One already inside `maxDimension` keeps its size.
- **Photos come out upright.** One whose EXIF says the camera was held sideways or upside down is turned the way it is shown, with its width and height swapped when it is turned a quarter. The `width` and `height` reported are the upright ones.
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

## Metadata

```kotlin
processor.compressImage(uri, keepMetadata = true)
```

By default the result carries no metadata at all: none of the source's EXIF is copied. That is deliberate. A photo's EXIF holds where it was taken, which is personal, and most images are uploaded to be seen by other people.

With `keepMetadata = true`, the source's **location** (latitude, longitude and altitude) and its **capture time** are written into the result as EXIF, in every format: a WebP's `EXIF` chunk, a JPEG's APP1 segment or a PNG's `eXIf` chunk. Nothing else of the source's EXIF is kept, not the camera, its settings or the orientation, since the image is already upright.

- **Only an exact capture time is kept:** the original date and time with its offset from UTC, or else the GPS date and time stamp, which is UTC. A date and time with no offset names no moment, since the camera's time zone is unknown, so it is left out rather than guessed. Most phones record the offset.
- **The source's EXIF is read** with ExifInterface on Android and with ImageIO on iOS, both of which also read HEIC. On the JVM and the web it is read here, from JPEG, PNG and WebP sources.

## How each target works

| Target | Decodes and resizes with | WebP | JPEG and PNG |
|---|---|---|---|
| Android | `BitmapFactory`, subsampling while decoding so a large photo never sits in memory whole, turned upright from ExifInterface's orientation | media-webp, libwebp built in | `Bitmap.compress` |
| iOS | ImageIO, which decodes, shrinks and turns upright in one pass, so a large photo is never decoded whole | media-webp, libwebp built in | UIKit |
| JVM | Skia, which also reads WebP sources and turns them upright as it decodes | media-webp, Skia | Skia |
| JS, wasmJs | an `<img>`, which the browser turns upright, and an offscreen `<canvas>` | media-webp, Skia | the canvas |

- **WebP is the same encoder everywhere a target allows,** rather than whatever each platform ships: Android's own WebP output varies by release and device, iOS's ImageIO cannot be relied on to write it, and Safari's canvas cannot write it at all.
- **Transparency is checked pixel by pixel** before JPEG is refused, so an opaque image that merely carries an alpha channel, as most PNGs do, still encodes as JPEG.

### Android

`ImageProcessor()` reads the application context from `AppContext`, which the platform library captures at start-up, so there is nothing to pass and no dependency-injection container is involved. `ImageProcessor(context)` takes one explicitly.

### JVM

Skia's native library must be on the classpath at run time. A Compose desktop app has it already; anything else adds `org.jetbrains.skiko:skiko-awt-runtime-<os>-<arch>` at the Skiko version its Compose brings. On Linux, Skia also needs the `libgl1`, `libx11-6` and `libegl1` packages.

### Web

The source must come from this origin: a cross-origin image taints the canvas, and the browser then refuses to read it. Skia's WebAssembly must have loaded before WebP is encoded, as it has once a Compose app is showing.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-media-image`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-media-image/SKILL.md](src/commonMain/skills/com-oddlyclever-media-image/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-media-image/`.
