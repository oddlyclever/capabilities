package com.oddlyclever.sundries

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints

/**
 * Tags this element for UI tests when [tagId] is given, with [suffix] parts joined to it by underscores.
 *
 * A reusable component takes its tag as an optional parameter and passes it here, so it is tagged only where the
 * screen using it asked for one. Empty parts are skipped, and a null or empty [tagId] tags nothing.
 */
fun Modifier.testTagIfNotNull(tagId: String? = null, vararg suffix: String): Modifier =
    testTagIfNotNull(tagId, null, *suffix)

/**
 * Tags this element for UI tests when [tagId] is given: [tagId], then [suffix], then [index], joined by underscores.
 *
 * The index is for an element repeated in a list, so each row gets a tag of its own, such as `row_title_3`.
 */
fun Modifier.testTagIfNotNull(
    tagId: String? = null,
    index: Int? = null,
    vararg suffix: String,
): Modifier {
    if (tagId.isNullOrEmpty()) return this
    val parts = buildList {
        add(tagId)
        addAll(suffix.filter { it.isNotEmpty() })
        if (index != null) add(index.toString())
    }
    return testTag(parts.joinToString("_"))
}

/** Adds what [factory] makes when [condition] holds, so a modifier chain needs no `if` around one link. */
inline fun Modifier.addIf(
    condition: Boolean,
    crossinline factory: Modifier.() -> Modifier,
): Modifier = if (condition) factory() else this

/** Adds what [factory] makes from [value] when there is one, handing it over already checked for null. */
inline fun <T> Modifier.addIfNotNull(
    value: T?,
    crossinline factory: Modifier.(T) -> Modifier,
): Modifier = if (value != null) factory(value) else this

/** Adds what [factory] makes when [value] is null: the other half of [addIfNotNull], for a fallback. */
inline fun Modifier.addIfNull(
    value: Any?,
    crossinline factory: Modifier.() -> Modifier,
): Modifier = if (value == null) factory() else this

/** Fills even [row]s with [alternate] and leaves odd ones clear, for a striped list or table. */
fun Modifier.alternatingBackground(alternate: Color, row: Int): Modifier =
    background(if (row % 2 == 0) alternate else Color.Unspecified)

/**
 * Sizes an icon to [style]'s font size times [scale], so it sits in a line of that text.
 *
 * Where the height it is given is unbounded it takes no space at all, so it never stretches a row it sits in.
 */
@Composable
fun Modifier.iconSizeByTextStyle(style: TextStyle, scale: Float = 1.3f): Modifier {
    val size = with(LocalDensity.current) { style.fontSize.toDp() * scale }
    return size(size).layout { measurable, constraints ->
        if (constraints.maxHeight == Constraints.Infinity) {
            layout(0, 0) {}
        } else {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
    }
}

/**
 * Makes this element clickable without the ripple, for an element that shows its own response to a press.
 *
 * Give [onClickLabel] and [role] whenever the element is not already a button, so a screen reader says what the
 * click does. Removing the ripple removes only the visual; the element is still announced and focusable.
 */
fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier = clickable(
    interactionSource = interactionSource,
    indication = null,
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    onClick = onClick,
)
