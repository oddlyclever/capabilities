# Encoding PNG and WebP

DOC-0034 · 2026-11-04
Keywords: encodeToPng, encodeToWebp, save map as file, map image upload, ByteArray, WebP quality, MAP_WEBP_QUALITY, lossless WebP, JPEG not supported, EXIF, GPS location in the image, capture time, WebpMetadata, GpsLocation, WebpException, empty WebP file, Bitmap.compress, image too large, 16383 pixels, email attachment, WebP not shown in mail client

A rendered map is an `ImageBitmap`. Two extension functions turn it into the bytes of a file, for a share sheet, an upload, an email or a record. This page covers both, what goes into the file, and what each refuses.

## The two formats

| Call | What it gives |
|---|---|
| `image.encodeToPng()` | Lossless and larger. Shown everywhere. No metadata. |
| `image.encodeToWebp(quality = MAP_WEBP_QUALITY, metadata = null)` | Lossy, several times smaller than PNG, with optional EXIF metadata. |

Both are blocking CPU work. On Android, call them off the main thread.

There is no JPEG, and the WebP here is lossy only. PNG is the lossless choice.

Some desktop mail clients do not show WebP images, so a map going into an email is safer as PNG.

## WebP

```kotlin
val webp: ByteArray = image.encodeToWebp()
val smaller: ByteArray = image.encodeToWebp(quality = 75)
```

`quality` is 0 to 100, higher being larger and closer to the original. The default, `MAP_WEBP_QUALITY`, is 90: WebP's artefacts show first on thin lines, and at 90 roads and labels keep their edges. That is higher than media-webp's own default, 80.

WebP is encoded by [media-webp](Media-Webp): with libwebp built into it on Android and iOS, so the output does not depend on the device's own encoder, and with Skia's libwebp on the JVM and the web. The image's pixels are handed over with straight alpha, so a translucent area keeps its colour rather than darkening.

A failure throws. It never returns an empty or truncated file.

### Metadata

Pass a `WebpMetadata` to write where the map is, when it was made and what made it into the file, as EXIF:

```kotlin
val webp = image.encodeToWebp(
    metadata = WebpMetadata(
        location = GpsLocation(center.lat, center.lon),
        capturedAt = Clock.System.now(),
        software = "Acme",
    ),
)
```

- `location` is written as EXIF GPS tags. `GpsLocation` takes latitude, longitude and an optional altitude in metres, and throws `IllegalArgumentException` for a latitude outside -90 to 90 or a longitude outside -180 to 180.
- `capturedAt` is written as the original date and time, in UTC.
- `software` is what made the image, such as the app's name.

Every field is optional, and metadata with none set writes nothing. `WebpMetadata` and `GpsLocation` are media-webp's, in `com.oddlyclever.media.webp`, and come with this module.

## PNG

`encodeToPng()` is encoded by Skia on the JVM, iOS and the web, and by Android's `Bitmap.compress` on Android. It writes no metadata.

## What they refuse

| Call | Result |
|---|---|
| `encodeToWebp` with `quality` outside 0 to 100 | `IllegalArgumentException` |
| `encodeToWebp` on an image wider or taller than 16,383 pixels | `IllegalArgumentException`; that is WebP's limit |
| `encodeToWebp` when libwebp refuses the image | `WebpException`, from `com.oddlyclever.media.webp` |
| `encodeToPng` when the platform's encoder refuses the image | `IllegalStateException`, saying Skia or Android could not encode it |
