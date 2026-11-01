package com.oddlyclever.jsonlogic

/**
 * Evaluates JSONLogic expressions. Handed to [LazyOperator] implementations so they can
 * evaluate their raw arguments on their own terms (lazily, repeatedly, or against a
 * scoped data context).
 */
interface Evaluator {
    /**
     * Evaluates [rule] against [data] and returns the raw result.
     *
     * @param rule a JSONLogic expression: a `Map` of one operator name to its arguments,
     *   a `List` (evaluated element-wise), or a literal (returned as-is).
     * @param data the data context; sub-expressions like `{"var": "x"}` resolve against it.
     */
    fun evaluate(rule: Any?, data: Any?): Any?

    /**
     * JSONLogic truthiness of [value]: `null`, `false`, `0`, `NaN`, `""`, and empty
     * lists are falsy; everything else (including empty maps) is truthy.
     */
    fun truthy(value: Any?): Boolean
}
