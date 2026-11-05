package com.oddlyclever.h3spatial

import kotlin.math.roundToLong

data class H3LatLng(val lat: Double, val lng: Double)

/**
 * The single failure type of [H3SpatialService] on every target.
 *
 * Each platform binds H3 by a different mechanism with its own idea of failure — a Java exception,
 * a C error code, a JS throw — and some (h3-js) return sentinel values instead of failing. Without
 * a common type the failure contract would differ per target silently, and a caller must be able to
 * catch one exception and know invalid input is covered on Android, iOS, JVM, JS and wasmJs alike.
 *
 * @param code The H3 core error code (1..15, see h3geo.org/docs/library/errors) when the platform
 *   supplied one; `null` when the failure did not originate in the H3 core library.
 */
class H3SpatialException(
    message: String,
    val code: Int? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * The public H3 surface: `String` cell indexes and [H3LatLng], one vocabulary on all five targets,
 * so a consumer's positional logic can live in shared code rather than per platform.
 *
 * Index strings are always **canonical H3 form** — unpadded lowercase hex, the form produced by
 * the H3 C library, `h3-js` and `com.uber:h3` — so indexes computed here compare equal to indexes
 * from any standard H3 tool and from this service on every other target. Inputs tolerate a
 * zero-padded form as well.
 *
 * Coordinates are degrees, rounded to eight decimal places (about a millimetre). Each platform's H3
 * computes them in its own floating-point environment, and rounding is what makes a cell's centre or
 * boundary compare equal across targets rather than differ in the last few bits.
 *
 * Failure contract: every function throws [H3SpatialException] on invalid input — except
 * [isValidCell], which is the designated no-throw validity check. Validation happens here, in common
 * code, before any platform call, so every target rejects the same inputs with the same error code.
 * Resolution is always the caller's choice; there are no defaults, because the right cell size
 * depends on what the caller is bucketing.
 *
 * Swift and Objective-C see the throwing functions as `throws` because of [Throws]; without it
 * Kotlin/Native would terminate the process instead of bridging the exception.
 */
class H3SpatialService {

    /** The cell containing a point. */
    @Throws(H3SpatialException::class)
    fun latLngToH3(lat: Double, lng: Double, resolution: Int): String {
        requireResolution(resolution)
        // The C library silently normalizes out-of-range coordinates; a valid-looking index at the
        // wrong place is worse than an error.
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            throw H3SpatialException("Latitude or longitude out of range: $lat, $lng", E_LATLNG_DOMAIN)
        }
        return translate { H3Native.latLngToCell(lat, lng, resolution) }.toCanonical()
    }

    /**
     * The cell's boundary, as H3 orders it: counter-clockwise, and not closed. A hexagon has six vertices and a pentagon
     * five at even resolutions; at odd ones H3 adds a vertex wherever the edge crosses an icosahedron face, so a
     * hexagon can have seven or eight and a pentagon ten. Size anything by the list, not by [isPentagon].
     */
    @Throws(H3SpatialException::class)
    fun h3ToPolygon(h3Index: String): List<H3LatLng> {
        val cell = validCell(h3Index)
        return translate { H3Native.cellToBoundary(cell) }.map { it.rounded() }
    }

    /** The cell's resolution, 0 (coarsest) to 15 (finest). */
    @Throws(H3SpatialException::class)
    fun getResolution(h3Index: String): Int = resolutionOf(validCell(h3Index))

    /** The centre of the cell — the inverse of [latLngToH3]. */
    @Throws(H3SpatialException::class)
    fun cellToLatLng(h3Index: String): H3LatLng {
        val cell = validCell(h3Index)
        return translate { H3Native.cellToLatLng(cell) }.rounded()
    }

    /** Whether the string is a valid H3 cell index. Never throws, even on unparseable input. */
    fun isValidCell(h3Index: String): Boolean {
        val cell = parseIndex(h3Index) ?: return false
        return try {
            H3Native.isValidCell(cell)
        } catch (e: Error) {
            throw e // infrastructure failure (native library load, out of memory) — not "invalid cell"
        } catch (_: Throwable) {
            false
        }
    }

    /** Whether the cell is one of the twelve pentagons at its resolution. */
    @Throws(H3SpatialException::class)
    fun isPentagon(h3Index: String): Boolean {
        val cell = validCell(h3Index)
        return translate { H3Native.isPentagon(cell) }
    }

    /**
     * All cells within grid distance [k] of [h3Index], including the cell itself.
     *
     * The result size is NOT fixed: a full disk has `3k(k+1)+1` cells, but disks touching one of
     * the twelve pentagons per resolution are smaller (a pentagon has five neighbours, not six).
     * Never assume 7 cells at k=1.
     */
    @Throws(H3SpatialException::class)
    fun gridDisk(h3Index: String, k: Int): List<String> {
        val cell = validCell(h3Index)
        // Upper bound as well as lower: a disk too large for an Int-indexed list cannot be returned
        // on any target, and the native targets size their output buffers from it. Bounded here so
        // every target fails the same way instead of each failing differently or not at all.
        if (k < 0 || maxGridDiskSize(k) > Int.MAX_VALUE) {
            throw H3SpatialException("k out of range: $k", E_DOMAIN)
        }
        return translate { H3Native.gridDisk(cell, k) }.map { it.toCanonical() }
    }

    /** Distance between two same-resolution cells, in cells — proximity without projecting to metres. */
    @Throws(H3SpatialException::class)
    fun gridDistance(origin: String, destination: String): Long {
        val from = validCell(origin)
        val to = validCell(destination)
        requireSameResolution(from, to)
        return translate { H3Native.gridDistance(from, to) }
    }

    /** Whether two same-resolution cells border each other. A cell is not its own neighbour. */
    @Throws(H3SpatialException::class)
    fun areNeighborCells(origin: String, destination: String): Boolean {
        val from = validCell(origin)
        val to = validCell(destination)
        requireSameResolution(from, to)
        return translate { H3Native.areNeighborCells(from, to) }
    }

    /** The coarser cell containing this cell at [parentResolution]. */
    @Throws(H3SpatialException::class)
    fun cellToParent(h3Index: String, parentResolution: Int): String {
        val cell = validCell(h3Index)
        requireResolution(parentResolution)
        if (parentResolution > resolutionOf(cell)) {
            throw H3SpatialException("parentResolution $parentResolution is finer than the cell", E_RES_DOMAIN)
        }
        return translate { H3Native.cellToParent(cell, parentResolution) }.toCanonical()
    }

    /**
     * The finer cells of this cell at [childResolution] — `7^(childResolution - res)` for a hexagon.
     *
     * At most [MAX_CHILD_RESOLUTION_STEPS] resolutions finer: one step more and a hexagon has more
     * children than an Int-indexed list can hold.
     */
    @Throws(H3SpatialException::class)
    fun cellToChildren(h3Index: String, childResolution: Int): List<String> {
        val cell = validCell(h3Index)
        requireResolution(childResolution)
        val resolution = resolutionOf(cell)
        if (childResolution < resolution) {
            throw H3SpatialException("childResolution $childResolution is coarser than the cell", E_RES_DOMAIN)
        }
        if (childResolution - resolution > MAX_CHILD_RESOLUTION_STEPS) {
            throw H3SpatialException(
                "childResolution $childResolution is too many steps finer than the cell to list",
                E_MEMORY_BOUNDS,
            )
        }
        return translate { H3Native.cellToChildren(cell, childResolution) }.map { it.toCanonical() }
    }

    companion object {
        /** How many resolutions finer than a cell [cellToChildren] will list: `7^11` fits an Int, `7^12` does not. */
        const val MAX_CHILD_RESOLUTION_STEPS = 11
    }
}

