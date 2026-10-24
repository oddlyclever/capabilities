package com.oddlyclever.wayfinder.vector

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.coderwise.libs.mapview.tiles.vector.LayerStyle
import com.coderwise.libs.mapview.tiles.vector.VectorStyle

/**
 * The two colours of a road: the [casing] that edges it and the [core] drawn over the casing. The contrast between
 * them is what makes a road read as one.
 */
data class RoadColors(val casing: Color, val core: Color)

/**
 * Every colour a vector map is drawn in, and nothing else.
 *
 * A palette recolours a [VectorStyle] with [recolored] and leaves everything that is not colour alone: the road
 * widths that grow with zoom, the zooms at which casings and building outlines appear, the paint order. Those are
 * map-view's, tuned against real tiles, and a dark map wants the same hierarchy in different ink.
 *
 * Roads are coloured by tier, an importance ramp from [motorway] down to [service]; [path], [rail] and [ferry] are
 * the lines OpenMapTiles files among the roads that are not roads.
 */
data class MapPalette(
    /** The land, which in OpenMapTiles is not a layer but what shows where nothing else is drawn. */
    val land: Color,
    val water: Color,
    /** Woods, grass, parks and wetland. */
    val greenery: Color,
    /** Sand, rock, ice and farmland: ground that is not green. */
    val bareGround: Color,
    /** The tint for built-up land use: residential, commercial, industrial. */
    val builtUp: Color,
    val building: Color,
    val buildingOutline: Color,
    val airport: Color,
    val boundary: Color,
    /** Place and road names, and the [halo] drawn under them so a name survives a road beneath it. */
    val ink: Color,
    val halo: Color,
    /** Lake and river names. */
    val waterInk: Color,
    val motorway: RoadColors,
    val major: RoadColors,
    val tertiary: RoadColors,
    val minor: RoadColors,
    val service: RoadColors,
    val path: Color,
    val rail: Color,
    val ferry: Color,
) {
    companion object {
        /**
         * A controlled night map. Roads read as a brightness ramp of muted warm greys with no pure white, so side
         * streets do not overpower the map, and the one warm accent is spent on motorways. Cool water, near-black
         * land, a hint of green.
         */
        val Dark = MapPalette(
            land = Color(0xFF16181D),
            water = Color(0xFF12283C),
            greenery = Color(0xFF17251B),
            bareGround = Color(0xFF1E1F22),
            builtUp = Color(0xFF1A1C21),
            building = Color(0xFF21242B),
            buildingOutline = Color(0xFF2C3038),
            airport = Color(0xFF1C1E24),
            boundary = Color(0xFF5A5C66),
            ink = Color(0xFFCFC8BE),
            halo = Color(0xFF16181D),
            waterInk = Color(0xFF7FA6C2),
            motorway = RoadColors(casing = Color(0xFF6E4327), core = Color(0xFFE0A55E)),
            major = RoadColors(casing = Color(0xFF4A4E57), core = Color(0xFF9A9488)),
            tertiary = RoadColors(casing = Color(0xFF3E424B), core = Color(0xFF7C776D)),
            minor = RoadColors(casing = Color(0xFF2E323A), core = Color(0xFF5E606A)),
            service = RoadColors(casing = Color(0xFF2A2D34), core = Color(0xFF4A4C54)),
            path = Color(0xFF40423F),
            rail = Color(0xFF4A4C54),
            ferry = Color(0xFF2E5470),
        )

        /**
         * A daytime map with a warm accent on motorways and neutral grey casings that darken with importance, so
         * side streets recede. An alternative to map-view's own default style, which [MapStyles.Light] is.
         */
        val Light = MapPalette(
            land = Color(0xFFECE9E2),
            water = Color(0xFFB4CFDF),
            greenery = Color(0xFFD9E6CE),
            bareGround = Color(0xFFE6E1D6),
            builtUp = Color(0xFFE6E2DA),
            building = Color(0xFFE0DCD4),
            buildingOutline = Color(0xFFCFC9BF),
            airport = Color(0xFFE6E4DF),
            boundary = Color(0xFF9E9AA3),
            ink = Color(0xFF4A4640),
            halo = Color(0xFFF7F5F0),
            waterInk = Color(0xFF2F5D78),
            motorway = RoadColors(casing = Color(0xFFC0772E), core = Color(0xFFF2B35F)),
            major = RoadColors(casing = Color(0xFF9B968D), core = Color(0xFFFFFFFF)),
            tertiary = RoadColors(casing = Color(0xFFB2ADA4), core = Color(0xFFFFFFFF)),
            minor = RoadColors(casing = Color(0xFFC9C4BB), core = Color(0xFFFFFFFF)),
            service = RoadColors(casing = Color(0xFFD4CFC6), core = Color(0xFFE2DED6)),
            path = Color(0xFFC6BFB2),
            rail = Color(0xFFB4AEA5),
            ferry = Color(0xFF6E9DB3),
        )
    }
}

