package com.oddlyclever.sundries

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * This text size in dp at the current density and font scale, for sizing something to match a line of text.
 *
 * Composable because the answer depends on [LocalDensity]; outside a composition, use `Density.toDp` directly.
 */
@Composable
fun TextUnit.toDp(): Dp = with(LocalDensity.current) { toDp() }

/** This many pixels in dp at the current density, for a size measured in pixels, such as a layout result. */
@Composable
fun Int.toDp(): Dp = (this / LocalDensity.current.density).dp

/** This many pixels in dp at the current density, for a size measured in pixels, such as a layout result. */
@Composable
fun Float.toDp(): Dp = (this / LocalDensity.current.density).dp

/** This size as a text size at the current density and font scale, for text that has to fill a known space. */
@Composable
fun Dp.toTextUnit(): TextUnit = with(LocalDensity.current) { toSp() }
