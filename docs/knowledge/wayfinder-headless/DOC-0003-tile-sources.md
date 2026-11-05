# Tile Sources

DOC-0003 · 2026-11-04
Keywords: tile server, tile URL, MVT, vector tiles, OpenMapTiles schema, raster tiles, PNG tiles, blank map, empty map, map shows only the route, gzip tiles, MBTiles, Content-Encoding, tile cache, rate limit, tile server down, API key in tile URL

`renderMapImage` makes no HTTP requests of its own. Every tile comes through the `tiles` function the caller passes, which takes a `TileKey` and returns the tile's bytes. This page covers what those bytes may be, what happens when they're wrong, and what the function is responsible for.

## The function

```kotlin
tiles = { key -> http.get("https://tiles.example.com/${key.z}/${key.x}/${key.y}.pbf").body<ByteArray>() }
```

`key.z` is the zoom level and `key.x` and `key.y` the tile's column and row, in the usual web-map scheme. The function builds the URL, adds any key or header the tile server needs, sends the request with the app's own client, and returns the body, or `null` for a tile it cannot supply. The URL, the credentials and the client all stay with the app, so the module never sees them.

## Images or vector tiles

A tile server may answer with images or with vector tiles, and the module tells them apart by the bytes, so there is no setting for it:

- **PNG, JPEG and WebP** are recognised by their signatures and drawn as they are.
- **Anything else** is read as a Mapbox Vector Tile and drawn in `style`, with names unless `names` is off.

Vector tiles have to use the OpenMapTiles schema, the layer and field names the styles in `MapStyles` are written for. A tile in another schema decodes without error but draws nothing, because no rule in the style matches its layers.

An empty `ByteArray` is a valid, empty vector tile: open ground with nothing on it. It is not a missing tile. Return `null` for a tile that doesn't exist.

## When a tile fails

A tile that fails never fails the call. An exception thrown from `tiles`, a `null`, and bytes that are neither an image nor a readable vector tile are all treated as a missing tile. A missing tile is drawn from its nearest ancestor, magnified, up to five levels up. Where there is no ancestor either, the style's background colour shows.

So when the tile server is down, `renderMapImage` still returns an image: the route and its markers on a plain background. That is deliberate, since an email or a preview is better with a plain map than with none, but it means a broken tile function looks like an empty map, not an error. When the map comes out empty, look at what `tiles` returns. The common causes are:

| Cause | What `tiles` returns |
|---|---|
| The server returned an error page | HTML or JSON, which is neither an image nor a vector tile |
| The tiles are gzip-compressed | Compressed bytes, which the decoder cannot read. Vector tiles from an MBTiles file are usually stored compressed, and an HTTP client that does not undo `Content-Encoding: gzip` passes them on that way. Decompress them in `tiles`. |
| The tiles use another schema | A valid vector tile that the style has no rules for |
| The URL is wrong for the zoom range | `null` or an error for every level |

## Zoom range

`zoomRange` is the levels the source has, `0..14` by default, which is where most vector tile servers stop. Tiles are only requested inside it, and the camera goes no closer than its last level. A raster source with more levels needs a wider range, such as `zoomRange = 0..19`.

## No cache

The module keeps no tiles between calls. Each call requests every tile it needs, all at once, so a server that renders images on demand sends a burst of requests to the tile server for every image. Caching belongs in the `tiles` function: an in-memory or disk cache keyed by `TileKey` in front of the HTTP client, such as [wayfinder-cache](../wayfinder-cache/README.md)'s `cached`, which needs no Compose. That is also where a rate limit, a timeout or a retry goes.

A cache is worth having for another reason: many tile services' usage policies ask for one, and some count every request against a quota.
