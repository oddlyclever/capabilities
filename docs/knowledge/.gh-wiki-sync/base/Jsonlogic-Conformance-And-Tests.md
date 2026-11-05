# Conformance and Tests

DOC-0012 · 2026-11-04
Keywords: JSONLogic conformance suite, tests.json, spec-correct, shared test suite, 278 cases, JsonLogicConformanceTest, generated test file, do not edit by hand, update the suite, regenerate tests, run tests on every target, jsonlogic check, platform differences JVM JS, review regression tests, json-logic-js comparison

What makes this module correct is the shared conformance suite that jsonlogic.com publishes for every implementation. This page covers how the suite is held and run, and what the hand-written tests add.

## The conformance suite

The suite, [jsonlogic.com/tests.json](https://jsonlogic.com/tests.json), is vendored unmodified at [`src/commonTest/resources/tests.json`](https://github.com/oddlyclever/capabilities/blob/HEAD/jsonlogic/src/commonTest/resources/tests.json). It was retrieved on 2026-07-23 and has 278 cases. Each case is a `[rule, data, expected]` triplet, and its string entries are section headings.

The cases are transcribed into Kotlin in [`JsonLogicConformanceTest.kt`](https://github.com/oddlyclever/capabilities/blob/HEAD/jsonlogic/src/commonTest/kotlin/com/oddlyclever/jsonlogic/JsonLogicConformanceTest.kt), one test function per case. Each section heading becomes a comment and the prefix of the function names. The file is generated, and its header says not to edit it by hand. The generator is not in this repository, so when the suite is updated, the vendored `tests.json` is replaced and the Kotlin file is regenerated from it.

Passing the suite is the definition of spec-correct here. The three places where this module departs from json-logic-js all pass it; they are listed in [Standard Operators](Jsonlogic-Standard-Operators).

Each case is checked against `JsonLogic.Default` with a deep, JSON-typed comparison: numbers compare by value, since the suite writes `1` where evaluation may produce `1L` or `1.0`, lists compare element by element, maps key by key, and everything else strictly. A failure prints the rule, the data, and the expected and actual values with their types.

## The hand-written tests

They cover what the suite does not pin down.

| Test class | What it pins |
|---|---|
| `OperatorRegistrationTest` | Custom eager and lazy operators, scalar arguments wrapped in a list, overriding a standard operator without touching `JsonLogic.Default`, a lazy registration displacing an eager one, the unknown-operator exception, and the `log` sink. |
| `EvaluatorSemanticsTest` | `var` absent against present-but-`null`, short-circuiting in `and`, `or` and `if`, `reduce`'s evaluated starting accumulator, the normalization of results to `Long`, and multi-key maps passing through unevaluated. |
| `ReviewRegressionTest` | Cases where an implementation that leaned on each platform would answer differently on the JVM and in a browser: number formatting, `var` with `length` and non-canonical indices, `substr` with a fractional negative length, and `min` and `max` with negative zero. Each asserts one literal, the same on every target. |

## Running them

Every test is in `commonTest`, so it runs on every target:

```bash
./gradlew :jsonlogic:check
```

That is the bar, not a passing `jvmTest`. The point of the module is the same answer on every target, and a JVM-only run cannot show it. Android runs them as host tests.
