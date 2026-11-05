# wayfinder-cache

The full documentation of [`wayfinder-cache`](https://github.com/oddlyclever/capabilities/blob/HEAD/wayfinder-cache/README.md): a tile cache in front of the app's `TileClient`. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Where it goes

wayfinder fetches nothing itself, as [The App Fetches Every Tile](Wayfinder-The-App-Fetches-Every-Tile) explains, so a cache goes where the fetching is: in the app's `TileClient`, wrapped around the HTTP call. `cached(store, source, mode, fetch)` returns a function of the same shape as `fetch`, and whatever calls it never knows the store is there. The same wrapping works in `wayfinder-headless`'s `tiles` function and in a map frame's load function, which take a `TileKey` and return bytes in the same way.

What is cached is the bytes, before they are decoded. A vector basemap that changes style, light to dark for example, decodes every tile again; with a cache it does so without fetching them again.

## Stores

- **`FileTileStore(fileSystem, directory, maxBytes)`** keeps one file per tile at `source/z/x/y.tile`, up to `maxBytes` in all, 200 MB by default, dropping the least recently used first. It takes an okio `FileSystem` rather than reaching for one, which keeps it in common code: hand it `FileSystem.SYSTEM` over the app's cache directory on Android, iOS and the desktop. A tile is written beside its place and then moved there, so one being written when the process died is never read back as whole. Recency is kept in memory; after a restart, the order the files were written in stands in for it.
- **`MemoryTileStore(maxBytes)`** is the same, bounded and least-recently-used, in memory, 64 MB by default. It is what a browser build uses, since there is no file system, and what tests use.
- **`TileStore`** is the interface both implement, `get`, `put`, `remove` and `clear`, for a store of the app's own. A tile is addressed by its source and its `TileKey`, never by a hash, so two tiles can never be mistaken for one another.

## Modes

| `CacheMode` | Reads the store | Uses the network | Use it for |
|---|---|---|---|
| `CacheFirst` | first | for what is not held, which is then kept | most maps |
| `NetworkFirst` | only when the network fails | always first | tiles that change, such as weather or traffic |
| `OfflineOnly` | only | never | a map that must not use the network |
| `NoCache` | never | always | turning the cache off without rewiring the client |

A failed fetch is the client's own failure, which the map treats as a missing tile, except in `NetworkFirst`, where falling back to the store is the point.

## A cache is not offline maps

A cache keeps what was looked at. A map that has to work with no network needs more:

- **Nothing downloads an area ahead of time.** To make an area available offline, the app walks its tiles over the zoom range it needs and passes each through the cached client, or writes them into the store with `put`.
- **Downloaded tiles can be evicted.** `FileTileStore` drops the least recently used tiles once it is over `maxBytes`, and the cache directory is one the operating system may clear when space runs low. Tiles that must stay belong in a store of their own: a separate `FileTileStore` in a directory the system does not clear, with a limit large enough for the area, or a `TileStore` of the app's own that never evicts them.
- **Tile servers' terms apply.** Many tile services forbid or limit bulk downloading, separately from what they allow a cache to keep. Check before downloading an area.

## Where it came from

It began as map-view's `map-view-cache` 0.3.0, Apache-2.0, copied here when map-view chose to stop publishing it. It is wayfinder's to maintain now: a fix made in map-view does not reach it unless it is copied across. map-view's `tileUrl` was left out, because wayfinder's `TileSource.url` builds tile URLs.
