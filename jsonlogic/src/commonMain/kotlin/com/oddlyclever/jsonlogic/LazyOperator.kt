package com.oddlyclever.jsonlogic

/**
 * A lazy JSONLogic operator: it receives its arguments *unevaluated*, together with the
 * [Evaluator], and decides itself what to evaluate, when, and against which data context.
 *
 * This is the mechanism behind the standard control-flow and collection operators:
 * - `if`/`and`/`or` evaluate only the branches they need (short-circuit).
 * - `map`/`filter`/`all`/`some`/`none` evaluate their sub-expression once per element,
 *   with the element itself as the data context.
 * - `reduce` evaluates its sub-expression against `{"current": ..., "accumulator": ...}`.
 *
 * Prefer [Operator] for ordinary computations over already-evaluated values.
 */
fun interface LazyOperator {
    /**
     * @param args the operator's raw, unevaluated arguments as they appear in the rule.
     *   A scalar argument in the rule (`{"op": x}`) arrives wrapped as a single-element list.
     * @param data the current data context the rule is being evaluated against.
     * @param evaluator evaluates sub-expressions; pass any data context you need.
     * @return the operator's result value.
     */
    fun apply(args: List<Any?>, data: Any?, evaluator: Evaluator): Any?
}
