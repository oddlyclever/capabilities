package com.oddlyclever.wayfinder.geojson

import io.github.aughtone.types.geo.GeoFeature
import io.github.aughtone.types.geo.GeoFeatureCollection
import io.github.aughtone.types.geo.GeoGeometry
import io.github.aughtone.types.geo.GeoJson
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/**
 * Reads GeoJSON out of [json] into aughtone-types' model: a FeatureCollection, a single Feature, or a bare geometry, which
 * comes back as a collection of one feature without properties.
 *
 * Fetching the text is the caller's. Throws [IllegalArgumentException] with the reason for JSON that is not JSON and for
 * JSON that is not GeoJSON as RFC 7946 defines it: an unknown geometry type, a position without a latitude, a line of
 * one position, or a polygon ring that is not closed or has fewer than four positions. One bad geometry fails the whole
 * document. Members the specification does not define, such as `crs`, are ignored, and a feature without a `geometry`
 * or `properties` member is read as one whose value is `null`. A byte order mark before the document is skipped.
 *
 * [parseGeoJsonOrNull] is the same without the exception, which is what the layers use.
 */
fun parseGeoJson(json: String): GeoFeatureCollection =
    when (val document = geoJsonFormat.decodeFromString(GeoJson.serializer(), json.removePrefix(BYTE_ORDER_MARK))) {
        is GeoFeatureCollection -> document
        is GeoFeature -> GeoFeatureCollection(listOf(document))
        is GeoGeometry -> GeoFeatureCollection(listOf(GeoFeature(geometry = document)))
    }

/** [geoJson] parsed by [parseGeoJson], or null for text that is not GeoJSON. */
fun parseGeoJsonOrNull(geoJson: String): GeoFeatureCollection? = try {
    parseGeoJson(geoJson)
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}

/**
 * GeoJSON's own reading of JSON: `type` names the kind of object, as aughtone-types' model expects, and anything a
 * producer adds beyond the specification is skipped rather than refused.
 */
private val geoJsonFormat = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** What some editors write at the start of a UTF-8 file, which is not JSON and which kotlinx.serialization refuses. */
private const val BYTE_ORDER_MARK = "\uFEFF"
