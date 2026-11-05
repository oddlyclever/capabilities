# wayfinder-geojson

The full documentation of [`wayfinder-geojson`](../../../wayfinder-geojson/README.md): GeoJSON on a wayfinder map, from text, a URL or aughtone-types' GeoJSON model, drawn by the module's own copy of map-view's GeoJSON drawing. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [Drawing GeoJSON on a Map](DOC-0036-drawing-geojson-on-a-map.md): where the layers are drawn, styles and why two looks are two layers, hiding a layer by zoom, how taps are decided, and attribution.
- [Parsing and Loading GeoJSON](DOC-0037-parsing-and-loading-geojson.md): what the parser reads and refuses, longitude-first positions, aughtone-types' model and how properties and ids are read, why a layer shows nothing, parsing off the main thread, and how a remote layer fetches.

## Why it is built this way

- [GeoJSON Comes In as Text and Is Fetched by the App](ADR-0018-geojson-comes-in-as-text-and-is-fetched-by-the-app.md): why the layers take text and the app's load function, and why a failure draws nothing instead of throwing.
- [The GeoJSON Model Is aughtone-types'](ADR-0022-the-geojson-model-is-aughtone-types.md): why the drawing is a copy of map-view-geojson and the model aughtone-types', and what that changed for apps.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
