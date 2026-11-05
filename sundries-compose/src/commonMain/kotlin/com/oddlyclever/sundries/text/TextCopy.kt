package com.oddlyclever.sundries.text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * What [TextCopy] says, in the app's own words: the label a screen reader reads, which is also the tooltip, for each
 * [CopyState].
 *
 * Say what is copied, as in "Copy the handle", because that is all a screen reader user hears. The defaults are
 * English and generic; an app with its own wording or another language passes its own.
 */
@Immutable
data class CopyStrings(
    val copy: String = "Copy",
    val copied: String = "Copied",
    val refused: String = "Couldn't copy. Select the text and copy it yourself.",
) {
    /** The sentence for [state]. */
    fun said(state: CopyState): String = when (state) {
        CopyState.Ready -> copy
        CopyState.Copied -> copied
        CopyState.Refused -> refused
    }
}

/**
 * Text that copies itself when tapped: it looks like [Text], with a copy icon at its end that turns to a tick once
 * the text is on the clipboard, and to a warning in the error colour when the platform would not take it.
 *
 * [copies] is what goes on the clipboard and defaults to [text], so a cell showing `Name · handle` can copy only the
 * handle. The whole of it is one button to a screen reader, labelled by [strings], and the outcome is announced.
 * Pass a [copier] to share its state with something else on screen; otherwise it has its own.
 *
 * It fits a table cell: [maxLines] and [overflow] apply to the text, and the icon is sized to the text, so the row
 * is no taller than a line. That also means the target is only as big as the text; give it room with [modifier] where
 * it stands alone on a touch screen.
 */
@Composable
fun TextCopy(
    text: String,
    modifier: Modifier = Modifier,
    copies: String = text,
    strings: CopyStrings = CopyStrings(),
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    copier: Copier = rememberCopier(),
) = TextCopy(AnnotatedString(text), modifier, copies, strings, color, style, maxLines, overflow, copier)

/** [TextCopy] for styled text. What it copies, [copies], is plain text, and defaults to the text without its styles. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextCopy(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    copies: String = text.text,
    strings: CopyStrings = CopyStrings(),
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    copier: Copier = rememberCopier(),
) {
    val state = copier.state
    val said = strings.said(state)
    val iconSize = with(LocalDensity.current) { (style.fontSize.takeIf { it.isSpecified } ?: 14.sp).toDp() + 2.dp }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(said) } },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        Row(
            Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .clickable(onClickLabel = strings.copy, role = Role.Button) { copier.copy(copies) }
                .semantics(mergeDescendants = true) {
                    contentDescription = said
                    liveRegion = LiveRegionMode.Polite
                },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, Modifier.weight(1f, fill = false), color = color, style = style, maxLines = maxLines, overflow = overflow)
            Icon(
                imageVector = when (state) {
                    CopyState.Ready -> CopyIcons.Copy
                    CopyState.Copied -> CopyIcons.Copied
                    CopyState.Refused -> CopyIcons.Refused
                },
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = when {
                    state == CopyState.Refused -> MaterialTheme.colorScheme.error
                    color.isSpecified -> color
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