/**
 * This style in [palette]'s colours, with every width, zoom gate and paint order kept.
 *
 * Each lookup of the new style asks this one for its paint and recolours what comes back, so a class this style does
 * not draw stays undrawn, and one it draws keeps its weight.
 *
 * Build it once and keep it. A [VectorStyle] is compared by its lookups, and this makes new ones on every call, so a
 * style rebuilt inside a composition would look new on every frame and throw away anything keyed on it.
 * [MapStyles] holds the ones built for you.
 */
fun VectorStyle.recolored(palette: MapPalette): VectorStyle {
    val base = this
    return copy(
        background = palette.land,
        ink = palette.ink,
        halo = palette.halo,
        waterInk = palette.waterInk,
        water = base.water?.tinted(palette.water),
        waterway = base.waterway?.tinted(palette.water),
        park = base.park?.tinted(palette.greenery),
        building = base.building?.let { it.copy(fill = it.fill?.let { palette.building }, stroke = it.stroke?.let { palette.buildingOutline }) },
        roadCasing = base.roadCasing?.tinted(palette.minor.casing),
        landcover = { kind -> base.landcover(kind)?.tinted(if (kind in BARE_GROUND) palette.bareGround else palette.greenery) },
        landuse = { kind -> base.landuse(kind)?.tinted(if (kind in GREEN_LAND_USE) palette.greenery else palette.builtUp) },
        road = { kind -> base.road(kind)?.let { paint -> roadPaint(kind, paint, palette) } },
        aeroway = { kind -> base.aeroway(kind)?.tinted(palette.airport) },
        boundary = { level -> base.boundary(level)?.tinted(palette.boundary) },
    )
}

/** The styles wayfinder builds, each made once so it compares equal to itself across compositions. */
object MapStyles {
    /** map-view's own default style, which is a daytime map. */
    val Light: VectorStyle = VectorStyle()

    /** map-view's default style in [MapPalette.Dark]. */
    val Dark: VectorStyle = VectorStyle().recolored(MapPalette.Dark)
}

/**
 * [dark] when the system is in dark mode and [light] otherwise, following the setting as it changes.
 *
 * A convenience, not a rule: a map that should stay light at night, or follow the app's own theme instead of the
 * system's, passes the style it wants to its tiles directly.
 */
@Composable
fun systemVectorStyle(light: VectorStyle = MapStyles.Light, dark: VectorStyle = MapStyles.Dark): VectorStyle =
    if (isSystemInDarkTheme()) dark else light

/** A road paint in the palette's colours: casing and core for a road, the line's own colour for a track. */
private fun roadPaint(kind: String, paint: LayerStyle, palette: MapPalette): LayerStyle {
    val line = when (kind) {
        "track", "path", "footway", "cycleway", "pedestrian", "steps" -> palette.path
        "rail", "transit" -> palette.rail
        "ferry" -> palette.ferry
        else -> null
    }
    if (line != null || paint.casing == null) return paint.tinted(line ?: palette.minor.core)
    val tier = when (kind) {
        "motorway", "trunk" -> palette.motorway
        "primary", "secondary" -> palette.major
        "tertiary" -> palette.tertiary
        "service" -> palette.service
        else -> palette.minor
    }
    return paint.copy(stroke = tier.core, casing = tier.casing, coarse = paint.coarse?.let { tier.casing })
}

/** Every colour this paint has, replaced by [color]; a part it does not paint stays unpainted. */
private fun LayerStyle.tinted(color: Color): LayerStyle = copy(
    fill = fill?.let { color },
    stroke = stroke?.let { color },
    casing = casing?.let { color },
    coarse = coarse?.let { color },
)

private val BARE_GROUND = setOf("sand", "beach", "ice", "glacier", "snow", "rock", "bare_rock", "scree", "farmland", "farm", "orchard", "vineyard")
private val GREEN_LAND_USE = setOf("cemetery", "recreation_ground", "dog_park", "park", "grass", "meadow")
