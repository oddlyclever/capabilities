# media-image

Images downsized and encoded for an upload or a file: a photo from the camera or the gallery made small enough to send, as WebP, JPEG or PNG, with the same call on every target.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:media-image:0.0.8")
```

It depends on [media-webp](../media-webp), which encodes the WebP, and on Android on [platform](../platform), which supplies the application context, so `ImageProcessor()` needs nothing passed in. On the JVM it needs Skia's native library at run time, which a Compose desktop app already has; [How Each Target Works](../docs/knowledge/media-image/DOC-0015-how-each-target-works.md) says what anything else adds.

## Use

```kotlin
val processor = ImageProcessor()

val image: ProcessedImage = processor.compressImage(uri, format = ImageFormat.Webp, maxDimension = 2048, quality = 80)
    .getOrElse { failure -> return handle(failure) }

upload(image.bytes, contentType = image.contentType)            // image.width and image.height are the size it came out at
```

Images only get smaller, photos come out upright, and `contentType` is the format that was actually written. Failures come back in the `Result` as a typed `ImageProcessingException`, and `compressImageWithFallback` tries a list of formats in turn. The result carries no metadata unless you pass `keepMetadata = true`, which keeps the source's location and capture time.

## Documentation

The full documentation is in the module's section of the [knowledge base](../docs/knowledge/media-image/README.md), which is mirrored to the repository's wiki:

- [Compressing an Image](../docs/knowledge/media-image/DOC-0013-compressing-an-image.md): every parameter, what `uri` may be, the typed failures, and the fallback.
- [Keeping Location and Capture Time](../docs/knowledge/media-image/DOC-0014-keeping-location-and-capture-time.md): what `keepMetadata` keeps and where it is written.
- [How Each Target Works](../docs/knowledge/media-image/DOC-0015-how-each-target-works.md): the decoders and encoders on each target, and what each needs from the app.
- [No Metadata Unless Asked](../docs/knowledge/media-image/ADR-0007-no-metadata-unless-asked.md), [The Format Asked For, or a Typed Failure](../docs/knowledge/media-image/ADR-0008-the-format-asked-for-or-a-typed-failure.md) and [WebP Through media-webp on Every Target](../docs/knowledge/media-image/ADR-0009-webp-through-media-webp-on-every-target.md): why the module is built the way it is.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-media-image`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-media-image/SKILL.md](src/commonMain/skills/com-oddlyclever-media-image/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-media-image/`.
