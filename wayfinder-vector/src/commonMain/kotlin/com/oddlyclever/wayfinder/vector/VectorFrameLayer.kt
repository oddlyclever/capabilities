package com.oddlyclever.wayfinder.vector

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import com.coderwise.libs.mapview.TileKey
import com.coderwise.libs.mapview.tiles.vector.VectorStyle
import com.coderwise.libs.mapview.tiles.vector.buildRenderTile
import com.coderwise.libs.mapview.tiles.vector.decodeVectorTile
import com.coderwise.libs.mapview.tiles.vector.drawRenderTile
import com.coderwise.libs.mapview.tiles.vector.drawVectorLabels
import com.coderwise.libs.mapview.tiles.vector.mvt.decodeMvt
import com.oddlyclever.wayfinder.MapFrameLayer
import com.oddlyclever.wayfinder.MapFrameTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * A vector layer for a map drawn without a composition, drawn by map-view's own renderer in [style], so a map frame
 * matches the live map.
 *
 * [load] fetches a tile's bytes, as a live layer's does; null or bytes that are not a tile leave the cell to its
 * nearest ancestor.
 *
 * With [names], the tile's place, road, water, peak and point-of-interest names are drawn too, laid out by map-view
 * exactly as on a live map, over every layer's ground and level at any bearing. Which kinds are named is [style]'s.
 * Laying text out needs a [TextMeasurer], and on Android that takes a context, so it is the app's to give:
 * `rememberTextMeasurer()` in a composable, or one built from a font family resolver elsewhere. Without one, the map
 * has no names. Names cost a second decode of each tile, and are laid out again whenever the frame is drawn.
 */
fun vectorFrameLayer(
    zoomRange: IntRange,
    style: VectorStyle = MapStyles.Light,
    names: TextMeasurer? = null,
    load: suspend (TileKey) -> ByteArray?,
): MapFrameLayer = object : MapFrameLayer {
    override val zoomRange = zoomRange

    override suspend fun tile(key: TileKey): MapFrameTile? {
        val bytes = load(key) ?: return null
        val render = withContext(Dispatchers.Default) {
            try {
                val layers = decodeMvt(bytes, keep = DRAWN_LAYERS, classLayers = CLASS_LAYERS, detailLayers = style.roadDetailLayers())
                buildRenderTile(layers, style)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        } ?: return null
        // Names come from map-view's own decode: the drawing above cannot carry them, and map-view keeps how it reads
        // them to itself.
        val named = if (names != null) decodeVectorTile(key.z, bytes, style) else null
        return object : MapFrameTile {
            override fun DrawScope.draw(src: Rect, zoom: Int) = drawRenderTile(render, style.background, zoom, src)

            override fun DrawScope.drawLabels(src: Rect, zoom: Int, bearing: Float) {
                if (named != null && names != null) drawVectorLabels(named, src, names, bearing)
            }
        }
    }
}
