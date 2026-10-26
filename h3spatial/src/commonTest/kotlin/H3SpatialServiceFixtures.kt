import com.oddlyclever.h3spatial.H3LatLng

// Expected values for H3SpatialServiceSpec, computed with Uber's h3-js 4.5.0 — an implementation
// independent of every binding under test — and rounded to the eight decimal places the service
// returns. Index strings are canonical H3 form: unpadded lowercase hex.

/** The resolution-4 cell containing latitude 0, longitude 0. */
internal const val zeroLatLngRes4 = "84754a9ffffffff"

/** [zeroLatLngRes4] zero-padded to 16 digits, a form the service must accept. */
internal const val paddedZeroLatLngRes4 = "084754a9ffffffff"

/** Ottawa: an asymmetric point, so a binding that transposes latitude and longitude fails. */
internal const val ottawaLat = 45.4215
internal const val ottawaLng = -75.6972
internal const val ottawaRes7 = "872b83b0effffff"

/** A cell bordering [zeroLatLngRes4], and one two steps away from it. */
internal const val neighborOfZeroLatLngRes4 = "8475485ffffffff"
internal const val notNeighborOfZeroLatLngRes4 = "8475481ffffffff"

internal val zeroBoundary = listOf(
    H3LatLng(-0.34749776, 0.04469065),
    H3LatLng(-0.1490766, 0.13959856),
    H3LatLng(0.0197745, 0.04056896),
    H3LatLng(-0.00969098, -0.15269442),
    H3LatLng(-0.20742123, -0.247275),
    H3LatLng(-0.37637541, -0.14892008),
)

internal val zeroCenter = H3LatLng(-0.1783007, -0.05417454)

/** [zeroLatLngRes4]'s k=1 disk, sorted. */
internal val gridDisk1OfZero = listOf(
    "8475485ffffffff",
    "84754a1ffffffff",
    "84754a9ffffffff",
    "84754abffffffff",
    "84754adffffffff",
    "84754e3ffffffff",
    "84754e7ffffffff",
)

internal const val parentRes3OfZero = "83754afffffffff"

/** [zeroLatLngRes4]'s resolution-5 children, sorted. */
internal val childrenRes5OfZero = listOf(
    "85754a83fffffff",
    "85754a87fffffff",
    "85754a8bfffffff",
    "85754a8ffffffff",
    "85754a93fffffff",
    "85754a97fffffff",
    "85754a9bfffffff",
)

/** A resolution-4 pentagon, and its k=1 disk sorted: six cells, not seven. */
internal const val pentagonRes4 = "8408001ffffffff"
internal val pentagonDisk1 = listOf(
    "8408001ffffffff",
    "8408005ffffffff",
    "8408007ffffffff",
    "8408009ffffffff",
    "840800bffffffff",
    "840800dffffffff",
)

/** Not hexadecimal at all. */
internal const val invalidIndex = "not-an-h3-index"

/** Well-formed hexadecimal that is not a valid H3 cell. */
internal const val invalidCellHex = "ffffffffffffffff"
