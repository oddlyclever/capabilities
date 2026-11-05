# Tile Sources and Tile Clients

DOC-0028 · 2026-11-04
Keywords: tile URL template, {z}/{x}/{y}, XYZ tiles, TMS tiles, tiles in wrong rows, flipped tiles, tile server API key, HTTP headers for tiles, Ktor client, blank map, map shows no tiles, tiles never load, tile errors swallowed, expired token, retry, tile cache, offline map, wayfinder-cache, map-view-cache, FileTileStore, MemoryTileStore, CacheMode, CacheFirst, NetworkFirst, OfflineOnly, NoCache, zoom range, overzoom, TileSource, TileClient, Tile URL template is missing, attribution missing

wayfinder fetches nothing itself. A `TileSource` describes where a layer's tiles come from, and a `TileClient` is the app's own fetching behind one call. The basemap modules take both and turn the bytes into tiles. Why the fetching is the app's is in [The App Fetches Every Tile](ADR-0014-the-app-fetches-every-tile.md).

## TileSource

```kotlin
val streets = TileSource(
    id = "streets",
    urlTemplate = "https://tiles.example.com/{z}/{x}/{y}.pbf?key=$apiKey",
    zoomRange = 0..14,
    attribution = "© Example contributors",
)
```

| Field | Default | What it is |
|---|---|---|
| `id` | required | names the source wherever tiles from different sources must not be confused, such as a cache key |
| `urlTemplate` | required | a URL with `{z}`, `{x}` and `{y}` where the tile's zoom, column and row go |
| `zoomRange` | `0..19` | the levels the source has; map-view's default range |
| `tms` | `false` | whether the source numbers rows from the bottom, as TMS sources do |
| `attribution` | null | the credit the source's licence asks to be shown with it |

`url(key)` fills in the template for a map-view `TileKey`. map-view numbers rows from the top, XYZ style, so a TMS source has its row flipped in `url`; everything else about the key is unchanged. A TMS source without `tms = true` draws its tiles in the wrong rows.

**A bad source fails when it is made, not when it draws.** Each of these throws `IllegalArgumentException` from the constructor:

| Mistake | Message |
|---|---|
| A template missing `{z}`, `{x}` or `{y}` | "Tile URL template "…" is missing {y}." |
| A blank `id` | "A tile source needs an id; got a blank one." |
| An empty `zoomRange`, or one starting below zero | "Zoom range … must be non-empty and start at 0 or above." |

A template missing a placeholder is refused because it would fetch the same tile for many keys.

**Give `zoomRange` the levels the source really has.** Past its last level the map magnifies the deepest tile instead of asking for one that does not exist. A range wider than the source asks for tiles that fail, and retries them.

**Two sources are equal when they would fetch the same tiles**: the same id, template, zoom range, TMS flag and attribution. A layer keyed on its source therefore starts fresh when the source changes, and nothing of the old one stays on screen. `toString` leaves out the attribution but includes the template, so a key in the URL shows up in a log line that prints a source.

**No credit is shown unless the source carries one.** The attribution ornament shows only the `attribution` of each layer's source. See [Ornaments and Attribution](DOC-0026-ornaments-and-attribution.md).

## TileClient

A `TileClient` is one call, `suspend fun fetch(source: TileSource, key: TileKey): ByteArray?`. The app builds the URL with `source.url(key)`, adds its keys and headers, sends the request with its own HTTP client, and returns the body:

```kotlin
val client = TileClient { source, key -> http.get(source.url(key)).body<ByteArray>() }
```

`source.fetch(client, key)` is the same call the other way round, for code that writes its own layer.

- **Return null for a tile that is not there or could not be fetched**, and for a response that is not a success. The map then draws the nearest ancestor tile instead. Returning an error page's body hands the basemap bytes it cannot decode.
- **A failing client fails silently.** In map-view's tile queue, which the basemap layers fetch through, an exception thrown from `fetch` is caught and treated exactly like null, and the tile is tried again after ten seconds. A wrong key or an expired token gives a blank map and no error, so log inside the client.
- **It is called concurrently, off the main thread.** The queue runs several workers on `Dispatchers.Default`, so the client must be safe to call from several coroutines at once.
- **Make it once.** A `TileClient` is compared by identity. One written inline in a composable is a new client on every recomposition, and a basemap made from it is a new layer each time, so the map blanks and refetches every tile whenever unrelated state changes. Keep the client in a top-level or view-model `val`, or `remember` it.

## Caching

Caching is the app's choice. [wayfinder-cache](../wayfinder-cache/README.md) (`com.oddlyclever:wayfinder-cache`) fits in front of a client without translation, since its stores are keyed by source and tile too:

```kotlin
val store = FileTileStore(FileSystem.SYSTEM, cacheDir / "tiles", maxBytes = 200L * 1024 * 1024)
val client = TileClient { source, key ->
    cached(store, source.id, CacheMode.CacheFirst) { k -> http.get(source.url(k)).body<ByteArray>() }(key)
}
```

It also has a `MemoryTileStore`, for the web, and the modes `NetworkFirst`, `OfflineOnly` and `NoCache`. The root module does not depend on it; an app that wants a cache adds it.
