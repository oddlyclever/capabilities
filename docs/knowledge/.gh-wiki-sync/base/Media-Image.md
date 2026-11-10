# media-image

The full documentation of [`media-image`](https://github.com/oddlyclever/capabilities/blob/HEAD/media-image/README.md): images downsized and encoded for an upload or a file, as WebP, JPEG or PNG, stood upright, with the same call on every target. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [Compressing an Image](Media-Image-Compressing-An-Image): every parameter, what `uri` may be on each target, what the result promises, the typed failures, and the fallback.
- [Keeping Location and Capture Time](Media-Image-Keeping-Location-And-Capture-Time): why nothing is kept by default, what `keepMetadata` keeps, where it is written in each format, and how the source's EXIF is read.
- [How Each Target Works](Media-Image-How-Each-Target-Works): the decoder and encoders on Android, iOS, the JVM and the web, what each needs from the app, and where they differ.

## Why it is built this way

- [No Metadata Unless Asked](Media-Image-No-Metadata-Unless-Asked): why EXIF is stripped by default, and why only location and an exact capture time are kept.
- [The Format Asked For, or a Typed Failure](Media-Image-The-Format-Asked-For-Or-A-Typed-Failure): why the call never substitutes a format, why failures are typed and returned in a `Result`, and why the fallback is separate.
- [WebP Through media-webp on Every Target](Media-Image-Webp-Through-Media-Webp-On-Every-Target): why WebP does not use each platform's own encoder.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
