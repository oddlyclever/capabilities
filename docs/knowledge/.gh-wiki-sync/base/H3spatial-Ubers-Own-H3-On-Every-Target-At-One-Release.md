# Uber's Own H3 on Every Target, at One Release

ADR-0002 · 2026-11-04 · Status: accepted
Keywords: pure Kotlin H3, reimplement H3 in Kotlin, port H3 to common code, H3 multiplatform, cells differ between platforms, cells disagree at edges, why vendor the C source, why commit libh3.a, why no NDK build, com.uber:h3-android, h3-js, cinterop, pin to a tag not master, version drift between targets, agreement tests against Uber's reference data

## Context

A spatial index is only useful if every device puts a point in the same cell. An app that computes cells on a phone, a web page and a server has to get identical answers on all of them, or records keyed by cell stop matching. H3 has no single binding that runs on every Kotlin Multiplatform target: Uber publishes the C library, a Java binding for the JVM and Android, and `h3-js` for JavaScript. A reimplementation in common Kotlin risks the worst outcome for a spatial index: cells that agree nearly everywhere, so cross-platform comparison fails only at cell edges, intermittently, in the field.

## Decision

Every target binds Uber's own H3, all of one pinned release, behind one internal seam, `H3Native`.

- **JVM:** `com.uber:h3`, which carries its native libraries in the jar.
- **Android:** `com.uber:h3-android`, Uber's own Android packaging of the same Java binding. Android does not compile H3 itself, so it needs no NDK or CMake.
- **iOS:** the H3 C library, vendored unmodified under `native/h3lib` and called through cinterop. The static libraries built from it are committed, so consumers need no CMake and no network access to an upstream checkout.
- **JS and wasmJs:** `h3-js`, as an npm dependency.
- **One release.** The vendored C, the h3-java release behind `com.uber:h3` and `com.uber:h3-android`, and `h3-js` move together or not at all. `native/PROVENANCE` records the pin, and `update-source.sh` refuses an h3-java release built against a different H3 tag.
- **A tag, not a branch.** The vendored C follows a released H3 tag, never `master`, so the pin is reproducible and iOS is never ahead of the targets that use published packages.
- **Checked against Uber.** Every target runs the same agreement tests against H3's own reference data for the vendored release, so a binding that drifts fails on the platform where it drifts.

## Consequences

- A cell computed on any target is the cell any other H3 tool computes, and agreement is tested rather than assumed.
- A binding per target to maintain instead of one implementation. Each `H3Native` is thin, and adding a call means writing it for every target, though never rebuilding anything native.
- Each platform fails in its own way, so the service has to validate inputs and translate failures in shared code to give one contract; [One Cell Vocabulary and One Failure Type on Every Target](H3spatial-One-Cell-Vocabulary-And-One-Failure-Type-On-Every-Target) covers that.
- An H3 upgrade waits for the slowest of three upstream releases. [Keeping Every Target on One H3 Release](H3spatial-Keeping-Every-Target-On-One-H3-Release) has the procedure, and why the module is held at 4.5.0.
- Android inherits `com.uber:h3-android`'s ABIs, ARM only, so H3 does not load on an x86 or x86_64 emulator.
- The repository carries the vendored C and two committed static libraries, which have to be rebuilt and committed together with any change to the source.
- Apps that ship the module ship Uber's H3, under its Apache License, on every target. The module's `NOTICE` and `LICENSE` travel in every published jar.
