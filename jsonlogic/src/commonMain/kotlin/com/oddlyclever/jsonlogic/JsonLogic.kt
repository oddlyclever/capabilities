package com.oddlyclever.jsonlogic

import kotlin.math.abs
import kotlin.math.floor

/**
 * A deterministic JSONLogic (https://jsonlogic.com) rule evaluator in pure common Kotlin.
 *
 * ### Why this exists
 * A JSONLogic rule is a JSON tree (`{"op": [args...]}`), so there is no parser or
 * grammar — this class *is* the whole implementation. Because it is a single
 * common-Kotlin implementation with no platform-specific code, the same rule evaluates
 * to the same result on every Kotlin target (JVM server included) *by construction* —
 * which is the property you need when a rule's outcome must be trusted wherever it ran.
 *
 * ### Value model
 * The evaluator is deliberately serialization-agnostic: rules and data are plain
 * `Map<String, Any?>` / `List<Any?>` / `String` / `Boolean` / `Number` / `null`
 * structures, duck-typed like the reference implementations. How JSON text becomes
 * those structures (kotlinx.serialization, org.json, a Swift bridge…) is the caller's
 * concern, which keeps this module dependency-free.
 *
 * ### Usage
 * ```
 * val rule = mapOf(">" to listOf(mapOf("var" to "temp"), 100))
 * JsonLogic.Default.apply(rule, mapOf("temp" to 110)) // => true
 * ```
 *
 * ### Custom operators
 * ```
 * val logic = JsonLogic.builder()
 *     .operator("double") { args, _ -> (args.firstOrNull() as? Number)?.toDouble()?.times(2) }
 *     .build()
 * ```
 * Registering a name that already exists (standard or custom) replaces it; the last
 * registration wins. See [Operator] (eager) and [LazyOperator] (short-circuit /
 * scoped-context) for which to implement.
 *
 * Instances are immutable and safe to share; [Default] provides the standard operator
 * set (`var`, `missing`, `missing_some`, `==`, `===`, `!=`, `!==`, `<`, `<=`, `>`, `>=`,
 * `and`, `or`, `!`, `!!`, `if`, `?:`, `in`, `map`, `filter`, `reduce`, `all`, `some`,
 * `none`, `merge`, `cat`, `substr`, `+`, `-`, `*`, `/`, `%`, `min`, `max`, `log`).
 */
class JsonLogic private constructor(
    eagerOperators: Map<String, Operator>,
    lazyOperators: Map<String, LazyOperator>,
) {
    private val evaluator = DefaultEvaluator(eagerOperators, lazyOperators)

    /**
     * Evaluates [rule] against [data].
     *
     * @param rule the JSONLogic expression tree: `Map` / `List` / primitives / `null`.
     * @param data the data context for `var` and friends; may be any value or `null`.
     * @return the result, with every integral number normalized to `Long` (so `1 + 2`
     *   is `3L`, not `3.0` — matching how JavaScript prints and re-serializes numbers)
     *   and fractional numbers as `Double`. Normalizing to one integral type is also
     *   what keeps results deterministic across targets: Kotlin/JS cannot tell `Int`
     *   from `Double` at runtime, so without it the same rule would surface `Int` on
     *   the JVM and something else on JS.
     * @throws JsonLogicException if the rule references an unregistered operator.
     */
    fun apply(rule: Any?, data: Any? = null): Any? = normalize(evaluator.evaluate(rule, data))

    /**
     * JSONLogic arithmetic happens in `Double` (as in JavaScript); on the way out,
     * integral values surface as `Long` so results serialize like JSON integers.
     *
     * Ordering matters for Kotlin/JS, where every number satisfies `is Double`
     * (and `is Int`, when whole): the `is Double` branch must come first and must
     * terminate without recursing, otherwise JS recurses infinitely; the later
     * branches then only ever run on platforms with reified number types.
     */
    private fun normalize(value: Any?): Any? = when (value) {
        is Double ->
            if (!value.isNaN() && !value.isInfinite() && value == floor(value) && abs(value) <= MAX_SAFE_WHOLE) {
                value.toLong()
            } else {
                value
            }
        is Long -> value
        is Int -> value.toLong()
        is Float -> normalize(value.toDouble())
        is Byte -> value.toLong()
        is Short -> value.toLong()
        is List<*> -> value.map { normalize(it) }
        is Map<*, *> -> value.entries.associate { (k, v) -> k to normalize(v) }
        else -> value
    }

    /** Builds a [JsonLogic] with the standard operators plus any custom registrations. */
    class Builder internal constructor() {
        private val eager = LinkedHashMap<String, Operator>()
        private val lazy = LinkedHashMap<String, LazyOperator>()
        private var logger: (Any?) -> Unit = { message -> println(message) }

        /** Registers an eager [Operator] as [name], replacing any existing operator. */
        fun operator(name: String, operator: Operator): Builder = apply {
            lazy.remove(name)
            eager[name] = operator
        }

        /** Registers a [LazyOperator] as [name], replacing any existing operator. */
        fun lazyOperator(name: String, operator: LazyOperator): Builder = apply {
            eager.remove(name)
            lazy[name] = operator
        }

        /** Replaces the sink used by the standard `log` operator (default: `println`). */
        fun logger(logger: (Any?) -> Unit): Builder = apply { this.logger = logger }

        fun build(): JsonLogic {
            val eagerOperators = LinkedHashMap<String, Operator>()
            val lazyOperators = LinkedHashMap<String, LazyOperator>()
            StandardOperators.registerEager(eagerOperators, logger)
            StandardOperators.registerLazy(lazyOperators)
            // Custom registrations override standard ones; a name lives in exactly
            // one of the two maps so dispatch is unambiguous.
            for ((name, operator) in eager) {
                lazyOperators.remove(name)
                eagerOperators[name] = operator
            }
            for ((name, operator) in lazy) {
                eagerOperators.remove(name)
                lazyOperators[name] = operator
            }
            return JsonLogic(eagerOperators, lazyOperators)
        }
    }

    companion object {
        /** 2^53 — the largest magnitude at which every whole Double is exact. */
        private const val MAX_SAFE_WHOLE = 9007199254740992.0

        /** A shared instance with the standard operator set. */
        val Default: JsonLogic = Builder().build()

        /** Starts a [Builder] for an instance with custom operators or logging. */
        fun builder(): Builder = Builder()
    }
}
