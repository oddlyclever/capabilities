package com.oddlyclever.wayfinder.raster

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import com.coderwise.libs.mapview.tiles.raster.RasterSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * A tile's bytes as an image ready to draw, or null when there is nothing to draw.
 *
 * Decoding a 256-pixel tile takes long enough to cost a frame on the main thread, so this does it on
 * [Dispatchers.Default] whatever thread it is called from: call it from a tile state's `load`, upstream of the view,
 * and each tile is decoded once as it arrives rather than whenever a slot wants it.
 *
 * Null for no bytes, empty bytes, or bytes no decoder on this platform can read. A tile server answering with an
 * error page or a truncated image is a missing tile, not a crash; the map shows the nearest ancestor instead. PNG and
 * JPEG decode everywhere; WebP decodes wherever the platform's image decoder reads it.
 */
suspend fun decodeRasterTile(bytes: ByteArray?): ImageBitmap? {
    if (bytes == null || bytes.isEmpty()) return null
    return withContext(Dispatchers.Default) {
        try {
            bytes.decodeToImageBitmap().takeIf { it.width > 0 && it.height > 0 }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * map-view's [RasterSlot], fading [tile] in when it arrives.
 *
 * Only a tile that arrives after its cell has shown nothing fades. A cell composed with its tile already in hand draws
 * it at once, so panning back over tiles already loaded does not make them flicker in again. Null draws nothing,
 * leaving whatever is beneath the layer.
 *
 * [fadeMillis] of 0 turns the fade off. [alpha] scales the whole slot, faded or not, for a layer drawn translucent
 * over another.
 */
@Composable
fun FadingRasterSlot(
    tile: ImageBitmap?,
    src: Rect,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    fadeMillis: Int = DEFAULT_FADE_MILLIS,
) {
    val shown = remember { Animatable(if (tile != null || fadeMillis <= 0) 1f else 0f) }
    LaunchedEffect(tile != null) {
        if (tile != null && shown.value < 1f) shown.animateTo(1f, tween(durationMillis = fadeMillis))
    }
    if (tile == null) return
    // Read in the draw phase, so each frame of the fade redraws the layer rather than recomposing the slot.
    RasterSlot(tile, src, modifier.graphicsLayer { this.alpha = shown.value * alpha })
}

/** How long a newly arrived tile takes to fade in: long enough to read as arriving, short enough not to lag a pan. */
const val DEFAULT_FADE_MILLIS = 300
