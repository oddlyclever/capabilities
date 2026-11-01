package com.oddlyclever.jsonlogic

import kotlin.math.abs
import kotlin.math.floor

/**
 * JavaScript-compatible coercion semantics.
 *
 * JSONLogic (https://jsonlogic.com) is specified by its JavaScript reference
 * implementation, so spec-correct evaluation requires reproducing JS truthiness,
 * `ToNumber`/`parseFloat`, `String()` conversion, and the abstract (`==`) and strict
 * (`===`) equality algorithms over the generic value model used here
 * (`Map` / `List` / `String` / `Boolean` / `Number` / `null`).
 *
 * Everything is implemented algorithmically and deterministically in common Kotlin.
 * Numeric strings are validated against JS grammar before parsing, and numbers are
 * formatted with the ECMA-262 `Number::toString` notation algorithm — the platform's
 * `Double.toString` contributes only the shortest-round-trip *digits* (identical on
 * all supported targets), never the notation, which is where platforms disagree.
 *
 * This object is public because custom operators ([Operator]/[LazyOperator]) are the
 * library's extension point, and an operator that wants to treat a `"2.5"` string the
 * way `+` or `<` would needs the same coercion the standard operators use — hand-rolled
 * `(arg as? Number)` logic would silently diverge from JSONLogic semantics.
 *
 * Clean-room implementation. Semantics cross-checked against json-logic-js
 * (https://github.com/jwadhams/json-logic-js, MIT), allegro/json-logic-kmp
 * (Apache-2.0) and jamsesso/json-logic-java (MIT). No code copied.
 */
object JsCoercion {

    /** 2^53 — the largest magnitude at which every whole Double is exact. */
    private const val MAX_SAFE_WHOLE = 9007199254740992.0

    /** Strict JS numeric literal grammar, used by ToNumber for full-string parsing. */
    private val JS_NUMBER = Regex("""[+-]?(\d+\.?\d*([eE][+-]?\d+)?|\.\d+([eE][+-]?\d+)?)""")

    /** JS hex literal grammar (ToNumber accepts "0x1F"). */
    private val JS_HEX = Regex("""0[xX][0-9a-fA-F]+""")

    /** Longest-numeric-prefix grammar used by JS parseFloat. */
    private val JS_FLOAT_PREFIX =
        Regex("""^[+-]?(Infinity|\d+\.?\d*([eE][+-]?\d+)?|\.\d+([eE][+-]?\d+)?)""")

    /**
     * JSONLogic truthiness: JS truthiness plus jsonLogic's special case that empty
     * arrays are falsy. Empty maps are truthy (as in JS).
     */
    fun truthy(value: Any?): Boolean = when (value) {
        null -> false
        is Boolean -> value
        is Number -> {
            val d = value.toDouble()
            !d.isNaN() && d != 0.0
        }
        is String -> value.isNotEmpty()
        is List<*> -> value.isNotEmpty()
        else -> true
    }

    /** JS `String(value)` conversion. */
    fun stringify(value: Any?): String = when (value) {
        null -> "null" // String(null); note Array.prototype.join renders null as "" instead
        is String -> value
        is Boolean -> value.toString()
        is Number -> numberToString(value)
        // Array.prototype.toString == join(","); null elements render as ""
        is List<*> -> value.joinToString(",") { element -> if (element == null) "" else stringify(element) }
        is Map<*, *> -> "[object Object]"
        else -> value.toString()
    }

    /**
     * JS number formatting: whole numbers print without a fraction part (`1`, not `1.0`).
     * Kept deterministic across platforms by never delegating whole values to
     * `Double.toString()`.
     */
    fun numberToString(n: Number): String {
        // Only Long gets a direct fast path: it is a real class on every target, so the
        // check is deterministic (and Longs beyond 2^53 would lose precision as Double).
        // `is Int` would be a platform-divergent check — on Kotlin/JS every whole number
        // satisfies it — so Int deliberately funnels through the Double path instead,
        // which formats identically on all targets.
        if (n is Long) return n.toString()
        val d = n.toDouble()
        return when {
            d.isNaN() -> "NaN"
            d == Double.POSITIVE_INFINITY -> "Infinity"
            d == Double.NEGATIVE_INFINITY -> "-Infinity"
            d == 0.0 -> "0" // covers -0.0: JS String(-0) == "0"
            d == floor(d) && abs(d) <= MAX_SAFE_WHOLE -> d.toLong().toString()
            else -> ecmaScriptDoubleToString(d)
        }
    }

