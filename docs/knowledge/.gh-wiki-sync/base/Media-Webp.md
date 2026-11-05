# media-webp

The full documentation of [`media-webp`](https://github.com/oddlyclever/capabilities/blob/HEAD/media-webp/README.md): WebP encoding, lossy or lossless, with GPS position and capture time in the file, and libwebp built in on Android and iOS. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [Encoding a WebP](Media-Webp-Encoding-A-Webp): every call, raw pixels and straight alpha, quality and lossless, how each target encodes, what throws, and threading.
- [Writing Metadata](Media-Webp-Writing-Metadata): the EXIF tags each field becomes, the extended file layout, rewriting the metadata of an existing WebP, and the same tags for JPEG and PNG.
- [Platform Notes](Media-Webp-Platform-Notes): Android ABIs, page size and R8, iOS linking, Skia on the JVM and the web, and the licences in the published artifacts.
- [The Vendored libwebp](Media-Webp-The-Vendored-Libwebp): maintainer notes on the vendored source, rebuilding the native libraries, the JNI binding and the Skiko version.

## Why it is built this way

- [libwebp Built In on Android and iOS](Media-Webp-Libwebp-Built-In-On-Android-And-Ios): why Android and iOS carry their own libwebp, why the JVM and the web use Skia's, and why lossless there is written in Kotlin.
- [Metadata Written in Kotlin](Media-Webp-Metadata-Written-In-Kotlin): why EXIF and the container are written in common code rather than by libwebp's mux library.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
