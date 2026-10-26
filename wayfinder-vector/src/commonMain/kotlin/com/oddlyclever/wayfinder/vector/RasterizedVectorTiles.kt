package com.oddlyclever.wayfinder.vector

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.coderwise.libs.mapview.tiles.vector.VectorStyle
import com.coderwise.libs.mapview.tiles.vector.buildRenderTile
import com.coderwise.libs.mapview.tiles.vector.drawRenderTile
import com.coderwise.libs.mapview.tiles.vector.mvt.decodeMvt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.ceil

/**
 * A vector tile drawn to an image, for an app that would rather not draw tile paths on the GPU: one that has seen a
 * GPU driver crash drawing complex paths on some device, for instance.
 *
 * The tile is drawn by map-view's own renderer in [style], on the CPU, at the size a tile is shown at: 256 dp at
 * [density]. [zoom] is the level the tile was cut at, which decides the road widths it is drawn with. Names are not
 * drawn into the image: on a map the user pans, zooms and turns, a name baked into a tile is clipped at its edge,
 * blurs when the tile is magnified and turns with the map. Show them with map-view's `VectorLabels` over the image
 * instead, as over a vector slot, which is what [vectorBasemap] does.
 *
 * It costs more than decoding: tens of milliseconds a tile, on [Dispatchers.Default] whatever thread calls it. Call
 * it from a tile state's `load`, upstream of the view. Null for bytes that are not a tile.
 */
suspend fun rasterizeVectorTile(
    bytes: ByteArray,
    zoom: Int,
    density: Float,
    style: VectorStyle = MapStyles.Light,
): ImageBitmap? = withContext(Dispatchers.Default) {
    try {
        val layers = decodeMvt(bytes, keep = DRAWN_LAYERS, classLayers = CLASS_LAYERS, detailLayers = style.roadDetailLayers())
        val render = buildRenderTile(layers, style)
        val side = ceil(TILE_DP * density).toInt()
        val image = ImageBitmap(side, side)
        CanvasDrawScope().draw(Density(density), LayoutDirection.Ltr, Canvas(image), Size(side.toFloat(), side.toFloat())) {
            drawRenderTile(render, style.background, zoom, WHOLE_TILE)
        }
        image
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}

/** A tile's side in dp, as map-view lays tiles out. */
private const val TILE_DP = 256

/** The whole of a tile, as a fraction of it. */
private val WHOLE_TILE = Rect(0f, 0f, 1f, 1f)

/**
 * The layers map-view's style paints, as map-view's vector slot decodes them. Copied, because map-view keeps its
 * list internal; a layer missing here is simply not drawn in the image.
 */
internal val DRAWN_LAYERS = setOf(
    "landcover", "landuse", "park", "water", "waterway", "aeroway", "building", "boundary",
    "transportation", "transportation_casing",
)

/** What each layer is styled by: `class` for most, `admin_level` for boundaries. */
internal val CLASS_LAYERS = VectorStyle.CLASS_AWARE_LAYERS

/**
 * The road tags to decode for this style: a road's `subclass` and `brunnel` when it paints by them through
 * [VectorStyle.roadDetail], else none, since resolving them costs every tile. As map-view's vector slot decides.
 */
internal fun VectorStyle.roadDetailLayers(): Map<String, List<String>>? =
    if (roadDetail != null) VectorStyle.DETAIL_LAYERS else null
