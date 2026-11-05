package com.oddlyclever.sundries

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** The alpha Material gives disabled content, so a colour made disabled by hand matches the built-in components. */
const val DISABLED_ALPHA = 0.38f

/**
 * This string as a colour, or [Color.Unspecified] when it is not one.
 *
 * Accepts `#RRGGBB` and `#AARRGGBB`, with or without the `#`; six digits are opaque. A colour read this way usually
 * comes from configuration or a server, where a bad value should fall back to the theme rather than fail the screen,
 * so it answers [Color.Unspecified] instead of throwing. Use [toColorInt] to be told why a value was refused.
 */
fun String.toColor(): Color = toArgbOrNull()?.let { Color(it) } ?: Color.Unspecified

/**
 * This string as an ARGB colour int: `#RRGGBB` or `#AARRGGBB`, with or without the `#`, six digits being opaque.
 *
 * @throws IllegalArgumentException when the string is neither form. Colour names are not accepted.
 */
fun String.toColorInt(): Int =
    requireNotNull(toArgbOrNull()) { "Not a colour: \"$this\". Expected #RRGGBB or #AARRGGBB." }

/**
 * This colour as `#AARRGGBB` text, always eight lower-case digits after [prefix], so [toColor] reads it back as the
 * same colour.
 */
fun Color.toColorString(prefix: String = "#"): String =
    prefix + (toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/** This colour at [alpha], by default [DISABLED_ALPHA], for content that cannot be used right now. */
fun Color.disabled(alpha: Float = DISABLED_ALPHA): Color = copy(alpha = alpha)

private fun String.toArgbOrNull(): Int? {
    val hex = removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return null
    if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
    val value = hex.toLong(16)
    return (if (hex.length == 6) value or 0xFF000000L else value).toInt()
}
