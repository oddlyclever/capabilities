# Metadata Written in Kotlin

ADR-0011 · 2026-11-04 · Status: accepted
Keywords: libwebp mux library, WebPMux, demux, why not use mux, EXIF on the JVM and the web, Skia has no mux, write EXIF without a library, same metadata on every target, VP8X, add EXIF to an existing WebP, GpsLocation instead of a geometry library

## Context

The images an app saves, photos and map images, need to keep where and when they were made. libwebp writes metadata through its container library, mux, but only Android and iOS encode with a libwebp of this module's own. The JVM and the web encode with Skia's libwebp, which has no mux, so metadata written by mux would exist on two targets and not the other three.

## Decision

- **The EXIF block and the WebP container around it are written in common Kotlin,** following the WebP container specification, and libwebp's mux and demux libraries are not vendored.
- **Metadata is applied after encoding,** by rewriting the RIFF container, so the same code adds metadata to any WebP: one this module encoded, or one from Skia, a server or a camera (`withWebpMetadata`).
- **The fields are fixed:** location, capture time and software, the ones an app that saves photos and map images needs. Only the fields that are set are written.
- **`GpsLocation` is this module's own type,** rather than one borrowed from a geometry library, so writing a WebP needs no other dependency.

## Consequences

- Every target writes the same metadata, byte for byte.
- The same tags are available as a bare EXIF block, `toExif()`, which media-image writes into JPEG and PNG.
- Metadata can be added to, or removed from, a WebP from any encoder without re-encoding its pixels.
- The module owns a small EXIF writer and container rewriter, and their correctness rests on its own tests, which read every tag back.
- There is no way to write arbitrary EXIF tags, or XMP.
