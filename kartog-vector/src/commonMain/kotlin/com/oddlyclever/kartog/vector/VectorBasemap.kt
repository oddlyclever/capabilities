package com.oddlyclever.kartog.vector

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import com.coderwise.libs.mapview.LocalTileMagnification
import com.coderwise.libs.mapview.TileKey
import com.coderwise.libs.mapview.tiles.raster.RasterSlot
import com.coderwise.libs.mapview.tiles.rememberTileState
import com.coderwise.libs.mapview.tiles.shown
import com.coderwise.libs.mapview.tiles.vector.VectorLabels
import com.coderwise.libs.mapview.tiles.vector.VectorSlot
import com.coderwise.libs.mapview.tiles.vector.VectorStyle
import com.coderwise.libs.mapview.tiles.vector.VectorTile
import com.coderwise.libs.mapview.tiles.vector.decodeVectorTile
import com.oddlyclever.kartog.KartogLayer
import com.oddlyclever.kartog.PlacedLayer
import com.oddlyclever.kartog.TileClient
import com.oddlyclever.kartog.TileSource
import com.oddlyclever.kartog.fetch
import kotlin.math.floor

/**
 * A vector basemap for a `KartogMap`: [source]'s tiles fetched through [client], drawn by map-view in [style], with
 * place and road names over them.
 *
 * [style] defaults to the one provided by [VectorStyleProvider], else the light style. Tiles are drawn as paths on the
 * GPU, through whatever renderer the platform uses. With [rasterize], the ground is drawn to images on the CPU
 * instead ([rasterizeVectorTile]), for an app that has a reason to keep tile paths off the GPU; names are still drawn
 * from the decoded tile, since text does not go through the path renderer.
 */
fun vectorBasemap(
    source: TileSource,
    client: TileClient,
    style: VectorStyle? = null,
    labels: Boolean = true,
    rasterize: Boolean = false,
): KartogLayer = KartogLayer { state ->
    val resolved = style ?: LocalVectorStyle.current
    val density = LocalDensity.current.density
    val viewZoom by remember(state) { derivedStateOf { floor(state.zoom).toInt() } }
    val tiles = rememberTileState(key = Triple(source, resolved, rasterize), zoomRange = source.zoomRange) { key ->
        val bytes = source.fetch(client, key) ?: return@rememberTileState null
        val decoded = decodeVectorTile(key.z, bytes, resolved) ?: return@rememberTileState null
        VectorCell(decoded, if (rasterize) rasterizeVectorTile(bytes, key.z, density, resolved) else null)
    }
    val ground: @Composable (TileKey) -> Unit = { key ->
        tiles.shown(key)?.let { shown ->
            val image = shown.content.image
            if (image != null) RasterSlot(image, shown.src)
            else VectorSlot(shown.content.tile, shown.src, viewZoom, magnification = LocalTileMagnification.current)
        }
    }
    val names: @Composable (TileKey) -> Unit = { key ->
        tiles.shown(key)?.let { shown ->
            VectorLabels(shown.content.tile, shown.src, bearing = state.bearing, magnification = LocalTileMagnification.current)
        }
    }
    PlacedLayer(tiles, if (labels) listOf(ground, names) else listOf(ground), source.attribution)
}

/** A tile as decoded, and as an image too where the paths cannot be drawn on the GPU. */
private class VectorCell(val tile: VectorTile, val image: ImageBitmap?)

/**
 * The vector style in scope, the way `MaterialTheme.colorScheme` is: what [vectorBasemap] draws in when it is given
 * none. Defaults to the light style.
 */
val LocalVectorStyle = staticCompositionLocalOf { KartogStyles.Light }

/**
 * Provides [style] to every vector basemap inside [content] that is not given one of its own. Wrap the map screen to
 * switch a whole app's maps between light and dark at once: `VectorStyleProvider(systemVectorStyle()) { … }`.
 */
@Composable
fun VectorStyleProvider(style: VectorStyle, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalVectorStyle provides style, content = content)
