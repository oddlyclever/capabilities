# WebP Through media-webp on Every Target

ADR-0009 · 2026-11-04 · Status: accepted
Keywords: Bitmap.compress WEBP varies by device, ImageIO cannot write WebP, Safari canvas cannot write WebP, toBlob image/webp, why not the platform WebP encoder, same WebP on every device, JPEG and PNG platform encoders

## Context

WebP is the default format, so it is the one that most needs to behave the same everywhere. Each platform's own WebP encoder does not: Android's output varies by release and device, iOS's ImageIO cannot be relied on to write it, Safari's canvas cannot write it at all, and the other browsers write whatever their own libwebp makes of it. [media-webp](Media-Webp) encodes WebP with the same library everywhere a target allows, with libwebp built in on Android and iOS.

## Decision

- **WebP is encoded by media-webp on every target,** from the decoded, resized pixels, not by the platform's encoder.
- **JPEG and PNG use each platform's own encoder:** `Bitmap.compress` on Android, UIKit on iOS, Skia on the JVM and the canvas in the browser.

## Consequences

- A WebP from this module does not depend on the Android release, the device maker or the browser.
- Metadata goes into a WebP through media-webp's container writer, the same on every target.
- In the browser, WebP needs Skia's WebAssembly loaded, as media-webp's lossy encoder is Skia's; before it is, WebP fails and the fallback moves on.
- On the JVM, Skia's native library is needed anyway, for decoding.
