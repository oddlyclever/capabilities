# The App Fetches Every Tile

ADR-0014 · 2026-11-04 · Status: accepted
Keywords: why does wayfinder not download tiles, built-in HTTP client, Ktor dependency, tile URL with API key, tile headers, tile cache built in, offline tiles, TileClient, TileSource, map-view-cache, who owns the network, mechanism not policy

## Context

A map needs tiles from a server, and how they are fetched differs from app to app: which HTTP client, which keys and headers the tile server wants, whether and where tiles are cached, and how long they are kept. A capability in this repository is mechanism, not policy, and an app's endpoints and configuration are parameters it passes in. A map library that fetched its own tiles would have to choose a client, hold the app's keys, and decide on a cache for it.

## Decision

wayfinder fetches nothing itself.

- **A `TileSource` describes a source and fetches nothing**: its id, URL template, zoom range, row numbering and credit.
- **A `TileClient` is the app's fetching behind one call**, `fetch(source, key)`. The app builds the URL with `source.url(key)`, adds whatever headers it needs, uses its own HTTP client, and caches as it likes.
- **Caching is left to the app.** map-view-cache's stores are keyed the same way, by source and tile, so they go in front of a client with no translation, but wayfinder does not depend on them.

## Consequences

- wayfinder carries no HTTP client and no cache. The app's keys are only where the app puts them: in its client, or in a source's URL template.
- The same client serves every layer, and the app's existing networking, logging, retries and authentication apply to tiles too.
- Every app writes the client, even a minimal one, and a client that hides its failures gives a blank map with no error. [Tile Sources and Tile Clients](DOC-0028-tile-sources-and-tile-clients.md) covers what the client must do.
- A client is compared by identity, so it has to be made once and kept, or the layers made from it change on every recomposition.
