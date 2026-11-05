# Errors and What Each Call Refuses

DOC-0005 · 2026-11-04
Keywords: H3SpatialException, H3 error code, H3Exception, E_RES_DOMAIN, E_CELL_INVALID, E_LATLNG_DOMAIN, resolution out of range, Not a valid H3 cell, Not an H3 index, Latitude or longitude out of range, Cells are at different resolutions, k out of range, too many steps finer than the cell to list, sentinel values h3-js, NaN coordinates, UnsatisfiedLinkError, LinkageError, Could not load the H3 native library, catch Throwable, isValidCell never throws, Swift throws, Kotlin/Native terminates the app

Every function except `isValidCell` throws one type, `H3SpatialException`, on invalid input, on every target. This page lists what each call refuses and the code it carries, and the failures that are deliberately not an `H3SpatialException`.

## One type, H3's own codes

Each platform binds H3 differently and fails differently: a Java exception on the JVM and Android, a C error code on iOS, a JavaScript throw on the web, and in `h3-js`'s case sentinel values that do not fail at all. The service validates its inputs in shared code before any platform call, so every target rejects the same inputs, with the same code, and one `catch (e: H3SpatialException)` covers all of them.

`code` is H3's own core error code, 1 to 15, as [H3 documents them](https://h3geo.org/docs/library/errors). It is `null` when the failure did not come from H3's core library and the platform supplied no code.

| Code | H3 name | Thrown when |
|---|---|---|
| `2` | `E_DOMAIN` | `gridDisk`'s `k` is negative, or its disk could not fit in a list. |
| `3` | `E_LATLNG_DOMAIN` | `latLngToH3`'s latitude is outside -90..90 or its longitude outside -180..180, including `NaN`. |
| `4` | `E_RES_DOMAIN` | A resolution is outside 0..15, `cellToParent` is given a finer resolution than the cell, or `cellToChildren` a coarser one. |
| `5` | `E_CELL_INVALID` | The text is not an H3 index, or is a number that is not a valid cell. |
| `12` | `E_RES_MISMATCH` | `gridDistance` or `areNeighborCells` is given cells at different resolutions. |
| `14` | `E_MEMORY_BOUNDS` | `cellToChildren` is asked for more than `MAX_CHILD_RESOLUTION_STEPS` (11) resolutions finer. |

Every function that takes a cell validates it first, so an invalid cell is code `5` from any of them. `cellsToGeoJson` validates each cell in turn and throws at the first invalid one.

## Failures only H3 can detect

Some inputs pass validation and still fail inside H3. `gridDistance` between two valid cells at one resolution can fail where H3 cannot measure between them, such as across a pentagon's distortion. Those failures are translated into `H3SpatialException` too, carrying the code the platform reported, or `null` when it reported none. On the JVM and Android the code comes from `com.uber:h3`'s `H3Exception`, on iOS from the C library's `H3Error`, and on the web from `h3-js`'s error: its `code` property on JS, and the `(code: N)` at the end of its message on wasmJs.

## Why out-of-range coordinates throw

The H3 C library silently normalises out-of-range coordinates. This library throws instead, because a valid-looking cell in the wrong place is worse than an error. A longitude from a map scrolled past the antimeridian has to be wrapped into -180..180 by the caller.

## `isValidCell` never throws for bad input

`isValidCell` is the designated no-throw check: it returns `false` for anything that is not a valid cell, including text that is not hex at all, has a sign or a `0x` prefix, or is longer than 16 digits. Use it on an index from outside before passing the index on.

## A broken runtime is an `Error`, not a bad index

A failure that is not about the input passes through untranslated, as a `java.lang.Error` or Kotlin `Error`: a native library that fails to load, or running out of memory. `isValidCell` lets these through as well, rather than answering `false`.

- On Android, H3's native code is there for `arm64-v8a` and `armeabi-v7a` only, so on an x86 or x86_64 emulator, and in an Android unit test on the host, the first call throws `UnsatisfiedLinkError`. See [Platform Notes](DOC-0006-platform-notes.md).
- On the JVM, a native library that cannot be loaded throws `LinkageError` with the message "Could not load the H3 native library".

`catch (e: H3SpatialException)` does not hide these, by design. Do not widen the catch to `Throwable` to make a broken runtime look like a bad index.

## From Swift

Every function that can fail is annotated `@Throws(H3SpatialException::class)`. Without the annotation, Kotlin/Native does not bridge the exception to Swift at all, and invalid input terminates the app. A function in your own shared module that calls one of these and lets the exception escape to Swift needs the same annotation. [Platform Notes](DOC-0006-platform-notes.md) has the rest of the iOS detail.
