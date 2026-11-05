# Writing Metadata

DOC-0017 · 2026-11-04
Keywords: WebP EXIF, geotag a WebP, GPS latitude longitude altitude in WebP, capture date and time, DateTimeOriginal, OffsetTimeOriginal, software tag, WebpMetadata, GpsLocation, withWebpMetadata, strip EXIF from WebP, remove geotag, toExif, EXIF for JPEG APP1, PNG eXIf chunk, VP8X extended header, Exif\0\0 prefix, XMP, ExifInterface, kotlinx.datetime Instant, time zone not recorded, custom EXIF tags, mux library

`WebpMetadata(location, capturedAt, software)` is written into a WebP as an EXIF chunk. This page covers which tags each field becomes, how the file around them changes, and the two other ways to use the same tags.

## The fields

| Field | EXIF tags written |
|---|---|
| `location: GpsLocation(latitude, longitude, altitude?)` | GPS version 2.3, latitude and longitude with their N/S and E/W references, to a ten-thousandth of a second of arc, about 3 mm; altitude above or below sea level, to a millimetre, when given |
| `capturedAt: Instant` | the original date and time in UTC, an offset of `+00:00`, and the milliseconds as sub-second time, in an Exif sub-IFD of version 0232; with a location, the GPS date and time stamp too |
| `software: String` | the software tag |

- **Only the fields that are set are written,** so a reader finds nothing it would have to second-guess. An empty `software` string counts as unset, and `WebpMetadata()` with nothing set writes nothing.
- **There is no way to write other tags.** There is no map of tag names and no `ExifInterface`; the three fields are the whole surface.
- **`capturedAt` is `kotlin.time.Instant`,** not `kotlinx.datetime.Instant`. It is always written in UTC; the photographer's local time zone is not recorded.
- **`GpsLocation` validates on construction.** A latitude outside -90..90, a longitude outside -180..180, `NaN` in either, or an altitude that is not finite throws `IllegalArgumentException`. Latitude is degrees north and longitude degrees east, so the southern and western hemispheres are negative numbers, not a separate reference letter. It is WGS 84, the datum GPS and EXIF use.

## In the file

A plain WebP, with one `VP8 ` or `VP8L` chunk, has no room for metadata. A file that gains EXIF becomes an extended WebP, as the WebP container specification lays it out: a `VP8X` header first, carrying the canvas size and the alpha and EXIF flags, then the image data, then the `EXIF` chunk, before any `XMP ` chunk. The EXIF payload starts with its TIFF header, with no `Exif\0\0` prefix, as the specification has it. The tests check every tag by reading it back.

This is written in common Kotlin, so every target writes the same bytes. Why it is not done by libwebp's own container library is in [Metadata Written in Kotlin](Media-Webp-Metadata-Written-In-Kotlin).

## Metadata on a WebP from somewhere else

`webp.withWebpMetadata(metadata)` rewrites only the container, so it adds metadata to a WebP from a server, from Skia, or from a camera, without re-encoding the image.

- **It replaces the whole EXIF chunk.** The pixels and any XMP are kept, but the file's own EXIF tags, such as a camera's model or orientation, are dropped, and only the fields of the new `WebpMetadata` remain.
- **`WebpMetadata()` with nothing set removes the EXIF** and clears its flag. On a plain WebP with no EXIF it returns the bytes unchanged.
- **Bytes that are not a WebP throw `WebpException`,** as does a file whose first chunk is not image data or whose chunks run past its end.

## The same tags for another format

`metadata.toExif()` gives the same tags as a bare EXIF block: the TIFF structure, little-endian, with no `Exif\0\0` prefix. It is for writing the tags into another container, such as a JPEG's APP1 segment, which wants the prefix in front of it, or a PNG's `eXIf` chunk. Empty metadata gives a block with no tags. [media-image](Media-Image) uses it to keep a photo's location and capture time in every format it writes.
