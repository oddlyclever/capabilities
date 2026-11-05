package com.oddlyclever.wayfinder

import com.coderwise.libs.mapview.TileKey

/**
 * How wayfinder's layers get tile bytes: the app's HTTP client, keys and cache, behind one call.
 *
 * wayfinder fetches nothing itself. A layer asks for the tile at [key] from [source]; the app builds the URL with
 * `source.url(key)`, adds whatever headers it needs, and caches as it likes. `com.oddlyclever:wayfinder-cache`'s stores are
 * keyed the same way, by source and tile, so they go in front with no translation:
 *
 * ```
 * val client = TileClient { source, key ->
 *     cached(store, source.id, CacheMode.CacheFirst) { k -> http.get(source.url(k)).body<ByteArray>() }(key)
 * }
 * ```
 *
 * Return null for a tile that is not there or could not be fetched: the map shows the nearest ancestor instead.
 */
fun interface TileClient {
    suspend fun fetch(source: TileSource, key: TileKey): ByteArray?
}

/** The bytes of the tile at [key] from this source, through [client]. */
suspend fun TileSource.fetch(client: TileClient, key: TileKey): ByteArray? = client.fetch(this, key)
