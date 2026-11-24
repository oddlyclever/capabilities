package com.oddlyclever.sundries.text

import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.oddlyclever.sundries.ShimmerMotion
import com.oddlyclever.sundries.shimmer

/**
 * Material's [Text] with a band of colour sweeping across its letters, the way an agent's "Thinking…" line shows that
 * work is under way: an indeterminate progress indicator that is also its own label.
 *
 * While [active], it is one to accessibility services as well: its semantics are an indeterminate progress bar's, so a
 * screen reader says the text and that it is in progress. Inactive, it is ordinary text.
 *
 * With nothing set it is the muted look: the text at half its usual colour, and each pass bringing it back to full
 * colour as the band crosses. Pass the app's highlight colour in [colors] for a colour shimmer, or several colours to
 * have each pass take the next. [color] is the text outside the band; keep it dimmer than the band's colours, or the
 * band does not show. The letters are drawn in [color] and the band's colours alone, so colours from [style] or a
 * span do not show; the text measures and wraps exactly as [Text] would.
 *
 * [active], [motion], [sweepMillis], [pauseMillis] and [band] are [shimmer]'s, which says what each does; every other
 * parameter is [Text]'s and passes through. With `active = false` it is plain text in [color], which is what to show
 * once the work is done or where the person has asked for less motion.
 *
 * @throws IllegalArgumentException as [shimmer] does, for a timing or band it cannot use.
 */
@Composable
fun TextShimmer(
    text: String,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(LocalTextStyle.current.color.takeOrElse { LocalContentColor.current }),
    color: Color = LocalTextStyle.current.color.takeOrElse { LocalContentColor.current }.let { it.copy(alpha = it.alpha * 0.5f) },
    active: Boolean = true,
    motion: ShimmerMotion = ShimmerMotion.Sweep,
    sweepMillis: Int = 1_000,
    pauseMillis: Int = 800,
    band: Float = 0.5f,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) = Text(
    text = text,
    // The letters opaque, then recoloured: a base of half alpha over them is faint, and the band over them is full.
    modifier = modifier
        .then(if (active) Modifier.progressSemantics() else Modifier)
        .shimmer(colors, active, motion, sweepMillis, pauseMillis, band, base = color),
    color = color.copy(alpha = 1f),
    autoSize = autoSize,
    fontSize = fontSize,
    fontStyle = fontStyle,
    fontWeight = fontWeight,
    fontFamily = fontFamily,
    letterSpacing = letterSpacing,
    textDecoration = textDecoration,
    textAlign = textAlign,
    lineHeight = lineHeight,
    overflow = overflow,
    softWrap = softWrap,
    maxLines = maxLines,
    minLines = minLines,
    onTextLayout = onTextLayout,
    style = style,
)
