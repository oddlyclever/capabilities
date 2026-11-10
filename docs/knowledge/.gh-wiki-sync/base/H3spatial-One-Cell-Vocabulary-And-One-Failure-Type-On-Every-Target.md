# One Cell Vocabulary and One Failure Type on Every Target

ADR-0003 · 2026-11-04 · Status: accepted
Keywords: String cell index, Long index, H3Core per platform, expect class per platform, canonical H3 string, rounding to eight decimal places, coordinates differ in the last bits, H3Exception, platform exceptions, h3-js sentinel values, silent normalisation of coordinates, default resolution, GeoJSON model dependency, GeoJSON as text, Koin module, dependency injection module, singleton or factory

## Context

The module binds a different H3 on every target ([Uber's Own H3 on Every Target, at One Release](H3spatial-Ubers-Own-H3-On-Every-Target-At-One-Release)), and each binding has its own types and its own idea of failure: a Java exception, a C error code, a JavaScript throw, and in `h3-js`'s case sentinel values that do not fail at all. Each platform's hex parser accepts a different set of index variants, and each computes coordinates in its own floating-point environment. Exposed as they are, the same input would give a different type, a different text, slightly different coordinates and a different failure on each target, and a consumer's positional logic would have to live per platform.

## Decision

The service presents one vocabulary in common code, and makes every target agree on it before and after the platform call.

- **Cells are canonical `String`s**, unpadded lowercase hex, the form standard H3 tools produce. Input is parsed in shared code, hex digits only, so the same text means the same cell everywhere.
- **Coordinates are `H3LatLng` in degrees, rounded to eight decimal places,** so a centre or boundary compares equal across targets rather than differing in the last few bits.
- **One failure type,** `H3SpatialException`, carrying H3's own error code. Inputs are validated in shared code before any platform call, so every target rejects the same inputs with the same code, and a failure only H3 can detect is translated after it.
- **`isValidCell` is the one call that never throws** for bad input.
- **A broken runtime is not a bad input.** An `Error`, such as a native library that fails to load, passes through untranslated so a caller does not mistake it for an invalid index.
- **Out-of-range coordinates throw** rather than being silently normalised as the H3 C library does, because a valid-looking cell in the wrong place is worse than an error.
- **Resolution is always the caller's choice.** There is no default, because the right cell size depends entirely on what is being bucketed.
- **GeoJSON is returned as text,** so the module carries no GeoJSON model dependency, and anything that parses GeoJSON can draw it.
- **The service is a plain stateless class with no DI module.** It takes no constructor arguments and has no platform variance to resolve, so a DI module inside the library would be a pass-through that forced a container onto every consumer. Bound in a container, it is a factory: nothing is retained, nothing is shared across threads, and no caller can come to depend on instance identity. A singleton would cache an object with nothing in it, while presenting a shared dependency where none exists.

## Consequences

- A consumer's positional logic lives in shared code, and a cell, a centre or a boundary from one target compares equal with `==` to the same one from another.
- One `catch (e: H3SpatialException)` covers every target, and from Swift every throwing call imports as `throws`.
- The service has to repeat checks H3 itself would make, and keep them in step with what each binding would otherwise do.
- Input is accepted in more forms than output is given, so an index from outside has to be canonicalised before it is compared or used as a key.
- Coordinates are no finer than about a millimetre.
- The GeoJSON text writes numbers with each platform's `Double.toString`, so the same cells can produce different text on different targets; it is for parsing, not comparing as strings.
- A caller that wraps longitudes past the antimeridian, or swaps latitude and longitude within ±90, has to fix that itself: the first throws, and the second is a valid cell in the wrong place.
