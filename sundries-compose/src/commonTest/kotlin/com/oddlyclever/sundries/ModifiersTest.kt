package com.oddlyclever.sundries

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Pins the conditional modifiers: each adds its link only when its condition holds and otherwise returns the chain it
 * was given, and [addIfNull] adds its link when the value is null, the opposite of [addIfNotNull].
 */
class ModifiersTest {

    private val link = Modifier.alpha(0.5f)

    @Test
    fun addIfAddsOnlyWhenTrue() {
        assertEquals(link, Modifier.addIf(true) { then(link) })
        assertSame(Modifier, Modifier.addIf(false) { then(link) })
    }

    @Test
    fun addIfNotNullAddsOnlyForAValue() {
        assertEquals(link, Modifier.addIfNotNull(0.5f) { alpha(it) })
        assertSame(Modifier, Modifier.addIfNotNull(null as Float?) { alpha(it) })
    }

    @Test
    fun addIfNullAddsOnlyForNull() {
        assertEquals(link, Modifier.addIfNull(null) { then(link) })
        assertSame(Modifier, Modifier.addIfNull(1) { then(link) })
    }
}
