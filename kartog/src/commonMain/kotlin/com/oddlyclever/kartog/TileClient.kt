package com.oddlyclever.kartog

/**
 * How kartog's layers get tile bytes: the app's HTTP client, keys and cache, behind one call.
 *
 * kartog fetches nothing itself. A layer asks for a URL built from its [TileSource], with a [cacheKey] naming the tile
 * by source and position, so a cache keyed on it never confuses two sources. What happens next (headers, keys, a disk
 * cache, offline modes) is the app's; map-view-cache's `cached` and `FileTileStore` are one way to do the caching.
 *
 * Return null for a tile that is not there or could not be fetched: the map shows the nearest ancestor instead.
 */
fun interface TileClient {
    suspend fun fetch(url: String, cacheKey: String): ByteArray?
}

/** The bytes of the tile at [key] from this source, through [client], keyed as `<id>/<z>/<x>/<y>`. */
suspend fun TileSource.fetch(client: TileClient, key: com.coderwise.libs.mapview.TileKey): ByteArray? =
    client.fetch(url(key), "$id/${key.z}/${key.x}/${key.y}")
