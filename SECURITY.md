# Security

## Reporting a vulnerability

Report it privately, through GitHub: on this repository's **Security** tab, choose **Report a vulnerability**. Only the maintainers see the report. Please don't open a public issue or pull request for it, because that publishes the problem before a fix exists.

A useful report says:

- which module and version it affects, such as `com.oddlyclever:media-image:0.0.7`
- what an attacker could do with it, and what they need first, such as control of an image or a tile response the app loads
- how to reproduce it: a failing test, a sample input, or the steps

You'll get a reply on the report itself. When the problem is confirmed, the fix ships in a new release and the advisory is published with that release, crediting you unless you'd rather not be named.

## Supported versions

Every module is released together, at one version. Until 1.0, fixes go into the next release only, not into older versions, so the fix for a vulnerability is to update to the release that contains it.

## Vendored code

Two modules build third-party C code from source:

- `media-webp` vendors [libwebp](https://github.com/webmproject/libwebp).
- `h3spatial` vendors [H3](https://github.com/uber/h3).

If the vulnerability is in that code itself, report it to the upstream project as well, under its own security policy. Tell us too, so the vendored copy is updated in the next release. The version each module vendors is in its `native/PROVENANCE` file.
