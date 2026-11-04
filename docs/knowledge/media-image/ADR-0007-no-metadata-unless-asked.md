# No Metadata Unless Asked

ADR-0007 · 2026-11-04 · Status: accepted
Keywords: strip EXIF by default, geotag privacy, photo location leaked in upload, why is the GPS gone, keepMetadata false, copy all EXIF, keep camera model, capture time without time zone, guessing the time zone, DateTimeOriginal with no offset, orientation tag dropped

## Context

A photo from a phone carries EXIF: the camera and its settings, the orientation, when it was taken, and often where. Most images an app compresses are uploaded to be seen by other people, and a location in a shared photo is personal. Some apps do want the location and time kept with the image. And the capture time a camera writes is not always a moment: a date and time with no offset from UTC means a different instant in every time zone.

## Decision

- **The result carries no metadata by default.** None of the source's EXIF is copied unless the caller passes `keepMetadata = true`.
- **With `keepMetadata = true`, only the location and the capture time are kept,** written as EXIF in every format the module writes. The camera, its settings and the orientation are not, the orientation because the pixels are already turned upright.
- **A capture time is kept only when the source records the moment exactly:** an original date and time with its offset, or a GPS date and time stamp. A local time with no offset is left out rather than guessed.

## Consequences

- An app that uploads photos does not leak where they were taken unless it asks to.
- An app that wants the location and time asks for them with one flag, and gets the same tags in WebP, JPEG and PNG.
- A source whose camera writes only a local time gives a result with a location but no time.
- There is no way through this module to keep other EXIF tags, or to write metadata of one's own; the latter is media-webp's `WebpMetadata`.
