package com.oddlyclever.wayfinder.headless

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.TileKey
import com.coderwise.libs.mapview.tiles.vector.VectorStyle
import com.oddlyclever.wayfinder.MapFrameLayer
import com.oddlyclever.wayfinder.MapFrameTile
import com.oddlyclever.wayfinder.fit
import com.oddlyclever.wayfinder.raster.rasterFrameLayer
import com.oddlyclever.wayfinder.frame.MAP_WEBP_QUALITY
import com.oddlyclever.wayfinder.frame.MapFrameProjection
import com.oddlyclever.wayfinder.frame.encodeToPng
import com.oddlyclever.wayfinder.frame.encodeToWebp
import com.oddlyclever.wayfinder.frame.renderMap
import com.oddlyclever.wayfinder.track.HeadingMarker
import com.oddlyclever.wayfinder.track.LineStyle
import com.oddlyclever.wayfinder.track.SpeedStyle
import com.oddlyclever.wayfinder.track.TrackPoint
import com.oddlyclever.wayfinder.track.TrackStyle
import com.oddlyclever.wayfinder.track.drawTrack
import com.oddlyclever.wayfinder.vector.MapStyles
import com.oddlyclever.wayfinder.vector.vectorFrameLayer
import kotlin.math.roundToInt

/** What [renderMapImage] encodes to. */
enum class ImageFormat {
    /** Several times smaller than PNG; not shown by some desktop mail clients. */
    Webp,

    /** Lossless, and shown everywhere. */
    Png,
}

/** How [renderMapImage] draws its lines: a [color] line over a darker [casing], so it reads on pale land and on water. */
class HeadlessLineStyle(
    val color: Color = Color(0xFF7C3AED),
    val width: Dp = 2.5.dp,
    val casing: Color = Color(0xCC1E1B2E),
    val casingWidth: Dp = 4.5.dp,
)

/**
 * A map image on a server, from coordinates to encoded bytes in one call: the camera fitted to [lines] and [points],
 * the tiles fetched through [tiles], the lines drawn, the ends and waypoints marked, and the result encoded.
 *
 * - **The image** is [width] by [height] at [scale]: 600 by 400 at scale 2 is a 1200 by 800 image for screens that
 *   need it, with lines and markers twice as thick.
 * - **The camera** frames every coordinate inside [padding], no closer than the last level of [zoomRange], so a single
 *   point shows the streets around it.
 * - **Tiles** may be images (PNG, JPEG or WebP) or vector tiles, told apart by their bytes; vector tiles are drawn in
 *   [style], with their place, road, water, peak and point-of-interest names unless [names] is off. A tile [tiles]
 *   cannot supply is drawn from its nearest ancestor.
 * - **Lines** are drawn one by one, so a route in pieces never jumps straight from one piece to the next. The first
 *   point of the first line is marked as the start and the last point of the last as the end, and [points] as
 *   waypoints; with no lines, [points] are marked as places. The lines' own points are never marked.
 * - **Encoding** is [format], WebP by default at [quality].
 *
 * Needs Java 21, and on Linux the system libraries Skia links against: on Debian or Ubuntu, `libgl1`, `libx11-6` and
 * `libegl1`. Skia's own native library comes with this module, for Linux and macOS on x64 and arm64. Names are drawn
 * in the machine's default font, so a server that draws them needs a font installed, such as `fonts-dejavu-core`.
 *
 * @throws IllegalArgumentException when there is no coordinate to fit, or the size is empty.
 * @throws com.oddlyclever.media.webp.WebpException when the WebP encoder refuses the image.
 */
suspend fun renderMapImage(
    width: Int,
    height: Int,
    lines: List<List<LatLon>> = emptyList(),
    points: List<LatLon> = emptyList(),
    tiles: suspend (TileKey) -> ByteArray?,
    scale: Float = 1f,
    format: ImageFormat = ImageFormat.Webp,
    quality: Int = MAP_WEBP_QUALITY,
    style: VectorStyle = MapStyles.Light,
    names: Boolean = true,
    zoomRange: IntRange = DEFAULT_ZOOM_RANGE,
    padding: Dp = 48.dp,
    line: HeadlessLineStyle = HeadlessLineStyle(),
): ByteArray {
    val everything = lines.flatten() + points
    require(everything.isNotEmpty()) { "Nothing to draw: no lines and no points." }
    val size = IntSize((width * scale).roundToInt(), (height * scale).roundToInt())
    require(size.width > 0 && size.height > 0) { "An image needs a size; got $width x $height at scale $scale." }
    val density = Density(scale)
    val position = fit(everything, size, density, padding, maxZoom = zoomRange.last.toFloat())
    val ends = TrackStyle(line = LineStyle.None, speed = SpeedStyle.None, heading = HeadingMarker.None, showAccuracy = false)

    val measurer = if (names) TextMeasurer(createFontFamilyResolver(), density, LayoutDirection.Ltr) else null
    val layer = eitherLayer(zoomRange, style, measurer, tiles)
    val image = renderMap(size, density, position, listOf(layer), background = style.background) { projection ->
        for (piece in lines) drawLine(piece, projection, line)
        drawTrack(lines.flatten().map { TrackPoint(it) }, projection, ends, waypoints = points)
    }
    return when (format) {
        ImageFormat.Webp -> image.encodeToWebp(quality)
        ImageFormat.Png -> image.encodeToPng()
    }
}

/** How far in a server image goes: street level, where most tile servers' vector tiles stop. */
val DEFAULT_ZOOM_RANGE = 0..14

private fun DrawScope.drawLine(points: List<LatLon>, projection: MapFrameProjection, style: HeadlessLineStyle) {
    if (points.size < 2) return
    val path = Path().apply {
        points.forEachIndexed { i, point ->
            val at = projection.project(point)
            if (i == 0) moveTo(at.x, at.y) else lineTo(at.x, at.y)
        }
    }
    drawPath(path, style.casing, style = Stroke(style.casingWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, style.color, style = Stroke(style.width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** One layer for a tile server that may answer with images or with vector tiles. */
private fun eitherLayer(
    zoomRange: IntRange,
    style: VectorStyle,
    names: TextMeasurer?,
    tiles: suspend (TileKey) -> ByteArray?,
) = object : MapFrameLayer {
    override val zoomRange = zoomRange

    override suspend fun tile(key: TileKey): MapFrameTile? {
        val bytes = tiles(key) ?: return null
        val layer = if (isImage(bytes)) rasterFrameLayer(zoomRange) { bytes } else vectorFrameLayer(zoomRange, style, names) { bytes }
        return layer.tile(key)
    }
}

/** Whether [bytes] start like a PNG, a JPEG or a WebP image rather than a vector tile. */
internal fun isImage(bytes: ByteArray): Boolean {
    fun at(i: Int) = if (i < bytes.size) bytes[i].toInt() and 0xFF else -1
    val png = at(0) == 0x89 && at(1) == 0x50 && at(2) == 0x4E && at(3) == 0x47
    val jpeg = at(0) == 0xFF && at(1) == 0xD8 && at(2) == 0xFF
    val webp = at(0) == 'R'.code && at(1) == 'I'.code && at(2) == 'F'.code && at(3) == 'F'.code &&
        at(8) == 'W'.code && at(9) == 'E'.code && at(10) == 'B'.code && at(11) == 'P'.code
    return png || jpeg || webp
}
