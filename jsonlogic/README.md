# JSONLogic for Kotlin Multiplatform

A clean-room, dependency-free [JSONLogic](https://jsonlogic.com) rule evaluator in pure common Kotlin. One implementation, every target — `jvm`, `android`, `iosArm64`, `iosSimulatorArm64`, `js(browser)`, `wasmJs(browser)` — so a rule evaluates to the same result wherever it runs, **by construction**.

```
package   com.oddlyclever.jsonlogic
group     com.oddlyclever
```

## Why JSONLogic

A JSONLogic rule *is* a JSON tree — `{"op": [args...]}` — so there is no parser and no
grammar. The entire implementation is an evaluator over plain data structures, which is
what makes it ownable across every Kotlin target.

## Design

- **Serialization-agnostic.** Rules and data are generic structures —
  `Map<String, Any?>` / `List<Any?>` / `String` / `Boolean` / `Number` / `null` —
  duck-typed like the reference implementations. How JSON text becomes those structures
  (kotlinx.serialization, org.json, a Swift bridge…) is the caller's concern; this
  module depends on the Kotlin stdlib only.
- **JavaScript semantics, implemented algorithmically.** JSONLogic is specified by its
  JS reference implementation, so coercion (truthiness, `ToNumber`/`parseFloat`,
  `String()`, `==`/`===`) is reproduced faithfully — and deterministically, with no
  reliance on platform parsing/formatting quirks.
- **Deterministic numbers.** Arithmetic happens in `Double` (as in JS); results
  normalize integral values to `Long` on every target, so `1 + 2` is `3L` on the JVM,
  JS, Wasm, and iOS alike.

## Usage

```kotlin
val rule = mapOf(">" to listOf(mapOf("var" to "temp"), 100))
JsonLogic.Default.apply(rule, mapOf("temp" to 110)) // => true
```

Standard operators: `var`, `missing`, `missing_some`, `==`, `===`, `!=`, `!==`, `<`,
`<=`, `>`, `>=`, `and`, `or`, `!`, `!!`, `if`, `?:`, `in`, `map`, `filter`, `reduce`,
`all`, `some`, `none`, `merge`, `cat`, `substr`, `+`, `-`, `*`, `/`, `%`, `min`, `max`,
`log`.

### Custom operators

The registration interface is the module's extension point. Eager operators receive
evaluated arguments; lazy operators receive raw arguments plus the evaluator, for
short-circuiting or scoped evaluation (how `map`/`filter`/`reduce` work internally).

```kotlin
val logic = JsonLogic.builder()
    .operator("pow") { args, _ ->
        // JsCoercion is public so custom operators coerce exactly like standard ones
        JsCoercion.toNumber(args.getOrNull(0)).pow(JsCoercion.toNumber(args.getOrNull(1)))
    }
    .lazyOperator("unless") { args, data, ev ->
        if (!ev.truthy(ev.evaluate(args.getOrNull(0), data))) ev.evaluate(args.getOrNull(1), data) else null
    }
    .build()
```

Registering an existing name replaces it — last registration wins. Instances are
immutable and safe to share across threads.

> Domain-specific operators do **not** belong in this module. An app's own operators live in the app, in a layer that depends on this module.

## Conformance

The canonical shared conformance suite — https://jsonlogic.com/tests.json — is vendored
at [src/commonTest/resources/tests.json](src/commonTest/resources/tests.json) and
transcribed into generated Kotlin unit tests
([JsonLogicConformanceTest.kt](src/commonTest/kotlin/com/oddlyclever/jsonlogic/JsonLogicConformanceTest.kt),
278 cases), which run on **every** target. Passing that suite is the definition of
spec-correct. Hand-written tests cover what the suite doesn't pin down: operator
registration, short-circuit behavior, `var` absent-vs-null semantics, and result
normalization.

### Documented divergences from json-logic-js

All spec-conformant per the shared suite:

1. `reduce`'s initial accumulator is **evaluated** against the data context (the
   reference passes it through raw — widely considered a reference bug).
2. `*` with zero arguments returns `null` (the reference throws a `TypeError`).
3. `all`/`some` treat a non-list first argument as an empty list (the reference can
   crash there).

Number→string conversion follows the ECMA-262 `Number::toString` notation algorithm on
every target (`0.0001`, `12345678.5`, `1e+21`, `1e-7` — never the JVM's `1.0E-4`). The
platform's `Double.toString` contributes only the shortest-round-trip digits, which are
identical across the supported toolchains.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-jsonlogic`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-jsonlogic/SKILL.md](src/commonMain/skills/com-oddlyclever-jsonlogic/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-jsonlogic/`.

## Attribution

Clean-room implementation of the JSONLogic specification (https://jsonlogic.com).
Evaluation approach informed by
[allegro/json-logic-kmp](https://github.com/allegro/json-logic-kmp) (Apache-2.0) and
cross-checked against
[jamsesso/json-logic-java](https://github.com/jamsesso/json-logic-java) (MIT) and the
reference [json-logic-js](https://github.com/jwadhams/json-logic-js) (MIT). No code
copied. The conformance suite is published by jsonlogic.com for shared use by all
implementations.
