package com.oddlyclever.wayfinder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coderwise.libs.mapview.MapCameraState
import com.oddlyclever.wayfinder.controls.Attribution
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** How the map shows the credits its sources ask for. */
enum class AttributionStyle {
    /** Always shown in full. */
    Full,

    /**
     * Shown in full when the map appears, then shrunk to an "i" after [ATTRIBUTION_SHOWN_MILLIS] or at the user's first
     * pan, zoom or turn, whichever comes first. Tapping the "i" shows the credits again, by the same rule.
     *
     * This is the form OpenStreetMap's attribution guidelines allow for an interactive map: visible on first display,
     * then collapsed, and still there for anyone who looks for it. A credit hidden from the start is not allowed.
     */
    Collapsible,
}

/** How long a [AttributionStyle.Collapsible] credit stays in full before shrinking to its "i". */
const val ATTRIBUTION_SHOWN_MILLIS: Long = 5_000L

/**
 * The credits line, in full or behind an "i". [onClick] opens the page behind the credit, such as
 * openstreetmap.org/copyright; what opens it is the app's, as wayfinder does not know what a browser is.
 */
@Composable
internal fun MapAttribution(
    credits: String,
    camera: MapCameraState,
    style: AttributionStyle,
    textSize: TextUnit,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    creditsLabel: String = ornamentWords().credits,
) {
    var expanded by remember { mutableStateOf(true) }
    if (style == AttributionStyle.Collapsible && expanded) {
        LaunchedEffect(Unit) {
            // Whichever comes first: the time runs out, or the user starts moving the map.
            coroutineScope {
                val timeout = async { delay(ATTRIBUTION_SHOWN_MILLIS) }
                val gesture = async { snapshotFlow { camera.isInteracting }.first { it } }
                select {
                    timeout.onAwait { }
                    gesture.onAwait { }
                }
                timeout.cancel()
                gesture.cancel()
            }
            expanded = false
        }
    }
    if (style == AttributionStyle.Full || expanded) {
        Attribution(credits, modifier, onClick = onClick, textStyle = TextStyle(fontSize = textSize))
    } else {
        // The "i": a small plate the same colours as the credit it stands for.
        Box(
            modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.75f))
                .clickable { expanded = true }
                .semantics {
                    contentDescription = creditsLabel
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                "i",
                style = TextStyle(color = Color.Black, fontSize = 12.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold),
            )
        }
    }
}
