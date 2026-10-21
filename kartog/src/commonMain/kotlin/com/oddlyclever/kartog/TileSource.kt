package com.oddlyclever.kartog

import com.coderwise.libs.mapview.TileKey
import com.coderwise.libs.mapview.tiles.DEFAULT_ZOOM_RANGE

/**
 * Where a layer's tiles come from: a URL template, the levels the source has, and the line it must be credited with.
 *
 * It describes a source and fetches nothing. Loading belongs to the app, which knows its HTTP client, its keys and
 * its cache, so a layer reads a source like this:
 *
 * ```
 * val ground = rememberTileState(key = streets, zoomRange = streets.zoomRange) { key ->
 *     http.get(streets.url(key))
 * }
 * ```
 *
 * Passing the source as the state's `key` is deliberate: two sources are equal when they would fetch the same tiles,
 * so swapping to a different source starts fresh slots and nothing of the old one stays on screen.
 *
 * @property id names the source wherever tiles from different sources must not be confused, such as a cache key.
 * @property urlTemplate a URL with `{z}`, `{x}` and `{y}` where the tile's zoom, column and row go. All three must be
 *   there, since a template missing one fetches the same tile for many keys.
 * @property zoomRange the levels the source has. Past its last level the map magnifies the deepest tile instead of
 *   asking for one that does not exist.
 * @property tms whether the source numbers rows from the bottom, as TMS sources do. map-view numbers them from the
 *   top, XYZ style, so a TMS source has its row flipped in [url]; everything else about the key is unchanged.
 * @property attribution the credit the source's licence asks to be shown with it, or null when it asks for none.
 * @throws IllegalArgumentException when [id] is blank, the template lacks a placeholder, or [zoomRange] is empty or
 *   starts below zero.
 */
class TileSource(
    val id: String,
    val urlTemplate: String,
    val zoomRange: IntRange = DEFAULT_ZOOM_RANGE,
    val tms: Boolean = false,
    val attribution: String? = null,
) {
    init {
        require(id.isNotBlank()) { "A tile source needs an id; got a blank one." }
        val missing = PLACEHOLDERS.filterNot { it in urlTemplate }
        require(missing.isEmpty()) { "Tile URL template \"$urlTemplate\" is missing ${missing.joinToString()}." }
        require(!zoomRange.isEmpty() && zoomRange.first >= 0) { "Zoom range $zoomRange must be non-empty and start at 0 or above." }
    }

    /** The URL of the tile at [key], with its row flipped first when the source is [tms]. */
    fun url(key: TileKey): String {
        val row = if (tms) (1 shl key.z) - 1 - key.y else key.y
        return urlTemplate
            .replace("{z}", key.z.toString())
            .replace("{x}", key.x.toString())
            .replace("{y}", row.toString())
    }

    override fun equals(other: Any?): Boolean =
        other is TileSource &&
            id == other.id &&
            urlTemplate == other.urlTemplate &&
            zoomRange == other.zoomRange &&
            tms == other.tms &&
            attribution == other.attribution

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + urlTemplate.hashCode()
        result = 31 * result + zoomRange.hashCode()
        result = 31 * result + tms.hashCode()
        result = 31 * result + (attribution?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "TileSource(id=$id, urlTemplate=$urlTemplate, zoomRange=$zoomRange, tms=$tms)"

    private companion object {
        val PLACEHOLDERS = listOf("{z}", "{x}", "{y}")
    }
}
