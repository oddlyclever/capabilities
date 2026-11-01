package com.oddlyclever.jsonlogic

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Reference-implementation semantics that the shared conformance suite does not pin
 * down explicitly, verified here so they can't regress silently.
 */
class EvaluatorSemanticsTest {

    // -- var: absence vs. present-but-null -----------------------------------------

    @Test
    fun varAbsentKeyYieldsDefault() {
        val rule = mapOf("var" to listOf("x", 26))
        // Integral results always surface as Long, on every target (see normalize()).
        assertEquals(26L, JsonLogic.Default.apply(rule, emptyMap<String, Any?>()))
    }

    @Test
    fun varPresentNullLeafYieldsNullNotDefault() {
        // json-logic-js: only an ABSENT key falls back to the default; an explicit
        // null value is a real value and is returned as-is.
        val rule = mapOf("var" to listOf("x", 26))
        assertEquals(null, JsonLogic.Default.apply(rule, mapOf("x" to null)))
    }

    @Test
    fun varNullMidPathYieldsDefault() {
        val rule = mapOf("var" to listOf("a.b", "fallback"))
        assertEquals("fallback", JsonLogic.Default.apply(rule, mapOf("a" to null)))
    }

    @Test
    fun varNumericIndexIntoListData() {
        assertEquals("b", JsonLogic.Default.apply(mapOf("var" to 1), listOf("a", "b", "c")))
    }

    @Test
    fun varDottedPathThroughListAndMap() {
        val data = mapOf("items" to listOf(mapOf("name" to "first"), mapOf("name" to "second")))
        assertEquals("second", JsonLogic.Default.apply(mapOf("var" to "items.1.name"), data))
    }

    // -- Short-circuit behavior ----------------------------------------------------

    @Test
    fun andShortCircuitsOnFirstFalsy() {
        var evaluated = 0
        val logic = JsonLogic.builder()
            .operator("probe") { _, _ -> evaluated++; true }
            .build()
        logic.apply(mapOf("and" to listOf(false, mapOf("probe" to emptyList<Any?>()))))
        assertEquals(0, evaluated)
    }

    @Test
    fun orShortCircuitsOnFirstTruthy() {
        var evaluated = 0
        val logic = JsonLogic.builder()
            .operator("probe") { _, _ -> evaluated++; true }
            .build()
        logic.apply(mapOf("or" to listOf(true, mapOf("probe" to emptyList<Any?>()))))
        assertEquals(0, evaluated)
    }

    @Test
    fun ifDoesNotEvaluateUntakenBranches() {
        var evaluated = 0
        val logic = JsonLogic.builder()
            .operator("probe") { _, _ -> evaluated++; "side-effect" }
            .build()
        val rule = mapOf("if" to listOf(true, "taken", mapOf("probe" to emptyList<Any?>())))
        assertEquals("taken", logic.apply(rule))
        assertEquals(0, evaluated)
    }

    // -- reduce --------------------------------------------------------------------

    @Test
    fun reduceInitialAccumulatorIsEvaluated() {
        // Deliberate divergence from json-logic-js (which passes it raw); documented
        // in StandardOperators. {"var":"start"} must resolve against the data context.
        val rule = mapOf(
            "reduce" to listOf(
                mapOf("var" to "ints"),
                mapOf("+" to listOf(mapOf("var" to "current"), mapOf("var" to "accumulator"))),
                mapOf("var" to "start"),
            )
        )
        val data = mapOf("ints" to listOf(1, 2, 3), "start" to 10)
        assertEquals(16L, JsonLogic.Default.apply(rule, data))
    }

    @Test
    fun reduceOverNonListYieldsInitial() {
        val rule = mapOf("reduce" to listOf(null, mapOf("+" to listOf(1, 1)), 7))
        assertEquals(7L, JsonLogic.Default.apply(rule))
    }

    // -- Result normalization ------------------------------------------------------

    @Test
    fun wholeDoublesNormalizeToLong() {
        assertEquals(3L, JsonLogic.Default.apply(mapOf("+" to listOf(1, 2))))
        assertEquals(2L, JsonLogic.Default.apply(mapOf("/" to listOf(4, 2))))
    }

    @Test
    fun fractionalResultsStayDouble() {
        assertEquals(3.5, JsonLogic.Default.apply(mapOf("+" to listOf(1, 2.5))))
    }

    @Test
    fun normalizationRecursesIntoCollections() {
        val rule = mapOf("map" to listOf(mapOf("var" to ""), mapOf("*" to listOf(mapOf("var" to ""), 2))))
        assertEquals(listOf(2L, 4L), JsonLogic.Default.apply(rule, listOf(1, 2)))
    }

    // -- Non-rule pass-through -----------------------------------------------------

    @Test
    fun multiKeyMapPassesThroughUnevaluated() {
        // Not an operation node (two keys), so it is never evaluated — but numbers in
        // the result still get the integral-to-Long normalization like any result.
        val literal = mapOf("a" to 1, "b" to mapOf("var" to "x"))
        val expected = mapOf("a" to 1L, "b" to mapOf("var" to "x"))
        assertEquals(expected, JsonLogic.Default.apply(literal, mapOf("x" to "must not resolve")))
    }

    @Test
    fun emptyMapPassesThrough() {
        assertEquals(emptyMap<Any?, Any?>(), JsonLogic.Default.apply(emptyMap<String, Any?>()))
    }

    // -- cat/string edge semantics -------------------------------------------------

    @Test
    fun catFormatsWholeNumbersLikeJavaScript() {
        // 1.0 + 2.0 is the number 3; JS String() prints it as "3", never "3.0".
        assertEquals("total 3", JsonLogic.Default.apply(mapOf("cat" to listOf("total ", mapOf("+" to listOf(1.5, 1.5))))))
    }
}
