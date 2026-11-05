package com.oddlyclever.sundries

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

/**
 * Whether the window is at least Material's medium width, 600dp: the usual point to go from one pane to two.
 *
 * It reads the live window size class, so it changes when the window is resized or the device turned. Do not keep
 * the answer across recompositions.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberIsMultiPane(): Boolean =
    currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

/**
 * Whether the window is at least Material's expanded width, 840dp: the step above [rememberIsMultiPane], for a
 * third pane or wider content on a large tablet or a desktop window.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberIsExpanded(): Boolean =
    currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