    /**
     * Formats [d] exactly as ECMAScript `Number::toString(10)` (ECMA-262 §6.1.6.1.20).
     *
     * The platform's `Double.toString` supplies the shortest-round-trip decimal
     * *digits* — the same on every supported target — but its *notation* is
     * platform-defined (JVM prints `0.0001` as `"1.0E-4"`, JS as `"0.0001"`), so the
     * notation is re-derived here from the (digits, exponent) pair per the JS rules:
     * plain notation for values whose decimal-point position n satisfies −6 < n ≤ 21,
     * exponential (`"1e+21"`, `"1e-7"`) outside that range.
     */
    private fun ecmaScriptDoubleToString(d: Double): String {
        val raw = d.toString() // any of: "123.45", "1.0E-4", "1e+21", "0.0001", ...
        val negative = raw.startsWith("-")
        val body = if (negative) raw.substring(1) else raw
        val eIndex = body.indexOfFirst { it == 'e' || it == 'E' }
        val mantissa = if (eIndex >= 0) body.substring(0, eIndex) else body
        val exponent = if (eIndex >= 0) body.substring(eIndex + 1).removePrefix("+").toInt() else 0
        val dot = mantissa.indexOf('.')
        var digits = if (dot >= 0) mantissa.removeRange(dot, dot + 1) else mantissa
        // Normalize to value == 0.<digits> * 10^n with no leading/trailing zeros.
        var n = (if (dot >= 0) dot else mantissa.length) + exponent
        val firstNonZero = digits.indexOfFirst { it != '0' }
        if (firstNonZero < 0) return "0" // unreachable (d != 0); defensive
        digits = digits.substring(firstNonZero)
        n -= firstNonZero
        digits = digits.trimEnd('0')
        val k = digits.length
        val sign = if (negative) "-" else ""
        return sign + when {
            n in k..21 -> digits + "0".repeat(n - k)
            n in 1..21 -> digits.substring(0, n) + "." + digits.substring(n) // k > n
            n in -5..0 -> "0." + "0".repeat(-n) + digits
            else -> {
                val exp = n - 1
                val head = if (k == 1) digits else "${digits[0]}.${digits.substring(1)}"
                head + (if (exp >= 0) "e+" else "e-") + abs(exp)
            }
        }
    }

    /** JS `ToNumber(value)`. Returns NaN for unconvertible values. */
    fun toNumber(value: Any?): Double = when (value) {
        null -> 0.0
        is Boolean -> if (value) 1.0 else 0.0
        is Number -> value.toDouble()
        is String -> stringToNumber(value)
        // ToPrimitive(array) is its joined string; ToNumber then parses that
        is List<*> -> stringToNumber(stringify(value))
        else -> Double.NaN
    }

    private fun stringToNumber(s: String): Double {
        val t = s.trim()
        return when {
            t.isEmpty() -> 0.0
            t == "Infinity" || t == "+Infinity" -> Double.POSITIVE_INFINITY
            t == "-Infinity" -> Double.NEGATIVE_INFINITY
            JS_HEX.matches(t) -> t.substring(2).toLongOrNull(16)?.toDouble() ?: Double.NaN
            JS_NUMBER.matches(t) -> t.toDoubleOrNull() ?: Double.NaN
            else -> Double.NaN
        }
    }

    /**
     * JS `parseFloat(String(value))` — parses the longest valid numeric prefix.
     * The reference implementation uses this (not ToNumber) inside `+` and `*`.
     */
    fun parseFloat(value: Any?): Double {
        val t = stringify(value).trim()
        val match = JS_FLOAT_PREFIX.find(t) ?: return Double.NaN
        val token = match.value
        if (token.endsWith("Infinity")) {
            return if (token.startsWith("-")) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        }
        return token.toDoubleOrNull() ?: Double.NaN
    }

    /** ToPrimitive for the equality/relational algorithms: containers become strings. */
    fun toPrimitive(value: Any?): Any? = when (value) {
        is List<*>, is Map<*, *> -> stringify(value)
        else -> value
    }

    /** JS abstract equality (`==`) over the generic value model. */
    fun looseEquals(a: Any?, b: Any?): Boolean = when {
        a == null && b == null -> true
        a == null || b == null -> false // null only equals null/undefined in JS
        // A boolean operand is converted to a number, then compared loosely again;
        // combined with ToNumber on the other side this collapses to numeric equality.
        a is Boolean || b is Boolean -> {
            val x = toNumber(toPrimitive(a))
            val y = toNumber(toPrimitive(b))
            !x.isNaN() && x == y
        }
        a is Number && b is Number -> a.toDouble() == b.toDouble()
        a is Number && b is String -> a.toDouble() == toNumber(b)
        a is String && b is Number -> toNumber(a) == b.toDouble()
        a is String && b is String -> a == b
        // container vs primitive: ToPrimitive the container and retry
        (a is List<*> || a is Map<*, *>) && (b is Number || b is String) -> looseEquals(toPrimitive(a), b)
        (b is List<*> || b is Map<*, *>) && (a is Number || a is String) -> looseEquals(a, toPrimitive(b))
        // two containers (or unknown types): reference identity, as in JS
        else -> a === b
    }

    /** JS strict equality (`===`): equal types and values; numbers compare numerically. */
    fun strictEquals(a: Any?, b: Any?): Boolean = when {
        a == null && b == null -> true
        a is Number && b is Number -> a.toDouble() == b.toDouble()
        a is String && b is String -> a == b
        a is Boolean && b is Boolean -> a == b
        else -> a != null && a === b
    }

    /**
     * JS relational comparison. Returns a negative/zero/positive integer, or `null`
     * when the operands are incomparable (either side coerces to NaN) — in which case
     * every relational operator is false, as in JS.
     */
    fun compare(a: Any?, b: Any?): Int? {
        val pa = toPrimitive(a)
        val pb = toPrimitive(b)
        if (pa is String && pb is String) return pa.compareTo(pb)
        val na = toNumber(pa)
        val nb = toNumber(pb)
        if (na.isNaN() || nb.isNaN()) return null
        // Primitive comparisons (not compareTo) so that -0.0 == 0.0, as in JS.
        return when {
            na < nb -> -1
            na > nb -> 1
            else -> 0
        }
    }
}
