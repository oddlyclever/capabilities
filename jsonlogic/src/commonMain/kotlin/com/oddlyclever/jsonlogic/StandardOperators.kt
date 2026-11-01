package com.oddlyclever.jsonlogic

/**
 * The standard JSONLogic operator set (https://jsonlogic.com/operations.html),
 * implemented with the JavaScript semantics of the reference implementation.
 *
 * Clean-room implementation. Semantics cross-checked against json-logic-js
 * (https://github.com/jwadhams/json-logic-js, MIT), allegro/json-logic-kmp
 * (Apache-2.0) and jamsesso/json-logic-java (MIT). No code copied.
 *
 * Documented divergences from json-logic-js (both spec-conformant per tests.json):
 * - `reduce`'s initial-accumulator argument is evaluated against the data context
 *   (the reference passes it through raw — widely considered a reference bug).
 * - `*` with zero arguments returns `null` (the reference throws a TypeError).
 * - `all`/`some` treat a non-list first argument as an empty list (the reference
 *   crashes on `null.length` in some of those paths).
 */
internal object StandardOperators {

    private const val ACCUMULATOR = "accumulator"
    private const val CURRENT = "current"

    // -- Data access ---------------------------------------------------------------

    /**
     * Resolves a `var`-style dot path against the data context.
     *
     * Follows the reference implementation exactly: an empty/null path returns the
     * whole data context; an absent key (or unindexable node) yields [ifAbsent];
     * a key that is *present with a null value* yields null, not the default —
     * absence and null are distinct, which is why lookups can't just chain `?.get`.
     */
    private fun resolveVar(path: Any?, data: Any?, ifAbsent: Any?): Any? {
        if (path == null || path == "") return data
        val parts = JsCoercion.stringify(path).split('.')
        var current: Any? = data
        for (part in parts) {
            current = when (current) {
                null -> return ifAbsent
                is Map<*, *> -> if (current.containsKey(part)) current[part] else return ifAbsent
                is List<*> -> when {
                    part == "length" -> current.size // arr.length is a real JS property
                    else -> {
                        val index = canonicalIndex(part) ?: return ifAbsent
                        if (index in current.indices) current[index] else return ifAbsent
                    }
                }
                is String -> when {
                    part == "length" -> current.length
                    else -> {
                        // JS strings are index-addressable: "abc"[1] == "b"
                        val index = canonicalIndex(part) ?: return ifAbsent
                        if (index in current.indices) current[index].toString() else return ifAbsent
                    }
                }
                else -> return ifAbsent
            }
        }
        return current
    }

    /**
     * JS array indexing only matches the CANONICAL numeric string: `arr["1"]` is
     * element 1, but `arr["01"]` and `arr["+1"]` are plain (absent) property lookups.
     */
    private fun canonicalIndex(part: String): Int? =
        part.toIntOrNull()?.takeIf { it >= 0 && it.toString() == part }

    /** `missing` semantics: a key is missing when it resolves to absent, null, or "". */
    private fun missingKeys(keys: List<Any?>, data: Any?): List<Any?> = keys.filter { key ->
        val value = resolveVar(key, data, null)
        value == null || value == ""
    }

    // -- Numeric helpers -----------------------------------------------------------

    // kotlin.math.min/max follow IEEE 754 like JS Math.min/max: NaN propagates, and
    // -0.0 < +0.0 on ties (a hand-rolled `<` loop would keep the wrong zero).
    private fun minOf(args: List<Any?>): Double =
        args.fold(Double.POSITIVE_INFINITY) { acc, arg -> kotlin.math.min(acc, JsCoercion.toNumber(arg)) }

    private fun maxOf(args: List<Any?>): Double =
        args.fold(Double.NEGATIVE_INFINITY) { acc, arg -> kotlin.math.max(acc, JsCoercion.toNumber(arg)) }

    /** JS String.prototype.substr(start, length), including negative-start handling. */
    private fun substr(source: String, startArg: Double, lengthArg: Double?): String {
        val length = source.length
        var start = if (startArg.isNaN()) 0 else startArg.toInt()
        if (start < 0) start = kotlin.math.max(length + start, 0)
        if (start >= length) return ""
        val available = length - start
        val count = when {
            lengthArg == null -> available
            lengthArg.isNaN() -> 0
            else -> lengthArg.toInt().coerceIn(0, available)
        }
        return source.substring(start, start + count)
    }

    // -- Registration --------------------------------------------------------------

