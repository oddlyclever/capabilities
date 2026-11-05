package com.oddlyclever.sundries

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Exposes the test tags under this element as Android resource ids, so tools that drive the app from outside, such
 * as UI Automator or Maestro, can find components by id.
 *
 * Apply it once near the root, with [enabled] tied to a build that is not for release; it returns this modifier
 * unchanged when [enabled] is false. Android only, because resource ids exist only there, so there is no common
 * form to call from shared code.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.exposeTestTagsAsResourceIds(enabled: Boolean): Modifier =
    if (enabled) semantics { testTagsAsResourceId = true } else this
