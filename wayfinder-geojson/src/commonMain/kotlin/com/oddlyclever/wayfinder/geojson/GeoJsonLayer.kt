package com.oddlyclever.wayfinder.geojson

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coderwise.libs.mapview.MapOverlayScope
import com.coderwise.libs.mapview.geojson.Feature
import com.coderwise.libs.mapview.geojson.FeatureCollection
import com.coderwise.libs.mapview.geojson.GeoJson
import com.coderwise.libs.mapview.geojson.GeoJsonStyle
import com.coderwise.libs.mapview.geojson.parseGeoJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * GeoJSON text drawn on a `WayfinderMap`, in its overlay: points, lines and polygons with holes, tappable.
 *
 * The text is parsed once and kept while it is unchanged; text that is not GeoJSON draws nothing rather than failing.
 * Below [minZoom] nothing is drawn. GeoJSON from another library, such as H3 cell outlines, comes in as text, so that
 * library needs no dependency on wayfinder.
 */
@Composable
fun MapOverlayScope.GeoJsonLayer(
    geoJson: String,
    style: GeoJsonStyle = GeoJsonStyle(),
    minZoom: Float = 0f,
    touchWidth: Dp = 24.dp,
    onFeatureClick: ((Feature) -> Unit)? = null,
) {
    val data = remember(geoJson) { parseOrNull(geoJson) } ?: return
    GeoJson(data, style = style, minZoom = minZoom, touchWidth = touchWidth, onFeatureClick = onFeatureClick)
}

/**
 * GeoJSON fetched from [url] with the app's [load] and drawn on a `WayfinderMap`, in its overlay. Nothing is drawn until
 * it arrives, or if [load] returns null, throws, or gives text that is not GeoJSON: a network failure is no layer, not a
 * crash, so [load] needs no catch of its own. It is fetched again only when [url] changes.
 */
@Composable
fun MapOverlayScope.RemoteGeoJsonLayer(
    url: String,
    load: suspend (url: String) -> ByteArray?,
    style: GeoJsonStyle = GeoJsonStyle(),
    minZoom: Float = 0f,
    touchWidth: Dp = 24.dp,
    onFeatureClick: ((Feature) -> Unit)? = null,
) {
    var data by remember(url) { mutableStateOf<FeatureCollection?>(null) }
    LaunchedEffect(url) {
        data = loadGeoJson(url, load)
    }
    val loaded = data ?: return
    GeoJson(loaded, style = style, minZoom = minZoom, touchWidth = touchWidth, onFeatureClick = onFeatureClick)
}

/** What [load] gives for [url], parsed off the main thread, or null when it gives nothing, throws, or is not GeoJSON. */
internal suspend fun loadGeoJson(url: String, load: suspend (url: String) -> ByteArray?): FeatureCollection? {
    val bytes = try {
        load(url)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    } ?: return null
    return withContext(Dispatchers.Default) { parseOrNull(bytes.decodeToString()) }
}

/** [geoJson] parsed, or null for text that is not GeoJSON. */
fun parseGeoJsonOrNull(geoJson: String): FeatureCollection? = parseOrNull(geoJson)

private fun parseOrNull(text: String): FeatureCollection? = try {
    parseGeoJson(text)
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}
