# Keeping Location and Capture Time

DOC-0014 · 2026-11-04
Keywords: keepMetadata, strip EXIF, remove geotag before upload, keep GPS location, keep capture date, photo date lost, DateTimeOriginal without offset, OffsetTimeOriginal, GPS time stamp, time zone unknown, camera model not kept, EXIF in JPEG APP1, EXIF in PNG eXIf, EXIF in WebP, HEIC metadata, ExifInterface, ImageIO properties, metadata missing in the browser, custom metadata, privacy

By default a result carries no metadata. With `keepMetadata = true` it carries two things from the source, its location and its capture time, in every format. This page covers what is kept, what is left out and why, and where it is written.

## The default: nothing

```kotlin
processor.compressImage(uri)                        // no metadata
processor.compressImage(uri, keepMetadata = true)   // location and capture time
```

None of the source's EXIF is copied unless asked for. A photo's EXIF holds where it was taken, which is personal, and most images are uploaded to be seen by other people. Why this is the default is in [No Metadata Unless Asked](Media-Image-No-Metadata-Unless-Asked).

## What is kept

With `keepMetadata = true`:

- **The location:** latitude, longitude, and altitude where the source records it. A location outside the valid range of latitude or longitude is dropped rather than written.
- **The capture time, only when the source records the moment exactly.** That is the original date and time with its offset from UTC, or else the GPS date and time stamp, which is UTC by definition. A date and time with no offset names no moment, since the camera's time zone is unknown, so it is left out rather than guessed. Most phones record the offset. A camera that writes only a local time gives a result with a location but no time.

Nothing else of the source's EXIF is kept: not the camera, its settings, or the orientation, since the image is already upright. There is no parameter for writing metadata of your own; for that, encode with [media-webp](Media-Webp), whose `WebpMetadata` also takes a software name.

The kept fields are written with media-webp's EXIF writer, so they become the same tags media-webp writes: the capture time as the original date and time in UTC with a `+00:00` offset, plus the GPS date and time stamp when there is a location.

## Where it is written

Every platform's encoder writes the image with no metadata, so the EXIF this adds is the only EXIF the result carries.

| Format | Where the EXIF goes |
|---|---|
| WebP | an `EXIF` chunk, through media-webp, which makes the file an extended WebP |
| JPEG | an APP1 segment with the `Exif\0\0` prefix, straight after the start of the image, or after the JFIF header when there is one, since JFIF requires its header first |
| PNG | an `eXIf` chunk straight after the header chunk, before the image data, as the format requires |

## Where the source's EXIF is read

| Target | Read with | Sources it understands |
|---|---|---|
| Android | ExifInterface | JPEG, PNG, WebP and HEIC |
| iOS | ImageIO, from the properties it reads with the image | everything ImageIO decodes, HEIC included |
| JVM | this module's own reader | JPEG, PNG and WebP |
| JS, wasmJs | this module's own reader | JPEG, PNG and WebP |

The module's own reader takes the EXIF from a JPEG's APP1 segment, a WebP's `EXIF` chunk or a PNG's `eXIf` chunk, in either byte order, and accepts a WebP whose EXIF carries the `Exif\0\0` prefix that the specification leaves out but some writers add. It never fails a compression: a source with no EXIF, or EXIF it cannot make sense of, reads as upright with nothing recorded. On Android, EXIF that ExifInterface cannot read is treated the same way.

**In the browser, the source is fetched a second time** to read its EXIF, since the `<img>` that decodes it does not expose its bytes. That happens only when `keepMetadata` is true. If the fetch fails, the image is still produced, and the metadata is silently left out.
