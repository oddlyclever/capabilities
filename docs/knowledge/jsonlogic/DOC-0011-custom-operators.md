# Custom Operators

DOC-0011 · 2026-11-04
Keywords: add custom JSONLogic operator, register operator, extend JSONLogic, JsonLogic.builder, operator, lazyOperator, eager versus lazy, short-circuit custom operator, Evaluator, evaluate, truthy, JsCoercion, toNumber, parseFloat, override standard operator, replace +, last registration wins, args as Int fails, ClassCastException, numbers arrive as Double, logger, log to logcat, domain-specific operators, client and server disagree

Custom operators are the module's extension point. This page covers the two kinds, how registration works, how to read arguments so they behave like the standard operators' arguments, and where an app's own operators belong.

## Building an instance

```kotlin
val logic = JsonLogic.builder()
    .operator("pow") { args, _ ->
        JsCoercion.toNumber(args.getOrNull(0)).pow(JsCoercion.toNumber(args.getOrNull(1)))
    }
    .lazyOperator("unless") { args, data, evaluator ->
        if (!evaluator.truthy(evaluator.evaluate(args.getOrNull(0), data))) evaluator.evaluate(args.getOrNull(1), data) else null
    }
    .build()

logic.apply(mapOf("pow" to listOf(2, 10))) // 1024L
```

`pow` is `kotlin.math.pow`. Every built instance starts from the standard operators and adds the registrations on top. Build it once and keep it: an instance is immutable and safe to share between threads, as long as the operators registered on it are.

## Eager or lazy

- **`operator(name) { args, data -> … }`** registers an `Operator`. Its arguments arrive already evaluated against the data. This is the kind most operators want.
- **`lazyOperator(name) { args, data, evaluator -> … }`** registers a `LazyOperator`. Its arguments arrive as written in the rule, with an `Evaluator`, and it decides what to evaluate, when, and against which data. That is how the standard `if`, `and` and `or` short-circuit, and how `map`, `filter` and `reduce` evaluate a sub-rule once per element. `evaluator.evaluate(rule, data)` evaluates a sub-rule against any data you choose, and `evaluator.truthy(value)` is JSONLogic's truthiness.

Either way, a single argument written without a list, `{"op": x}`, arrives as a one-element list.

## Registration

- **Registering a name that exists replaces it,** whether the existing one is standard or custom. The last registration wins. Replacing `+` on one instance leaves `JsonLogic.Default` untouched.
- **A name is either eager or lazy, never both.** Registering a lazy operator under the name of an eager one, standard or custom, removes the eager one, and the other way round.
- **`logger { value -> … }`** replaces where the standard `log` operator writes, which is `println` by default. Use it to send `log` to the app's own logging.

## Reading arguments

Results are normalized to `Long` and `Double` only on the way out of `apply`. Inside an operator, a number computed by another operator arrives as a `Double`, such as `3.0` from `+`, while a literal from the rule arrives as whatever type the rule held, often `Int`. So `args[0] as? Int` fails on computed values.

Read arguments with `JsCoercion`, which is public for this reason: an operator that treats `"2.5"` the way `+` or `<` would needs the same coercion they use, and hand-written `as? Number` checks quietly diverge from JSONLogic's semantics.

| Function | What it is |
|---|---|
| `JsCoercion.toNumber(v)` | JavaScript's `ToNumber`: the whole string or `NaN`, `null` is 0, booleans are 0 and 1. |
| `JsCoercion.parseFloat(v)` | JavaScript's `parseFloat`: the longest numeric prefix. |
| `JsCoercion.truthy(v)` | JSONLogic truthiness. |
| `JsCoercion.stringify(v)` | JavaScript's `String(v)`, with numbers written as JavaScript writes them. |
| `JsCoercion.looseEquals(a, b)`, `strictEquals(a, b)` | `==` and `===`. |
| `JsCoercion.compare(a, b)` | Relational comparison: negative, zero or positive, or `null` when the two cannot be compared. |

A custom operator can return any value. A whole `Double` it returns comes back from `apply` as a `Long`, like any other result.

## Where an app's operators belong

Operators specific to one domain or one app, such as distances, hashing or an app's business terms, do not go in this module. They live in the app, in a layer that depends on this module, registered with `operator` or `lazyOperator`. The module is mechanism, and what a rule means for a particular app is that app's policy.

When a client and a server must agree on a rule's result, both have to register the same custom operators with the same behaviour. A rule that names an operator one side lacks throws `JsonLogicException` there.
