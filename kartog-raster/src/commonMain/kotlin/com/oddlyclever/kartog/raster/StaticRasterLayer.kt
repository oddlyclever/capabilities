package com.oddlyclever.kartog.raster

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.coderwise.libs.mapview.TileKey
import com.oddlyclever.kartog.StaticLayer
import com.oddlyclever.kartog.StaticTile
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A raster layer for a map drawn without a composition. [load] fetches a tile's bytes; they are decoded with
 * [decodeRasterTile], so a missing or unreadable tile leaves the cell to its nearest ancestor.
 */
fun staticRasterLayer(zoomRange: IntRange, load: suspend (TileKey) -> ByteArray?): StaticLayer = object : StaticLayer {
    override val zoomRange = zoomRange

    override suspend fun tile(key: TileKey): StaticTile? {
        val image = decodeRasterTile(load(key)) ?: return null
        return StaticTile { src, _ ->
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
