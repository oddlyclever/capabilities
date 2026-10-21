package com.oddlyclever.kartog.vector

import com.coderwise.libs.mapview.TileKey
import com.coderwise.libs.mapview.tiles.vector.VectorStyle
import com.coderwise.libs.mapview.tiles.vector.buildRenderTile
import com.coderwise.libs.mapview.tiles.vector.drawRenderTile
import com.coderwise.libs.mapview.tiles.vector.mvt.decodeMvt
import com.oddlyclever.kartog.StaticLayer
import com.oddlyclever.kartog.StaticTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * A vector layer for a map drawn without a composition, drawn by map-view's own renderer in [style], so an image
 * matches the live map.
 *
 * [load] fetches a tile's bytes, as a live layer's does; null or bytes that are not a tile leave the cell to its
 * nearest ancestor. Labels are not drawn, since map-view draws them only inside a composition (request W3 in the
 * kartog plan).
 */
fun staticVectorLayer(
    zoomRange: IntRange,
    style: VectorStyle = KartogStyles.Light,
    load: suspend (TileKey) -> ByteArray?,
): StaticLayer = object : StaticLayer {
    override val zoomRange = zoomRange

    override suspend fun tile(key: TileKey): StaticTile? {
        val bytes = load(key) ?: return null
        val render = withContext(Dispatchers.Default) {
            try {
                buildRenderTile(decodeMvt(bytes, keep = DRAWN_LAYERS, classLayers = CLASS_LAYERS), style)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        } ?: return null
        return StaticTile { src, zoom -> drawRenderTile(render, style.background, zoom, src) }
    }
}
