package com.oddlyclever.jsonlogic

import kotlin.test.fail

/**
 * Asserts one `[rule, data, expected]` conformance triplet against [JsonLogic.Default].
 *
 * Comparison is deep and JSON-typed: numbers compare numerically (the suite writes
 * `1` where evaluation may produce `1L` or `1.0` — all the same JSON number), lists
 * element-wise, maps key-wise, everything else strictly.
 */
internal fun checkConformance(rule: Any?, data: Any?, expected: Any?) {
    val actual = JsonLogic.Default.apply(rule, data)
    if (!jsonDeepEquals(expected, actual)) {
        fail(
            "JSONLogic conformance failure\n" +
                "  rule:     $rule\n" +
                "  data:     $data\n" +
                "  expected: $expected (${typeName(expected)})\n" +
                "  actual:   $actual (${typeName(actual)})"
        )
    }
}

internal fun jsonDeepEquals(expected: Any?, actual: Any?): Boolean = when {
    expected == null && actual == null -> true
    expected is Number && actual is Number -> expected.toDouble() == actual.toDouble()
    expected is List<*> && actual is List<*> ->
        expected.size == actual.size &&
            expected.indices.all { jsonDeepEquals(expected[it], actual[it]) }
    expected is Map<*, *> && actual is Map<*, *> ->
        expected.size == actual.size &&
            expected.keys.all { key -> actual.containsKey(key) && jsonDeepEquals(expected[key], actual[key]) }
    else -> expected == actual
}

private fun typeName(value: Any?): String = when (value) {
    null -> "null"
    else -> value::class.simpleName ?: "?"
}
