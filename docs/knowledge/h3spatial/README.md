# h3spatial

The full documentation of [`h3spatial`](../../../h3spatial/README.md): Uber's H3 hexagonal grid behind one `String`-cell API, with the same answers on every target. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [Cells, Coordinates and GeoJSON](DOC-0004-cells-coordinates-and-geojson.md): canonical cell strings, rounded coordinates, resolution, traversal and hierarchy at the edges, boundary vertex counts, the GeoJSON output, and what the service does not do.
- [Errors and What Each Call Refuses](DOC-0005-errors-and-what-each-call-refuses.md): `H3SpatialException` and its codes, `isValidCell`, the failures that are an `Error` instead, and calling from Swift.
- [Platform Notes](DOC-0006-platform-notes.md): what backs each target and arrives with the dependency, Android's ABIs and keep rules, iOS, the web, the JVM, and wiring.

## Maintaining it

- [Keeping Every Target on One H3 Release](DOC-0007-keeping-every-target-on-one-h3-release.md): where the H3 version is pinned, why it is held at 4.5.0, and the scripts that re-vendor and rebuild the C library.
- [Adding a Function and Verifying It](DOC-0008-adding-a-function-and-verifying-it.md): the steps to add a call on every target, and the two test suites that run on each.

## Why it is built this way

- [Uber's Own H3 on Every Target, at One Release](ADR-0002-ubers-own-h3-on-every-target-at-one-release.md): why every target binds Uber's H3 instead of a reimplementation, and why all of them move together.
- [One Cell Vocabulary and One Failure Type on Every Target](ADR-0003-one-cell-vocabulary-and-one-failure-type-on-every-target.md): why cells are canonical strings, coordinates are rounded, failures are one type, and the service ships no DI module.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