// H3 core error codes used for uniform pre-validation (h3geo.org/docs/library/errors).
private const val E_DOMAIN = 2
private const val E_LATLNG_DOMAIN = 3
private const val E_RES_DOMAIN = 4
private const val E_CELL_INVALID = 5
private const val E_RES_MISMATCH = 12
private const val E_MEMORY_BOUNDS = 14

private const val COORDINATE_SCALE = 100_000_000.0

/**
 * Runs a platform call, translating any failure into [H3SpatialException]. Inputs are validated
 * before platform calls, so this is a backstop for failures only the H3 library itself can detect,
 * such as pentagon distortion in [H3SpatialService.gridDistance].
 *
 * [Error]s (native library failed to load, out of memory) are infrastructure, not invalid input —
 * they pass through untranslated so callers don't mistake a broken runtime for a bad index.
 */
private inline fun <T> translate(block: () -> T): T = try {
    block()
} catch (e: H3SpatialException) {
    throw e
} catch (e: Error) {
    throw e
} catch (e: Throwable) {
    throw H3SpatialException(e.message ?: "H3 call failed", null, e)
}

/**
 * The index as a number, or null when the text is not one. Only hex digits are accepted — no sign,
 * no `0x` prefix — because each platform's own parser tolerates a different set of variants, and the
 * same text must mean the same cell everywhere. Up to 16 digits, so a zero-padded form parses too.
 */
private fun parseIndex(text: String): ULong? {
    if (text.isEmpty() || text.length > 16) return null
    if (!text.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
    return text.toULongOrNull(radix = 16)
}

/** Parses and validates a cell index, or throws [H3SpatialException]. */
private fun validCell(h3Index: String): ULong {
    val cell = parseIndex(h3Index) ?: throw H3SpatialException("Not an H3 index: '$h3Index'", E_CELL_INVALID)
    if (!translate { H3Native.isValidCell(cell) }) {
        throw H3SpatialException("Not a valid H3 cell: '$h3Index'", E_CELL_INVALID)
    }
    return cell
}

private fun requireResolution(resolution: Int) {
    if (resolution !in 0..15) {
        throw H3SpatialException("resolution $resolution is out of range (must be 0 <= res <= 15)", E_RES_DOMAIN)
    }
}

/** Two-cell operations require equal resolutions; checked here so every target fails alike. */
private fun requireSameResolution(origin: ULong, destination: ULong) {
    if (resolutionOf(origin) != resolutionOf(destination)) {
        throw H3SpatialException("Cells are at different resolutions", E_RES_MISMATCH)
    }
}

/** A valid cell's resolution, read from bits 52–55 of the index as the H3 index format defines. */
private fun resolutionOf(cell: ULong): Int = ((cell shr 52) and 0xFUL).toInt()

/** The size of a full disk of radius [k]: `3k(k+1)+1`. Computed here because h3-js does not expose it. */
private fun maxGridDiskSize(k: Int): Long = 3L * k * (k + 1L) + 1

/** Canonical H3 text: unpadded lowercase hex. */
internal fun ULong.toCanonical(): String = toString(radix = 16)

private fun H3LatLng.rounded(): H3LatLng = H3LatLng(lat.roundCoordinate(), lng.roundCoordinate())

private fun Double.roundCoordinate(): Double = (this * COORDINATE_SCALE).roundToLong() / COORDINATE_SCALE
