# h3spatial

The full documentation of [`h3spatial`](https://github.com/oddlyclever/capabilities/blob/HEAD/h3spatial/README.md): Uber's H3 hexagonal grid behind one `String`-cell API, with the same answers on every target. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [Cells, Coordinates and GeoJSON](H3spatial-Cells-Coordinates-And-Geojson): canonical cell strings, rounded coordinates, resolution, traversal and hierarchy at the edges, boundary vertex counts, the GeoJSON output, and what the service does not do.
- [Errors and What Each Call Refuses](H3spatial-Errors-And-What-Each-Call-Refuses): `H3SpatialException` and its codes, `isValidCell`, the failures that are an `Error` instead, and calling from Swift.
- [Platform Notes](H3spatial-Platform-Notes): what backs each target and arrives with the dependency, Android's ABIs and keep rules, iOS, the web, the JVM, and wiring.

## Maintaining it

- [Keeping Every Target on One H3 Release](H3spatial-Keeping-Every-Target-On-One-H3-Release): where the H3 version is pinned, why it is held at 4.5.0, and the scripts that re-vendor and rebuild the C library.
- [Adding a Function and Verifying It](H3spatial-Adding-A-Function-And-Verifying-It): the steps to add a call on every target, and the two test suites that run on each.

## Why it is built this way

- [Uber's Own H3 on Every Target, at One Release](H3spatial-Ubers-Own-H3-On-Every-Target-At-One-Release): why every target binds Uber's H3 instead of a reimplementation, and why all of them move together.
- [One Cell Vocabulary and One Failure Type on Every Target](H3spatial-One-Cell-Vocabulary-And-One-Failure-Type-On-Every-Target): why cells are canonical strings, coordinates are rounded, failures are one type, and the service ships no DI module.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