    fun registerEager(into: MutableMap<String, Operator>, logger: (Any?) -> Unit) {
        // Data access
        into["var"] = Operator { args, data -> resolveVar(args.getOrNull(0), data, args.getOrNull(1)) }
        into["missing"] = Operator { args, data ->
            // {"missing": ["a", "b"]} and {"missing": {"merge": ...}} both funnel here;
            // if the first evaluated argument is a list, it IS the key list.
            val keys = args.getOrNull(0) as? List<Any?> ?: args
            missingKeys(keys, data)
        }
        into["missing_some"] = Operator { args, data ->
            val needed = JsCoercion.toNumber(args.getOrNull(0))
            val options = args.getOrNull(1) as? List<Any?> ?: emptyList()
            val missing = missingKeys(options, data)
            if (options.size - missing.size >= needed) emptyList() else missing
        }

        // Equality
        into["=="] = Operator { args, _ -> JsCoercion.looseEquals(args.getOrNull(0), args.getOrNull(1)) }
        into["!="] = Operator { args, _ -> !JsCoercion.looseEquals(args.getOrNull(0), args.getOrNull(1)) }
        into["==="] = Operator { args, _ -> JsCoercion.strictEquals(args.getOrNull(0), args.getOrNull(1)) }
        into["!=="] = Operator { args, _ -> !JsCoercion.strictEquals(args.getOrNull(0), args.getOrNull(1)) }

        // Negation
        into["!"] = Operator { args, _ -> !JsCoercion.truthy(args.getOrNull(0)) }
        into["!!"] = Operator { args, _ -> JsCoercion.truthy(args.getOrNull(0)) }

        // Relational (with the reference's 3-argument "between" sugar on < and <=)
        into["<"] = Operator { args, _ ->
            val first = JsCoercion.compare(args.getOrNull(0), args.getOrNull(1))
            when {
                first == null || first >= 0 -> false
                args.size < 3 -> true
                else -> {
                    val second = JsCoercion.compare(args.getOrNull(1), args.getOrNull(2))
                    second != null && second < 0
                }
            }
        }
        into["<="] = Operator { args, _ ->
            val first = JsCoercion.compare(args.getOrNull(0), args.getOrNull(1))
            when {
                first == null || first > 0 -> false
                args.size < 3 -> true
                else -> {
                    val second = JsCoercion.compare(args.getOrNull(1), args.getOrNull(2))
                    second != null && second <= 0
                }
            }
        }
        into[">"] = Operator { args, _ ->
            val c = JsCoercion.compare(args.getOrNull(0), args.getOrNull(1))
            c != null && c > 0
        }
        into[">="] = Operator { args, _ ->
            val c = JsCoercion.compare(args.getOrNull(0), args.getOrNull(1))
            c != null && c >= 0
        }

        // Arithmetic. The reference uses parseFloat inside + and *, but plain JS
        // operator coercion (ToNumber) for -, /, %, min, max.
        into["+"] = Operator { args, _ -> args.fold(0.0) { acc, v -> acc + JsCoercion.parseFloat(v) } }
        into["*"] = Operator { args, _ ->
            when (args.size) {
                0 -> null // reference throws here; returning null is kinder and untested by the suite
                1 -> args[0] // JS Array.reduce with no initial returns a single element uncoerced
                else -> args.map { JsCoercion.parseFloat(it) }.reduce { a, b -> a * b }
            }
        }
        into["-"] = Operator { args, _ ->
            when (args.size) {
                0 -> null
                1 -> -JsCoercion.toNumber(args[0])
                else -> JsCoercion.toNumber(args[0]) - JsCoercion.toNumber(args[1])
            }
        }
        into["/"] = Operator { args, _ ->
            JsCoercion.toNumber(args.getOrNull(0)) / JsCoercion.toNumber(args.getOrNull(1))
        }
        into["%"] = Operator { args, _ ->
            JsCoercion.toNumber(args.getOrNull(0)) % JsCoercion.toNumber(args.getOrNull(1))
        }
        into["min"] = Operator { args, _ -> minOf(args) }
        into["max"] = Operator { args, _ -> maxOf(args) }

        // String
        into["cat"] = Operator { args, _ ->
            // Array.prototype.join semantics: null renders as "", not "null"
            args.joinToString("") { if (it == null) "" else JsCoercion.stringify(it) }
        }
        into["substr"] = Operator { args, _ ->
            val source = JsCoercion.stringify(args.getOrNull(0))
            val start = JsCoercion.toNumber(args.getOrNull(1))
            val third = args.getOrNull(2)
            if (third == null && args.size < 3) {
                substr(source, start, null)
            } else {
                val end = JsCoercion.toNumber(third)
                if (end < 0) {
                    // negative third argument trims from the END of the remainder;
                    // JS truncates the SUM (ToInteger(len + end)), not `end` alone,
                    // so a fractional -1.5 removes 2 characters, not 1
                    val remainder = substr(source, start, null)
                    val keep = (remainder.length + end).toInt().coerceAtLeast(0)
                    remainder.substring(0, keep)
                } else {
                    substr(source, start, end)
                }
            }
        }

        // Array
        into["merge"] = Operator { args, _ ->
            args.flatMap { if (it is List<*>) it else listOf(it) }
        }
        into["in"] = Operator { args, _ ->
            val needle = args.getOrNull(0)
            when (val haystack = args.getOrNull(1)) {
                is String -> haystack.contains(JsCoercion.stringify(needle))
                is List<*> -> haystack.any { JsCoercion.strictEquals(it, needle) } // JS indexOf uses ===
                else -> false
            }
        }

        // Logging
        into["log"] = Operator { args, _ ->
            val value = args.getOrNull(0)
            logger(value)
            value
        }
    }

