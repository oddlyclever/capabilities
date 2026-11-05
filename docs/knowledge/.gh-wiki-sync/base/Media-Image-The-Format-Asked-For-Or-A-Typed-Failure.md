# The Format Asked For, or a Typed Failure

ADR-0008 · 2026-11-04 · Status: accepted
Keywords: wrong content type, image/png reported as image/webp, toBlob falls back to PNG, silent format substitution, why Result not exception, typed failures, ImageProcessingException, when to retry another format, fallback chain, compressImageWithFallback, JPEG with transparency flattened, black background on transparent JPEG

## Context

An app that compresses an image stores or sends it with a content type, and the content type has to match the bytes. Encoders do not always produce what they were asked for: a browser canvas's `toBlob` silently writes PNG when it cannot write the requested type. JPEG cannot hold transparency, so encoding a transparent image as JPEG anyway silently flattens it. And the caller's next step depends on why a compression failed: a source that cannot be read will fail in every format, while a format the platform or the image cannot take is worth replacing with another.

## Decision

- **`compressImage` encodes exactly the format asked for, or fails.** It never silently substitutes another. Where an encoder may substitute, as `toBlob` does, the type that came back is checked, and a mismatch is a failure.
- **`ProcessedImage.contentType` reports the format actually produced,** and callers store that rather than inferring the format from the request or the bytes.
- **Failures are returned in a `Result`, as a sealed `ImageProcessingException`,** whose type says what happened: the source could not be read, it is not an image, the image has transparency the format cannot hold, the platform cannot write the format, or the encoder failed.
- **Transparency is checked pixel by pixel** before JPEG is refused, since an alpha channel is often present but unused.
- **Falling back is a separate, opt-in call,** `compressImageWithFallback`, which acts on those types: it skips a format the platform or the image cannot take, and stops at once on a source that cannot be read or decoded.

## Consequences

- A stored content type always matches the bytes, even after a fallback.
- A transparent image is never flattened into a JPEG without the caller knowing.
- A caller can write its own retry policy from the failure type, or take the fallback's.
- Failures do not throw, so a `try`/`catch` around the call catches nothing useful; the caller has to read the `Result`.
