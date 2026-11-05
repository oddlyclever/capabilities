# Drawing GeoJSON on a Map

DOC-0036 · 2026-11-04
Keywords: GeoJsonLayer, RemoteGeoJsonLayer, GeoJson composable, GeoFeatureCollection, aughtone-types, draw polygons on a map, coverage area, delivery zone, geofence, boundary, trails, H3 hexagons on a map, GeoJsonStyle, fill colour, stroke width, point radius, style by property, data-driven styling, minZoom, hide layer when zoomed out, tap a feature, onFeatureClick, touchWidth, feature under the marker, layer order, polygon holes, antimeridian, attribution for GeoJSON data, addLayerToMap, GeoJSON on a map image

`GeoJsonLayer` and `RemoteGeoJsonLayer` draw a GeoJSON document's points, lines and polygons on a `WayfinderMap`. This page covers where they are drawn, how they look, hiding them by zoom, and taps. Parsing and fetching are in [Parsing and Loading GeoJSON](DOC-0037-parsing-and-loading-geojson.md).

## The call

Both are composables in a `WayfinderMap`'s trailing `overlay` lambda. `GeoJsonLayer` takes the document as text, or already parsed into aughtone-types' `GeoFeatureCollection`:

```kotlin
WayfinderMap(state, layers = listOf(basemap)) {
    GeoJsonLayer(coverageText, style = GeoJsonStyle(fill = Color(0x330066FF), stroke = Color(0xFF0066FF)))
    RemoteGeoJsonLayer(
        url = "https://data.example.com/trails.geojson",
        load = { url -> http.get(url).body<ByteArray>() },
        minZoom = 12f,
        onFeatureClick = { feature -> select(feature.stringProperty("name")) },
    )
}
```

| Parameter | Default | What it does |
|---|---|---|
| `geoJson` or `data` (`GeoJsonLayer`) | required | The document, as text or as a `GeoFeatureCollection`. |
| `url`, `load` (`RemoteGeoJsonLayer`) | required | Where the document is, and the app's function that fetches it. |
| `style` | `GeoJsonStyle()` | One style for every polygon, line and point in the layer. |
| `minZoom` | `0f` | Below this zoom, the layer is neither drawn nor tappable. |
| `touchWidth` | `24.dp` | The width of the band around a line or point that a tap must land in. |
| `onFeatureClick` | `null` | Gets the feature tapped. With `null`, the layer takes no taps. |

There is no layer object to add to a map and no resource id: a layer is a composable in the overlay, and it takes a `String`, a URL or a `GeoFeatureCollection`.

## Where it is drawn

The overlay is drawn over the map's tiles and polylines and under its markers. Layers in the overlay are drawn in the order they are declared, so a layer declared later covers one declared earlier.

The shapes are projected once, when the document is first drawn, and kept for as long as it is the same. Only the camera's transform runs on each frame, so the layer follows the map as it pans and pinches without recomposing.

Every geometry type is drawn: points, lines, polygons, their multi-part forms, and geometry collections. Within a layer, polygons are drawn first, then lines, then points, so a point is never lost under the shape it sits on. A polygon's holes are holes, and two features whose polygons overlap are both filled. Lines and polygon edges cross the antimeridian the short way, so a shape across it stays in one piece.

A feature whose `geometry` is `null` draws nothing.

## Style

`GeoJsonStyle(fill, stroke, strokeWidth, pointColor, pointRadius)` applies to every feature in the layer:

| Field | Default | What it sets |
|---|---|---|
| `fill` | `Color(0x330066FF)` | The inside of a polygon, holes excepted. |
| `stroke` | `Color(0xFF0066FF)` | A polygon's outline, and a line. |
| `strokeWidth` | `2.dp` | The width of that outline and of a line. |
| `pointColor` | `stroke` | A point is a dot of this colour. |
| `pointRadius` | `4.dp` | And this big. |

Widths are in dp, so they stay the same on screen at every zoom. A transparent colour or a width of 0 leaves that part out: `fill = Color.Transparent` draws outlines only.

There is no styling by property. Two looks are two layers: parse the document once, split its features, and draw each part with the `GeoJsonLayer` that takes a `GeoFeatureCollection`:

```kotlin
val parsed by produceState<GeoFeatureCollection?>(null, text) {
    value = withContext(Dispatchers.Default) { parseGeoJsonOrNull(text) }
}
val (open, closed) = remember(parsed) {
    parsed?.features.orEmpty().partition { it.stringProperty("status") == "open" }
        .let { (a, b) -> GeoFeatureCollection(a) to GeoFeatureCollection(b) }
}
WayfinderMap(state, layers = listOf(basemap)) {
    GeoJsonLayer(open, style = GeoJsonStyle(stroke = Color(0xFF16A34A)))
    GeoJsonLayer(closed, style = GeoJsonStyle(stroke = Color(0xFFDC2626)))
}
```

`GeoJsonStyle` is this module's, in `com.oddlyclever.wayfinder.geojson`. `GeoFeatureCollection`, `GeoFeature` and `stringProperty` are aughtone-types', in `io.github.aughtone.types.geo`, and come with this module.

## Hiding a layer by zoom

Below `minZoom`, a layer is not drawn and cannot be tapped. At `minZoom` exactly, it is shown. Use it to keep a dense layer from covering a zoomed-out map.

Use `minZoom` rather than an `if` around the layer. A `RemoteGeoJsonLayer` fetches again whenever it leaves the composition and comes back, so one wrapped in `if (state.zoom > 12f)` fetches again every time the user zooms across 12. With `minZoom` it stays in the composition and keeps what it fetched.

## Taps

With `onFeatureClick` set, a tap on a feature calls it with that `GeoFeature`, whose `properties` and `id` are the document's. How their values are typed is in [Parsing and Loading GeoJSON](DOC-0037-parsing-and-loading-geojson.md).

- **A tap anywhere inside a polygon counts.** A tap in one of its holes does not.
- **A tap near a line or point counts within half of `touchWidth`** of it: 12 dp either side with the default 24 dp.
- **The nearest feature takes the tap, not the top one.** That holds across every layer and every tappable shape in the overlay, polylines included. Between features of one layer that are equally near, the one drawn on top wins.
- **A marker keeps its own taps.** A tap that a marker takes does not reach the layer.
- **A tap that misses** every feature goes on to the map underneath.
- **A drag that starts on a feature still pans the map.**

## Attribution

The map's credits ornament shows the sources of its tile layers only. GeoJSON layers add nothing to it, so data whose licence asks for a credit needs one the app draws itself.

## Live maps only

The layers draw on a live `WayfinderMap`. For drawing without a composition, `drawGeoJson(data, style, projection, minZoom)` draws a `GeoFeatureCollection` into any `DrawScope` through a `GeoJsonProjection`, the zoom and the projection a map image has. It does not yet draw on a map image from [wayfinder-frame](../wayfinder-frame/README.md), whose `MapFrameProjection` does not implement `GeoJsonProjection`.
