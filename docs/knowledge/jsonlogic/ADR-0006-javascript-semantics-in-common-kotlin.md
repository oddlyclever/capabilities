# JavaScript Semantics in Common Kotlin

ADR-0006 · 2026-11-04 · Status: accepted
Keywords: why does JSONLogic behave like JavaScript, type coercion, truthiness, loose equality, parseFloat, ToNumber, Number toString, 1.0E-4 on the JVM, scientific notation differs by platform, Double.toString differs, results as Long, Int on JVM but not on JS, Kotlin/JS Int and Double indistinguishable, 2^53, why JsCoercion is public, divergences from json-logic-js, reduce bug in reference

## Context

JSONLogic is specified by its JavaScript reference implementation, so a correct evaluator has to reproduce JavaScript's truthiness, its `ToNumber` and `parseFloat`, its `String()` conversion, and its `==` and `===`. Kotlin's platforms disagree at exactly those points. The JVM's `Double.toString` writes `0.0001` as `1.0E-4` where JavaScript writes `0.0001`. On Kotlin/JS every whole number is both an `Int` and a `Double` at runtime, so a type check that is reliable on the JVM is not on JS. Leaning on the platform would give different answers per target, which is the one thing the module exists not to do.

## Decision

JavaScript's semantics are reproduced by algorithm in common code, in `JsCoercion`, with nothing delegated to the platform where platforms differ.

- **Numeric strings are checked against JavaScript's number grammar** before they are parsed.
- **Numbers are written with ECMA-262's `Number::toString` notation algorithm.** The platform's `Double.toString` supplies only the shortest round-trip digits, which are the same on every supported toolchain, and never the notation.
- **Arithmetic is in `Double`, as in JavaScript, and whole results surface as `Long`** on every target. Without that step the same rule would return an `Int` on the JVM and something else on JS. `Long` is also how a JSON integer serializes. Values beyond 2^53 stay `Double`, since not every whole number is exact there.
- **`JsCoercion` is public,** because custom operators are the extension point, and an operator that reads `"2.5"` the way `+` or `<` would needs the coercion the standard operators use.
- **It departs from json-logic-js in three places,** each still passing the shared suite: `reduce` evaluates its starting accumulator, which the reference passes through raw and which is widely considered a bug in the reference; `*` with no arguments is `null` rather than a thrown `TypeError`; and `all` and `some` treat a non-list as empty where the reference can crash.

## Consequences

- A rule's result, including how its numbers are written, is the same on every target.
- Callers compare results with `Long` values, `3L` rather than `3`, which surprises Kotlin code that expects `Int`.
- Custom operators see numbers before normalization, so they read them through `JsCoercion` rather than casting.
- Malformed input degrades to a value, as in JavaScript, rather than throwing; only an unknown operator throws.
- Each JavaScript edge reproduced is code to keep correct, pinned by the regression tests in [Conformance and Tests](DOC-0012-conformance-and-tests.md).
