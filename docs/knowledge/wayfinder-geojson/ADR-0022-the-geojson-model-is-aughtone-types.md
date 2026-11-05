# The GeoJSON Model Is aughtone-types'

ADR-0022 · 2026-11-05 · Status: accepted
Keywords: map-view-geojson dropped, FeatureCollection vs GeoFeatureCollection, which GeoJSON model, kotlinx.serialization GeoJSON, encode GeoJSON, properties as JsonObject, RFC 7946 validation, ring not closed, own parser vs serialization, copy map-view-geojson, Apache-2.0 derived code, GeoJsonLayer overload

## Context

The layers drew through map-view's map-view-geojson, and its model was the module's public vocabulary: `FeatureCollection`, `Feature`, `Geometry`, and a hand-written parser. The ornament controls and the tile cache had already moved here from map-view, which is dropping them, and the GeoJSON drawing followed, so that the wayfinder family depends on map-view for the map alone.

That left the model. Copying map-view's would have kept apps' code unchanged, but it is a model of its own that nothing else speaks: it cannot be encoded back to GeoJSON, its `properties` lose the difference between a whole number and a fraction, and its parser accepts documents RFC 7946 does not, such as a ring left open. aughtone-types already has a GeoJSON model, which wayfinder depends on for its locale anyway: `@Serializable`, with properties as a `JsonObject` and typed readers for them, and validation of lines and rings as the specification defines them.

## Decision

- **The drawing is a copy of map-view-geojson 0.3.0,** under its Apache-2.0 licence, with each file's header saying so and the module README naming the origin. It draws and hit-tests aughtone-types' geometries in place of map-view's.
- **The model is aughtone-types'.** The layers take and give back `GeoFeatureCollection` and `GeoFeature`, and the module exposes aughtone-types as `api`. `GeoJsonStyle` and `GeoJsonProjection` stay the module's own, in `com.oddlyclever.wayfinder.geojson`.
- **Text is parsed by kotlinx.serialization** into that model, skipping members the specification does not define. `parseGeoJson` throws with the reason, and `parseGeoJsonOrNull` and the layers turn a failure into no layer, as before.
- **A parsed collection has a layer of its own.** `GeoJsonLayer` takes a `GeoFeatureCollection` beside the one that takes text, in place of map-view's `GeoJson` composable.

## Consequences

- The module no longer depends on map-view-geojson; it depends on map-view's map, as wayfinder does, and on aughtone-types and kotlinx.serialization.
- Source-incompatible for an app that named map-view's GeoJSON types: `FeatureCollection` becomes `GeoFeatureCollection`, `Feature` becomes `GeoFeature`, `GeoJsonStyle` moves package, `GeoJson(data)` becomes `GeoJsonLayer(data)`, and properties are read with `stringProperty` and its siblings rather than out of a `Map<String, Any?>`.
- Stricter: a line of one position, or a ring that is not closed or has fewer than four positions, fails the document, so a file some tools write that map-view drew now draws nothing.
- A collection built or decoded by the app is drawn without being written to text and parsed again, and one parsed here can be encoded back to GeoJSON.
- The drawing and hit-testing are this repository's to maintain: a fix map-view makes to its own copy no longer arrives with a version bump.
