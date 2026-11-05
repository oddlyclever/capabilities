# GeoJSON Comes In as Text and Is Fetched by the App

ADR-0018 · 2026-11-04 · Status: accepted
Keywords: why does GeoJsonLayer take a String, pass a JSON object, kotlinx.serialization model, depend on wayfinder from another library, H3 cells on a map, why no HTTP client in the module, RemoteGeoJsonLayer load function, API key for GeoJSON URL, why does a failed fetch not throw, silent failure, network error crashes the app, error callback

## Context

GeoJSON reaches an app from many places: a file shipped with it, a server's response, another library such as one that outlines H3 cells. If the layer took another library's model, that library or this one would have to depend on the other. A document behind a URL also needs fetching, and an app already has an HTTP client with its own headers, keys, timeouts and cache. And a server that is down, or a file that is malformed, should cost the layer, never the app.

## Decision

- **GeoJSON comes in as text.** `GeoJsonLayer` takes a `String`, and `parseGeoJsonOrNull` parses one. A library that produces GeoJSON returns text, and needs no dependency on wayfinder.
- **The app fetches.** `RemoteGeoJsonLayer` takes a `load: suspend (url) -> ByteArray?` and calls it with its URL. The module makes no request and holds no client, URL template, key or cache.
- **A failure is no layer.** Text that does not parse, a `load` that returns `null`, and a `load` that throws all draw nothing, rather than throwing into the composition. Cancellation is still passed on.

## Consequences

- Any source of GeoJSON works with no adapter, including [h3spatial](H3spatial)'s cell outlines.
- A remote layer uses the app's HTTP client, so its credentials, caching, retries and timeouts are the app's to set, in `load`.
- A network failure costs the layer, never the app, and `load` needs no catch of its own.
- Failures are silent. There is no error callback: an app that wants to know why a layer is missing logs inside `load`, or calls map-view's `parseGeoJson`, which throws with the reason.
- A document already parsed by another JSON library is encoded to text before it is drawn, and parsed again.
- Before 0.0.7, a `load` that threw escaped the layer and crashed the app. Since then it draws nothing, as one that returns `null` does.
