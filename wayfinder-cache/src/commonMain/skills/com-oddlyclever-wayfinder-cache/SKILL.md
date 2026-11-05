---
name: com-oddlyclever-wayfinder-cache
description: >-
  A tile cache for wayfinder maps in Kotlin Multiplatform: keeps the bytes the
  app's TileClient fetches in a size-bounded store on disk or in memory, so a
  map reopens, restyles or pans back without fetching every tile again. Use
  instead of writing a tile cache by hand, to cache map tiles on Android, iOS,
  the desktop or a server, to make a map work from what it has seen when the
  network is down, to keep tiles fresh with a fallback, or to stay within a
  tile service's request quota or usage policy. Wraps any function from a
  TileKey to bytes, so it also fits wayfinder-headless's tiles function and a
  map frame's load function. Cache-first, network-first, offline-only and
  no-cache modes.
license: Apache-2.0
metadata:
  version: "0.0.7"
  repository: https://github.com/oddlyclever/capabilities
---

# wayfinder-cache

`com.oddlyclever:wayfinder-cache`, package `com.oddlyclever.wayfinder.cache`. Called from Kotlin only. It brings map-view-core, for `TileKey` in `com.coderwise.libs.mapview`, and okio, both as `api`; it does not bring Compose or `com.oddlyclever:wayfinder`.

## What it solves

wayfinder fetches nothing itself: tiles come through the app's `TileClient`. Without a cache, a map fetches every tile again whenever it leaves the screen and comes back, whenever its style changes, and whenever the app restarts. This module keeps the fetched bytes and answers from them, without the client or the map changing shape.

## How it is meant to be used

Wrap the HTTP call inside the client, once, outside any composable:

```kotlin
import com.oddlyclever.wayfinder.cache.CacheMode
import com.oddlyclever.wayfinder.cache.FileTileStore
import com.oddlyclever.wayfinder.cache.cached
import okio.FileSystem
import okio.Path.Companion.toPath

val store = FileTileStore(FileSystem.SYSTEM, cacheDir.toPath() / "tiles", maxBytes = 200L * 1024 * 1024)
val client = TileClient { source, key ->
    cached(store, source.id, CacheMode.CacheFirst) { k -> http.get(source.url(k)).body<ByteArray>() }(key)
}
```

On the web there is no `FileSystem.SYSTEM`: use `MemoryTileStore(maxBytes)`. On a server, wrap `wayfinder-headless`'s `tiles` function the same way, with a fixed source name.

## Invariants and traps

**Name every source differently.** The second argument of `cached` is what keeps one source's tile 14/4824/6159 from answering for another's. Pass `source.id`, and give two `TileSource`s sharing a store different ids. A constant string for every source mixes them silently.

**Make the client and the store once.** A `TileClient` is compared by identity, so one built in a composable makes the map refetch everything on each recomposition, cache or not. Two `FileTileStore`s over one directory each keep their own count and evict each other's files; use one per directory.

**A broken store never shows.** A store that cannot be read is a miss and one that cannot be written is skipped, so a cache in an unwritable directory looks exactly like no cache. Check the directory exists and is writable if tiles keep being fetched.

**It is not offline maps.** Nothing downloads an area ahead of time, and `FileTileStore` evicts the least recently used tiles once it is over `maxBytes`, from a directory the operating system may clear. Tiles that must stay go in a separate store with room for them, outside the cache directory. Many tile services limit bulk downloading; check their terms.

**`OfflineOnly` never touches the network,** even for a tile it does not hold, which then comes back null and the map draws the nearest ancestor tile.

**Bytes are shared, not copied.** What `get` returns is what was put; treat it as read-only.

## What moved, and what it used to be called

New in 0.0.7. It is map-view's `map-view-cache` 0.3.0 (`com.coderwise.libs.mapview.cache`), moved into `com.oddlyclever.wayfinder.cache`. Code written against map-view-cache changes its imports and nothing else, except `tileUrl`, which is gone: build URLs with wayfinder's `TileSource.url(key)`.

## What it is not for

It fetches nothing: the function passed to `cached` is the app's, with its client, headers, retries and rate limits. It stores bytes, not decoded tiles; the basemap modules keep decoded tiles in memory themselves. It does not download areas for offline use, expire tiles by age, or read MBTiles files.
