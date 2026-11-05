// Derived from map-view-controls 0.3.0, part of map-view (https://github.com/coderwise/libs-mobile), licensed under
// the Apache License 2.0. Modified: moved into com.oddlyclever.wayfinder.controls and made internal to wayfinder, with
// ScaleUnits moved to com.oddlyclever.wayfinder, and the label written in the reader's locale by aughtone's
// format-readable rather than by hand. See the module README.

package com.oddlyclever.wayfinder.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coderwise.libs.mapview.MapCameraState
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import com.oddlyclever.wayfinder.ScaleUnits
import io.github.aughtone.readable.metrics.formatReadableMetric
import io.github.aughtone.types.locale.Locale
import io.github.aughtone.types.units.UnitOfMeasure

/**
 * A bar and what it measures: metres and kilometres, or feet and miles, rounded down to a number
 * a person would say — 1, 2 or 5 of something — so that the bar is never wider than [maxWidth].
 *
 * True at the middle of the map. On a Mercator map the same distance is a longer bar towards the
 * poles, so a bar read at the edge of a tall map is only an approximation, as it is on paper.
 */
@Composable
internal fun ScaleBar(
    camera: MapCameraState,
    modifier: Modifier = Modifier,
    units: ScaleUnits = ScaleUnits.Metric,
    maxWidth: Dp = 120.dp,
    color: Color = Color.Black,
    background: Color = Color.White.copy(alpha = 0.75f),
    textStyle: TextStyle = TextStyle(fontSize = 11.sp)
) {
    val locale = remember { Locale.current }
    // Derived, so the bar recomposes when what it shows changes and not on every pixel of a pan.
    val sized by remember(camera, units, maxWidth, locale) {
        derivedStateOf { scaleBarSize(camera.metersPerDp, maxWidth.value, units, locale) }
    }
    val bar = sized ?: return

    Column(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        BasicText(bar.label, style = textStyle.copy(color = color))
        // An open bracket on the ground: two ticks and the line between them.
        Box(
            Modifier
                .width(bar.width.dp)
                .height(5.dp)
                .drawBehind {
                    val stroke = 1.5.dp.toPx()
                    drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), stroke)
                    drawLine(color, Offset(stroke / 2, 0f), Offset(stroke / 2, size.height), stroke)
                    drawLine(color, Offset(size.width - stroke / 2, 0f), Offset(size.width - stroke / 2, size.height), stroke)
                }
        )
    }
}

/** A bar [width] dp wide, saying what it measures in [label]. */
internal data class ScaleBarSize(val width: Float, val label: String)

/**
 * The bar for a map showing [metersPerDp], no wider than [maxWidth] dp; null where there is nothing
 * sensible to draw.
 *
 * Metric picks its own prefix, so metres give way to kilometres at a thousand and to millimetres below one. Feet give
 * way to miles at a mile, which is where a person would stop counting in the smaller one. The number is written the
 * way [locale] writes numbers, with its own digit grouping and decimal separator, so `1,000 ft` in English is
 * `1 000 ft` in French.
 */
internal fun scaleBarSize(metersPerDp: Double, maxWidth: Float, units: ScaleUnits, locale: Locale): ScaleBarSize? {
    if (!(metersPerDp > 0.0) || !metersPerDp.isFinite() || !(maxWidth > 0f)) return null
    val maxMeters = metersPerDp * maxWidth
    val (meters, label) = when (units) {
        ScaleUnits.Metric -> niceFloor(maxMeters).let { it to it.formatReadableMetric(UnitOfMeasure.Meter, locale, LABEL_PRECISION) }

        ScaleUnits.Imperial -> {
            val maxFeet = maxMeters / METERS_PER_FOOT
            if (maxFeet >= FEET_PER_MILE) niceFloor(maxFeet / FEET_PER_MILE)
                .let { it * FEET_PER_MILE * METERS_PER_FOOT to it.formatReadableMetric(UnitOfMeasure.Mile, locale, LABEL_PRECISION) }
            else niceFloor(maxFeet).let { it * METERS_PER_FOOT to it.formatReadableMetric(UnitOfMeasure.Foot, locale, LABEL_PRECISION) }
        }
    }
    return ScaleBarSize((meters / metersPerDp).toFloat(), label)
}

/** The largest of 1, 2 and 5 times a power of ten that is no more than [x]. */
internal fun niceFloor(x: Double): Double {
    val magnitude = 10.0.pow(floor(log10(x)))
    val leading = x / magnitude
    return magnitude * when {
        leading >= 5 -> 5.0
        leading >= 2 -> 2.0
        else -> 1.0
    }
}

// Enough places for a bar a fraction of a foot long. Trailing zeros are dropped, so a whole number shows none.
private const val LABEL_PRECISION = 2
private const val METERS_PER_FOOT = 0.3048
private const val FEET_PER_MILE = 5280.0
