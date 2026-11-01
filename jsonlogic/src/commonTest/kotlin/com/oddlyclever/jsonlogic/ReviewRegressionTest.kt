package com.oddlyclever.jsonlogic

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression pins for defects found in adversarial review. Every expectation here is a
 * single literal asserted identically on ALL targets — that is the point: these are the
 * cases where a platform-delegating implementation would give different answers on the
 * JVM than in a browser.
 */
class ReviewRegressionTest {

    // -- ECMA-262 number formatting (was: platform Double.toString leaked through) ---

    @Test
    fun catFormatsSmallFractionsWithoutScientificNotation() {
        // JVM Double.toString(0.0001) is "1.0E-4"; JS String(0.0001) is "0.0001".
        // The evaluator must print the ECMAScript form everywhere.
        assertEquals("0.0001", JsonLogic.Default.apply(mapOf("cat" to listOf(0.0001))))
    }

    @Test
    fun catFormatsLargeDecimalsPlainUpTo1e21() {
        // JVM would print "1.23456785E7".
        assertEquals("12345678.5", JsonLogic.Default.apply(mapOf("cat" to listOf(12345678.5))))
    }

    @Test
    fun catFormatsWholeDoublesBeyondTwoPow53AsPlainDigits() {
        // 1e16 exceeds 2^53 (so it skips the Long fast path); JVM raw form is "1.0E16".
        assertEquals("10000000000000000", JsonLogic.Default.apply(mapOf("cat" to listOf(1.0E16))))
    }

    @Test
    fun catFormatsAtAndAboveTenPow21Exponentially() {
        assertEquals("1e+21", JsonLogic.Default.apply(mapOf("cat" to listOf(1.0E21))))
    }

    @Test
    fun catFormatsBelowTenPowMinus6Exponentially() {
        assertEquals("1e-7", JsonLogic.Default.apply(mapOf("cat" to listOf(1.0E-7))))
    }

    @Test
    fun catFormatsNegativeFractions() {
        assertEquals("-0.0001", JsonLogic.Default.apply(mapOf("cat" to listOf(-0.0001))))
    }

    // -- var: .length and canonical indices (JS property-lookup semantics) ----------

    @Test
    fun varLengthOnListYieldsSize() {
        assertEquals(2L, JsonLogic.Default.apply(mapOf("var" to "a.length"), mapOf("a" to listOf(10, 20))))
    }

    @Test
    fun varLengthOnStringYieldsLength() {
        assertEquals(3L, JsonLogic.Default.apply(mapOf("var" to "s.length"), mapOf("s" to "abc")))
    }

    @Test
    fun varLiteralLengthKeyOnMapWinsOverProperty() {
        assertEquals("stored", JsonLogic.Default.apply(mapOf("var" to "a.length"), mapOf("a" to mapOf("length" to "stored"))))
    }

    @Test
    fun varNonCanonicalIndexIsAbsent() {
        // JS: arr["01"] is a plain property lookup (undefined), NOT index 1.
        val rule = mapOf("var" to listOf("a.01", "fallback"))
        assertEquals("fallback", JsonLogic.Default.apply(rule, mapOf("a" to listOf(10, 20))))
    }

    // -- substr: fractional negative end truncates the SUM (ToInteger semantics) ----

    @Test
    fun substrFractionalNegativeEndTruncatesSum() {
        // JS: "jsonlogic".substr(0).substr(0, ToInteger(9 + (-1.5))) == 7 chars.
        val rule = mapOf("substr" to listOf("jsonlogic", 0, -1.5))
        assertEquals("jsonlog", JsonLogic.Default.apply(rule))
    }

    // -- min/max: IEEE negative-zero ties (JS Math.min/Math.max semantics) ----------

    @Test
    fun minPrefersNegativeZero() {
        // Math.min(0, -0) is -0, observable through division: 1/-0 == -Infinity.
        val rule = mapOf("/" to listOf(1, mapOf("min" to listOf(0.0, -0.0))))
        assertEquals(Double.NEGATIVE_INFINITY, JsonLogic.Default.apply(rule))
    }

    @Test
    fun maxPrefersPositiveZero() {
        val rule = mapOf("/" to listOf(1, mapOf("max" to listOf(-0.0, 0.0))))
        assertEquals(Double.POSITIVE_INFINITY, JsonLogic.Default.apply(rule))
    }
}
