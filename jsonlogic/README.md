# jsonlogic

A clean-room, dependency-free [JSONLogic](https://jsonlogic.com) rule evaluator in pure common Kotlin. One implementation on every target, so a rule evaluates to the same result wherever it runs, by construction.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:jsonlogic:0.0.8")
```

The package is `com.oddlyclever.jsonlogic`. It depends on the Kotlin standard library only.

## Use

A rule is a JSONLogic expression as plain Kotlin values: maps, lists, strings, booleans, numbers and `null`. Turn JSON text into those values with your own JSON library first; the module parses nothing.

```kotlin
val rule = mapOf(">" to listOf(mapOf("var" to "temp"), 100))
JsonLogic.Default.apply(rule, mapOf("temp" to 110)) // true
```

Whole numbers come back as `Long` on every target, so `{"+": [1, 2]}` is `3L`. Only a rule naming an unknown operator throws, with `JsonLogicException`; anything else malformed gives a value by JavaScript's rules.

Custom operators are the extension point. An eager operator receives its arguments evaluated; a lazy one receives them raw, with the evaluator, to short-circuit or evaluate against other data. `JsCoercion` is public so a custom operator coerces exactly like the standard ones.

```kotlin
val logic = JsonLogic.builder()
    .operator("pow") { args, _ ->
        JsCoercion.toNumber(args.getOrNull(0)).pow(JsCoercion.toNumber(args.getOrNull(1)))
    }
    .lazyOperator("unless") { args, data, ev ->
        if (!ev.truthy(ev.evaluate(args.getOrNull(0), data))) ev.evaluate(args.getOrNull(1), data) else null
    }
    .build()
```

Registering a name that exists replaces it; the last registration wins. Instances are immutable and safe to share between threads. Domain-specific operators do not belong in this module: an app's own operators live in the app, in a layer that depends on this module.

## Documentation

The full documentation is in the module's section of the [knowledge base](../docs/knowledge/jsonlogic/README.md), which is mirrored to the repository's wiki:

- [Rules, Data and Results](../docs/knowledge/jsonlogic/DOC-0009-rules-data-and-results.md): the value model, converting JSON, `var`, results, and errors.
- [Standard Operators](../docs/knowledge/jsonlogic/DOC-0010-standard-operators.md): every operator and its JavaScript semantics, and the divergences from json-logic-js.
- [Custom Operators](../docs/knowledge/jsonlogic/DOC-0011-custom-operators.md): eager and lazy operators, registration, and `JsCoercion`.
- [Conformance and Tests](../docs/knowledge/jsonlogic/DOC-0012-conformance-and-tests.md): the shared conformance suite and the tests that run on every target.
- [A Clean-Room JSONLogic Evaluator in Common Kotlin](../docs/knowledge/jsonlogic/ADR-0004-a-clean-room-jsonlogic-evaluator-in-common-kotlin.md), [Plain Values Instead of a JSON Library](../docs/knowledge/jsonlogic/ADR-0005-plain-values-instead-of-a-json-library.md) and [JavaScript Semantics in Common Kotlin](../docs/knowledge/jsonlogic/ADR-0006-javascript-semantics-in-common-kotlin.md): why it is built the way it is.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-jsonlogic`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-jsonlogic/SKILL.md](src/commonMain/skills/com-oddlyclever-jsonlogic/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-jsonlogic/`.

## Attribution

Clean-room implementation of the JSONLogic specification (https://jsonlogic.com). Evaluation approach informed by [allegro/json-logic-kmp](https://github.com/allegro/json-logic-kmp) (Apache-2.0) and cross-checked against [jamsesso/json-logic-java](https://github.com/jamsesso/json-logic-java) (MIT) and the reference [json-logic-js](https://github.com/jwadhams/json-logic-js) (MIT). No code copied. The conformance suite is published by jsonlogic.com for shared use by all implementations.
