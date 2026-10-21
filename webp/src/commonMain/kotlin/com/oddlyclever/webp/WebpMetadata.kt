package com.oddlyclever.webp

import kotlin.time.Instant

/**
 * What a WebP says about itself, written as EXIF.
 *
 * The fields are the ones an app that saves photos and map images needs to keep with the image: where and when, and
 * what made it. Every field is optional, and a metadata with none set writes nothing.
 *
 * @property location where the image was taken or what it shows, written as EXIF GPS tags.
 * @property capturedAt when, written as the EXIF original date and time in UTC with a `+00:00` offset, and as the
 *   GPS date and time stamp when [location] is set too.
 * @property software what made the image, such as an app's name.
 */
class WebpMetadata(
    val location: GpsLocation? = null,
    val capturedAt: Instant? = null,
    val software: String? = null,
) {
    /** Whether there is nothing to write. */
    val isEmpty: Boolean get() = location == null && capturedAt == null && software.isNullOrEmpty()

    override fun equals(other: Any?): Boolean =
        other is WebpMetadata && location == other.location && capturedAt == other.capturedAt && software == other.software

    override fun hashCode(): Int = 31 * (31 * location.hashCode() + capturedAt.hashCode()) + software.hashCode()

    override fun toString(): String = "WebpMetadata(location=$location, capturedAt=$capturedAt, software=$software)"
}

/**
 * A position on the Earth in WGS 84, the datum GPS and EXIF use.
 *
 * Kept to this module rather than borrowed from a geometry library, so writing a WebP needs no other dependency.
 * Degrees are written to about a millimetre.
 *
 * @property latitude degrees north, -90 to 90.
 * @property longitude degrees east, -180 to 180.
 * @property altitude metres above sea level, negative below it, or null when unknown.
 */
class GpsLocation(val latitude: Double, val longitude: Double, val altitude: Double? = null) {
    init {
        require(latitude in -90.0..90.0) { "latitude $latitude is outside -90..90" }
        require(longitude in -180.0..180.0) { "longitude $longitude is outside -180..180" }
        require(altitude == null || altitude.isFinite()) { "altitude $altitude is not a number of metres" }
    }

    override fun equals(other: Any?): Boolean =
        other is GpsLocation && latitude == other.latitude && longitude == other.longitude && altitude == other.altitude

    override fun hashCode(): Int = 31 * (31 * latitude.hashCode() + longitude.hashCode()) + altitude.hashCode()

    override fun toString(): String = "GpsLocation(latitude=$latitude, longitude=$longitude, altitude=$altitude)"
}
