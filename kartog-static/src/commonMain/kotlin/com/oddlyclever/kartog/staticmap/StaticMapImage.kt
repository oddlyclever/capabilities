package com.oddlyclever.kartog.staticmap

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.coderwise.libs.mapview.LatLon
import com.oddlyclever.kartog.CameraPosition
import com.oddlyclever.kartog.StaticLayer
import com.oddlyclever.kartog.fit

/**
 * A map that does not move: a picture of [layers] where a live map would be too much, such as a card in a list or a
 * thumbnail. It renders without a composition of map-view, at the size it is laid out, and redraws when that changes.
 *
 * The camera is [position] if given, else the one that frames [fit] inside [padding]; with neither it shows nothing.
 * [overlay] draws over the map through its projection: a track, a pin.
 *
 * The map is drawn as a [StaticMapPainter], so if the image is scaled after it is drawn, lines stay sharp. Nothing is
 * shown until the tiles have arrived; [placeholder] fills the space until then.
 */
@Composable
fun StaticMapImage(
    layers: List<StaticLayer>,
    modifier: Modifier = Modifier,
    position: CameraPosition? = null,
    fit: List<LatLon> = emptyList(),
    padding: Dp = 32.dp,
    background: Color = Color.Transparent,
    contentDescription: String? = null,
    placeholder: @Composable () -> Unit = {},
    overlay: DrawScope.(StaticProjection) -> Unit = {},
) {
    val density = LocalDensity.current
    val currentOverlay = rememberUpdatedState(overlay)
    BoxWithConstraints(modifier) {
        val size = IntSize(constraints.maxWidth, constraints.maxHeight)
        var map by remember { mutableStateOf<PreparedMap?>(null) }
        LaunchedEffect(size, layers, position, fit, padding, background) {
            if (size.width <= 0 || size.height <= 0 || size.width == Int.MAX_VALUE || size.height == Int.MAX_VALUE) return@LaunchedEffect
            val camera = position ?: fit.takeIf { it.isNotEmpty() }?.let { fit(it, size, density, padding) } ?: return@LaunchedEffect
            map = prepareMap(size, density, camera, layers, background)
        }
        val ready = map
        if (ready == null) {
            placeholder()
        } else {
            val painter = remember(ready) { StaticMapPainter(ready) { currentOverlay.value(this, it) } }
            Image(painter, contentDescription, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        }
    }
}

/**
 * A prepared map as a [Painter]: drawn at whatever size it is given, scaled as a whole, so in an `Image` it scales like
 * a vector image instead of a bitmap. Its intrinsic size is the size the map was prepared for.
 */
class StaticMapPainter(
    private val map: PreparedMap,
    private val overlay: DrawScope.(StaticProjection) -> Unit = {},
) : Painter() {
    override val intrinsicSize: Size = Size(map.size.width.toFloat(), map.size.height.toFloat())

    override fun DrawScope.onDraw() {
        with(map) { draw(overlay) }
    }
}