    fun registerLazy(into: MutableMap<String, LazyOperator>) {
        // Control flow — short-circuiting, so unreached branches are never evaluated.
        val ifOperator = LazyOperator { args, data, ev ->
            // [cond, then, cond, then, ..., else?] — any odd trailing argument is the else.
            var i = 0
            var result: Any? = null
            while (i < args.size - 1) {
                if (ev.truthy(ev.evaluate(args[i], data))) {
                    result = ev.evaluate(args[i + 1], data)
                    break
                }
                i += 2
            }
            if (i < args.size - 1) result else if (i < args.size) ev.evaluate(args[i], data) else null
        }
        into["if"] = ifOperator
        into["?:"] = ifOperator

        into["and"] = LazyOperator { args, data, ev ->
            var current: Any? = null
            for (arg in args) {
                current = ev.evaluate(arg, data)
                if (!ev.truthy(current)) break // first falsy operand is the result
            }
            current
        }
        into["or"] = LazyOperator { args, data, ev ->
            var current: Any? = null
            for (arg in args) {
                current = ev.evaluate(arg, data)
                if (ev.truthy(current)) break // first truthy operand is the result
            }
            current
        }

        // Collection operators — the sub-expression evaluates against a SCOPED data
        // context: each element (map/filter/all/some/none), or {"current","accumulator"}
        // for reduce. This is what makes count-style rules possible, e.g.
        // {">=": [{"reduce": [{"filter": [{"var":"items"}, {"var":"checked"}]},
        //                     {"+": [{"var":"accumulator"}, 1]}, 0]}, 3]}
        into["map"] = LazyOperator { args, data, ev ->
            val list = ev.evaluate(args.getOrNull(0), data) as? List<*> ?: emptyList<Any?>()
            val logic = args.getOrNull(1)
            list.map { element -> ev.evaluate(logic, element) }
        }
        into["filter"] = LazyOperator { args, data, ev ->
            val list = ev.evaluate(args.getOrNull(0), data) as? List<*> ?: emptyList<Any?>()
            val logic = args.getOrNull(1)
            list.filter { element -> ev.truthy(ev.evaluate(logic, element)) }
        }
        into["reduce"] = LazyOperator { args, data, ev ->
            val listValue = ev.evaluate(args.getOrNull(0), data)
            val logic = args.getOrNull(1)
            val initial = if (args.size >= 3) ev.evaluate(args[2], data) else null
            val list = listValue as? List<*> ?: return@LazyOperator initial
            list.fold(initial) { accumulator, current ->
                ev.evaluate(logic, mapOf(CURRENT to current, ACCUMULATOR to accumulator))
            }
        }
        into["all"] = LazyOperator { args, data, ev ->
            val list = ev.evaluate(args.getOrNull(0), data) as? List<*> ?: emptyList<Any?>()
            if (list.isEmpty()) return@LazyOperator false // all of an empty set is false
            val logic = args.getOrNull(1)
            list.all { element -> ev.truthy(ev.evaluate(logic, element)) }
        }
        into["some"] = LazyOperator { args, data, ev ->
            val list = ev.evaluate(args.getOrNull(0), data) as? List<*> ?: emptyList<Any?>()
            val logic = args.getOrNull(1)
            list.any { element -> ev.truthy(ev.evaluate(logic, element)) }
        }
        into["none"] = LazyOperator { args, data, ev ->
            val list = ev.evaluate(args.getOrNull(0), data) as? List<*> ?: emptyList<Any?>()
            val logic = args.getOrNull(1)
            list.none { element -> ev.truthy(ev.evaluate(logic, element)) }
        }
    }
}
