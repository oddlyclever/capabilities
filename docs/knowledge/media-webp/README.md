# media-webp

The full documentation of [`media-webp`](../../../media-webp/README.md): WebP encoding, lossy or lossless, with GPS position and capture time in the file, and libwebp built in on Android and iOS. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [Encoding a WebP](DOC-0016-encoding-a-webp.md): every call, raw pixels and straight alpha, quality and lossless, how each target encodes, what throws, and threading.
- [Writing Metadata](DOC-0017-writing-metadata.md): the EXIF tags each field becomes, the extended file layout, rewriting the metadata of an existing WebP, and the same tags for JPEG and PNG.
- [Platform Notes](DOC-0018-platform-notes.md): Android ABIs, page size and R8, iOS linking, Skia on the JVM and the web, and the licences in the published artifacts.
- [The Vendored libwebp](DOC-0019-the-vendored-libwebp.md): maintainer notes on the vendored source, rebuilding the native libraries, the JNI binding and the Skiko version.

## Why it is built this way

- [libwebp Built In on Android and iOS](ADR-0010-libwebp-built-in-on-android-and-ios.md): why Android and iOS carry their own libwebp, why the JVM and the web use Skia's, and why lossless there is written in Kotlin.
- [Metadata Written in Kotlin](ADR-0011-metadata-written-in-kotlin.md): why EXIF and the container are written in common code rather than by libwebp's mux library.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
