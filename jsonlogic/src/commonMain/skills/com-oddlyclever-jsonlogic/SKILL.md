---
name: com-oddlyclever-jsonlogic
description: >-
  Evaluate JSONLogic rules in Kotlin Multiplatform: decide true or false, or
  compute a value, from a rule stored as JSON data, with the same answer on the
  JVM, Android, iOS, JS and Wasm. Use instead of writing a rules engine, a
  feature-flag or eligibility check, a pricing or discount rule, form
  validation or a filter that must be stored, sent between client and server,
  or edited without shipping code. Covers comparisons, and/or/not, if-then-else,
  variables read from nested data by dotted path, missing-field checks,
  arithmetic, min/max, string concatenation and substrings, membership, and
  map/filter/reduce/all/some/none over lists, plus custom operators of your own.
  Takes rules and data as plain maps and lists; it does not parse JSON text.
license: Apache-2.0
metadata:
  version: "0.0.8"
  repository: https://github.com/oddlyclever/capabilities
---

# jsonlogic

`com.oddlyclever:jsonlogic`, package `com.oddlyclever.jsonlogic`. Called from Kotlin only: there is no Swift or JavaScript surface.

## What it solves

A business rule that has to live outside the code: whether a user qualifies for an offer, whether a form passes, whether a feature is on, what a discount comes to. The rule is written as a JSONLogic expression, which is ordinary JSON, so it can be stored in a database, sent from a server, or edited in a tool without a release. The same rule gives the same result wherever it runs, because there is one implementation in common Kotlin with no platform code, so a check made on a phone and repeated on a server agree.

## How it is meant to be used

Turn the JSON into plain Kotlin values first, then evaluate. A rule is a map of one operator name to its arguments; data is whatever the rule reads with `var`.

```kotlin
import com.oddlyclever.jsonlogic.JsonLogic

val rule = mapOf(
    "and" to listOf(
        mapOf(">=" to listOf(mapOf("var" to "user.age"), 18)),
        mapOf("in" to listOf(mapOf("var" to "user.country"), listOf("CA", "US"))),
    )
)
val data = mapOf("user" to mapOf("age" to 21, "country" to "CA"))

val allowed = JsonLogic.Default.apply(rule, data) == true
```

`JsonLogic.Default` holds the standard operators and is safe to share between threads. For operators of your own, build an instance once and keep it:

```kotlin
import com.oddlyclever.jsonlogic.JsCoercion
import com.oddlyclever.jsonlogic.JsonLogic
import kotlin.math.pow

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

An `operator` receives its arguments already evaluated. A `lazyOperator` receives them as written in the rule, with the evaluator, for when it decides what to evaluate and against which data, as `if`, `and` and `map` do. Registering a name that exists, standard or custom, replaces it. `logger { }` replaces where the `log` operator writes, which is `println` by default.

## Invariants and traps

**JSON text is not a rule.** `JsonLogic.Default.apply("""{"==": [1, 1]}""")` returns the string itself, not `true`: a string is a literal, and the library parses nothing. Decode the text with your JSON library and pass the resulting maps and lists.

**kotlinx.serialization's `JsonElement` must be converted, not passed.** A `JsonObject` is a `Map`, so it compiles and runs, but its values are `JsonPrimitive`s, which the rules read as unknown objects: comparisons come out `false`, arithmetic gives `NaN`, and `cat` puts quote marks into strings. Convert first, a `JsonPrimitive` to its `String`, `Boolean`, number or `null`, a `JsonArray` to a `List` and a `JsonObject` to a `Map`.

**Results are `Long` or `Double`, never `Int`.** Every whole number in the result comes back as `Long` on every target, and fractional ones as `Double`, inside lists and maps too. `result == 3` is `false` when the result is `3L`; compare with `3L`, or with `(result as Number).toDouble()`.

**Custom operators see unnormalized numbers.** Normalizing happens only on the way out of `apply`. Inside an `operator`, an argument computed by `+` arrives as `3.0`, while a literal from the rule arrives as whatever type the rule held, often `Int`. `args[0] as? Int` therefore fails on computed values. Read numbers with `JsCoercion.toNumber`, and booleans with `JsCoercion.truthy`, which coerce the way the standard operators do.

**Only an unknown operator throws.** A rule naming an unregistered operator throws `JsonLogicException` ("Unrecognized operation: …"). Everything else, such as wrong argument counts or mismatched types, gives a value by JavaScript's coercion rules instead of failing: `{"+": ["a", 1]}` is `NaN`, `{"*": []}` is `null`. Check a result's type when it matters, rather than expecting an exception.

**A map with zero or several keys is data, not an operation.** `mapOf("a" to 1, "b" to 2)` evaluates to itself, so a rule built with two operators in one map silently does neither. Each operation is its own single-key map.

**A missing key and a `null` value differ.** `{"var": ["a", "fallback"]}` gives `"fallback"` when `a` is absent, but `null` when `a` is present with a `null` value. Paths use dots, and a number in a path indexes a list: `"items.0.price"`.

## What moved, and what it used to be called

`jsonlogic` was first released in 0.0.6, so no earlier versions exist.

- **Its agent skill moved in 0.0.7.** In 0.0.6 it was `META-INF/ai-skills/com.oddlyclever.jsonlogic.ai-skill.md` among the resources. It is now this file, in the sources jar. Ignore the old file if a copy turns up.
- **There is no public constructor.** `JsonLogic()` doesn't compile: use `JsonLogic.Default`, or `JsonLogic.builder()…build()` for custom operators.
- **The call is `apply(rule, data)`,** returning the value directly. There is no `evaluate` on `JsonLogic`, no result wrapper, and no rule-from-string method.

## What it is not for

It evaluates rules; it doesn't parse JSON, author or validate rule documents, or store them. Operators specific to one domain or one app, such as distances, hashing or an app's business terms, don't belong in this module. Register them in the app with `operator` or `lazyOperator`. When a client and a server must agree on a rule's result, both need the same set of custom operators registered.
