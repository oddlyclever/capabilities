package com.oddlyclever.jsonlogic

/**
 * An eager JSONLogic operator: its arguments are fully evaluated before it is invoked.
 *
 * This is the extension point for most custom operators. Implementations receive the
 * evaluated argument list and the current data context, and return the operator's result.
 *
 * Use [LazyOperator] instead when the operator must control evaluation itself — for
 * short-circuiting, or for evaluating sub-expressions against a scoped data context
 * (as the collection operators `map`/`filter`/`reduce`/`all`/`some`/`none` do).
 *
 * Example — registering a custom `pow` operator:
 * ```
 * val logic = JsonLogic.builder()
 *     .operator("pow") { args, _ ->
 *         val base = args.getOrNull(0) as? Number ?: return@operator null
 *         val exp = args.getOrNull(1) as? Number ?: return@operator null
 *         base.toDouble().pow(exp.toDouble())
 *     }
 *     .build()
 * logic.apply(mapOf("pow" to listOf(2, 10))) // => 1024
 * ```
 */
fun interface Operator {
    /**
     * @param args the operator's arguments, each already evaluated against [data].
     *   A scalar argument in the rule (`{"op": x}`) arrives wrapped as a single-element list.
     * @param data the current data context the rule is being evaluated against.
     * @return the operator's result value.
     */
    fun apply(args: List<Any?>, data: Any?): Any?
}
