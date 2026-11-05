package com.oddlyclever.wayfinder

/**
 * What the scale bar counts in, set by [MapOrnaments.scaleUnits]: metres and kilometres, or feet and miles.
 *
 * It belongs to wayfinder rather than to the ornament that draws it, so a screen can choose units, from a setting or a
 * locale, without depending on how the scale bar is built.
 */
enum class ScaleUnits { Metric, Imperial }
