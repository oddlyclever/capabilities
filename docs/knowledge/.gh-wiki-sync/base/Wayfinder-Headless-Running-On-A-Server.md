# Running on a Server

DOC-0001 · 2026-11-04
Keywords: UnsatisfiedLinkError, LibraryLoadException, libGL.so.1 missing, Docker image for map rendering, Skia on Linux, no display, headless server, Windows not supported, Alpine, fonts missing, names not drawn, skiko.data.path, read-only filesystem, UnsupportedClassVersionError

`wayfinder-headless` draws through Skia, a native library, so a server needs more than the jar. This page is everything the machine has to provide, and what each failure looks like when it doesn't.

## What the module brings

Skia's native library comes with the module, as Skiko's `skiko-awt-runtime` artifacts for four platforms:

| Operating system | Architectures |
|---|---|
| Linux | x64, arm64 |
| macOS | x64, arm64 |

Linux is there for servers and containers, macOS for running the same code on a developer's machine. All four are on the runtime classpath of every app that depends on the module, and Skiko picks the one that matches the machine the first time something is drawn. Nothing has to be configured.

There is no Windows library, and nothing for any other operating system or architecture. On Windows the first render fails with Skiko's `LibraryLoadException`, saying the native dependency is missing. Anywhere else Skiko fails with an `Unknown OS` or `Unknown arch` error, which is a `java.lang.Error` rather than an exception, so a `catch (e: Exception)` around the call does not catch it.

## What the machine has to provide

- **Java 21 or later.** map-view's classes are compiled for it, so an older JVM fails at class loading with `UnsupportedClassVersionError`.
- **The system libraries Skia links against**, on Linux: `libGL.so.1`, `libX11.so.6` and `libEGL.so.1`. They are needed even though nothing is shown and no display exists: Skia's library is linked against them and will not load without them. A missing one fails the first render with a `LibraryLoadException` that names it. On Debian and Ubuntu the packages are `libgl1`, `libx11-6` and `libegl1`.
- **A font**, if names are drawn. Place and road names are drawn in the machine's default font, and a slim container image often has none. `fonts-dejavu-core` is enough. With `names = false` no font is needed.
- **A writable directory for Skia's library.** Skiko unpacks the native library from its jar on first use, into `~/.skiko/` by default. On a read-only filesystem, or for a user with no writable home, point it elsewhere with the `skiko.data.path` system property: `-Dskiko.data.path=/tmp/skiko`.

## A container image

On a Debian or Ubuntu based image:

```dockerfile
FROM eclipse-temurin:21-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends libgl1 libx11-6 libegl1 fonts-dejavu-core \
 && rm -rf /var/lib/apt/lists/*
```

Skia's Linux library is built against glibc. An Alpine image uses musl instead, and the library does not load there; use a Debian, Ubuntu or other glibc-based image.

## When it fails

| What you see | Why |
|---|---|
| `LibraryLoadException` naming `libGL.so.1`, `libX11.so.6` or `libEGL.so.1` | A system library is missing. Install the packages above. |
| `LibraryLoadException` saying the native dependency is missing, on Windows | There is no Windows library. Run it on Linux or macOS. |
| An `Error` mentioning `Unknown OS` or `Unknown arch` | The machine is none of the four platforms. |
| `UnsupportedClassVersionError` | The JVM is older than 21. |
| An error writing to `~/.skiko` | The home directory is not writable. Set `skiko.data.path`. |
| The map draws but has no names | No font is installed. |
| The map draws only the route, on a plain background | The tiles failed, not the machine. See [Tile Sources](Wayfinder-Headless-Tile-Sources). |

## Other Compose code in the same process

The module's Skia libraries are the version Compose Multiplatform 1.12.2 brings, Skiko 0.150.2. A server that also uses Compose for desktop, or adds its own `skiko-awt-runtime` artifact, has to bring the same Skiko version: two versions of Skia's library on one classpath leave Gradle to pick one, and the classes and the native library it loads can then disagree.

## Why it is built this way

Why the drawing goes through Skia, and why the module is JVM only, is in [Skia on the JVM for Server-Side Map Images](Wayfinder-Headless-Skia-On-The-Jvm-For-Server-Side-Map-Images).
