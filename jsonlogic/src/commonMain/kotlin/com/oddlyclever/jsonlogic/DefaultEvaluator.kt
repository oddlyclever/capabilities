package com.oddlyclever.jsonlogic

/**
 * The recursive JSONLogic tree walker.
 *
 * A rule is data: a single-key `Map` is an operation node (`{"op": args}`), a `List`
 * is evaluated element-wise, and everything else — including maps with zero or more
 * than one key — is a literal that passes through unchanged, exactly as in the
 * json-logic-js reference implementation.
 *
 * Dispatch: [LazyOperator]s receive their arguments raw (plus this evaluator) so they
 * can short-circuit or scope the data context; [Operator]s receive arguments already
 * evaluated. A scalar argument (`{"var": "x"}`) is wrapped to a single-element list
 * before dispatch, mirroring the reference implementation's unary sugar.
 */
internal class DefaultEvaluator(
    private val eagerOperators: Map<String, Operator>,
    private val lazyOperators: Map<String, LazyOperator>,
) : Evaluator {

    override fun evaluate(rule: Any?, data: Any?): Any? = when {
        rule is List<*> -> rule.map { evaluate(it, data) }
        rule !is Map<*, *> -> rule
        rule.size != 1 -> rule // non-rule objects pass through untouched
        else -> {
            val name = rule.keys.first().toString()
            val rawArgs = rule.values.first()
            val args = if (rawArgs is List<*>) rawArgs else listOf(rawArgs)
            val lazyOperator = lazyOperators[name]
            if (lazyOperator != null) {
                lazyOperator.apply(args, data, this)
            } else {
                val operator = eagerOperators[name]
                    ?: throw JsonLogicException("Unrecognized operation: $name")
                operator.apply(args.map { evaluate(it, data) }, data)
            }
        }
    }

    override fun truthy(value: Any?): Boolean = JsCoercion.truthy(value)
}
