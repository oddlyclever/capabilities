# wayfinder-geojson

The full documentation of [`wayfinder-geojson`](https://github.com/oddlyclever/capabilities/blob/HEAD/wayfinder-geojson/README.md): GeoJSON on a wayfinder map, from text or a URL, drawn by map-view's GeoJSON support. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [Drawing GeoJSON on a Map](Wayfinder-Geojson-Drawing-Geojson-On-A-Map): where the layers are drawn, styles and why two looks are two layers, hiding a layer by zoom, how taps are decided, and attribution.
- [Parsing and Loading GeoJSON](Wayfinder-Geojson-Parsing-And-Loading-Geojson): what the parser reads, longitude-first positions, how properties and ids are typed, why a layer shows nothing, parsing off the main thread, and how a remote layer fetches.

## Why it is built this way

- [GeoJSON Comes In as Text and Is Fetched by the App](Wayfinder-Geojson-Geojson-Comes-In-As-Text-And-Is-Fetched-By-The-App): why the layers take text and the app's load function, and why a failure draws nothing instead of throwing.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
