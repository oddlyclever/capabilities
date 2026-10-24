package com.oddlyclever.wayfinder.raster

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.coderwise.libs.mapview.TileKey
import com.oddlyclever.wayfinder.MapFrameLayer
import com.oddlyclever.wayfinder.MapFrameTile
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A raster layer for a map drawn without a composition. [load] fetches a tile's bytes; they are decoded with
 * [decodeRasterTile], so a missing or unreadable tile leaves the cell to its nearest ancestor.
 */
fun rasterFrameLayer(zoomRange: IntRange, load: suspend (TileKey) -> ByteArray?): MapFrameLayer = object : MapFrameLayer {
    override val zoomRange = zoomRange

    override suspend fun tile(key: TileKey): MapFrameTile? {
        val image = decodeRasterTile(load(key)) ?: return null
        return MapFrameTile { src, _ ->
            val left = floor(src.left * image.width).toInt()
            val top = floor(src.top * image.height).toInt()
            drawImage(
                image = image,
                srcOffset = IntOffset(left, top),
                srcSize = IntSize(
                    (ceil(src.right * image.width).toInt() - left).coerceAtLeast(1),
                    (ceil(src.bottom * image.height).toInt() - top).coerceAtLeast(1),
                ),
                dstSize = IntSize(ceil(size.width).toInt(), ceil(size.height).toInt()),
            )
        }
    }
}
