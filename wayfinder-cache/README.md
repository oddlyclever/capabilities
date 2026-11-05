# wayfinder-cache

A tile cache for wayfinder maps, in front of the app's own `TileClient`: a store on disk or in memory, and four ways of sharing the work with the network. It caches the bytes the client returns, before they are decoded, so a change of style redraws without fetching again.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:wayfinder-cache:0.0.7")
```

The package is `com.oddlyclever.wayfinder.cache`. It depends on map-view-core for `TileKey` and on okio, both as `api`, and not on Compose or on `wayfinder` itself, so a server drawing map images with `wayfinder-headless` can use it as well as an app. There is no dependency-injection container.

## Use

```kotlin
// Once, outside any composable: a TileClient is compared by identity.
val store = FileTileStore(FileSystem.SYSTEM, cacheDir.toPath() / "tiles", maxBytes = 200L * 1024 * 1024)
val client = TileClient { source, key ->
    cached(store, source.id, CacheMode.CacheFirst) { k -> http.get(source.url(k)).body<ByteArray>() }(key)
}
```

`source.id` names the source in the store, so two sources sharing one store never answer for each other. `CacheMode` is `CacheFirst`, the usual choice; `NetworkFirst`, for tiles that should be fresh, with what is held as the fallback; `OfflineOnly`, which never touches the network; and `NoCache`. A browser has no file system to give `FileTileStore`, so a web build uses `MemoryTileStore`, with the browser's own HTTP cache doing the disk's job.

A cache is never the reason a tile does not load: a store that cannot be read is a miss, and one that cannot be written is skipped.

It is a cache, not offline maps on its own: nothing here downloads an area ahead of time, and `FileTileStore` evicts by size from a directory the system may clear. [Caching Tiles](../docs/knowledge/wayfinder-cache/README.md) says what that means for a map that has to work with no network.

## Documentation

The full documentation is in the module's section of the [knowledge base](../docs/knowledge/wayfinder-cache/README.md), which is mirrored to the repository's wiki.

## Third-party code

The module is derived from map-view-cache 0.3.0, part of [map-view](https://github.com/coderwise/libs-mobile), under the Apache License 2.0, taken when map-view chose to stop publishing it. It was moved into `com.oddlyclever.wayfinder.cache`, and map-view's `tileUrl` was left out because wayfinder's `TileSource.url` builds tile URLs. Each file's header says so.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-wayfinder-cache`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-wayfinder-cache/SKILL.md](src/commonMain/skills/com-oddlyclever-wayfinder-cache/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-wayfinder-cache/`.
