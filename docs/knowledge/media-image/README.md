# media-image

The full documentation of [`media-image`](../../../media-image/README.md): images downsized and encoded for an upload or a file, as WebP, JPEG or PNG, stood upright, with the same call on every target. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [Compressing an Image](DOC-0013-compressing-an-image.md): every parameter, what `uri` may be on each target, what the result promises, the typed failures, and the fallback.
- [Keeping Location and Capture Time](DOC-0014-keeping-location-and-capture-time.md): why nothing is kept by default, what `keepMetadata` keeps, where it is written in each format, and how the source's EXIF is read.
- [How Each Target Works](DOC-0015-how-each-target-works.md): the decoder and encoders on Android, iOS, the JVM and the web, what each needs from the app, and where they differ.

## Why it is built this way

- [No Metadata Unless Asked](ADR-0007-no-metadata-unless-asked.md): why EXIF is stripped by default, and why only location and an exact capture time are kept.
- [The Format Asked For, or a Typed Failure](ADR-0008-the-format-asked-for-or-a-typed-failure.md): why the call never substitutes a format, why failures are typed and returned in a `Result`, and why the fallback is separate.
- [WebP Through media-webp on Every Target](ADR-0009-webp-through-media-webp-on-every-target.md): why WebP does not use each platform's own encoder.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
