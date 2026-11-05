# Rules, Data and Results

DOC-0009 · 2026-11-04
Keywords: evaluate JSONLogic rule Kotlin, JsonLogic.Default.apply, JSON string is not a rule, parse JSON rule, kotlinx.serialization JsonElement, JsonObject, JsonPrimitive comparisons false, NaN result, cat adds quote marks, result is Long not Int, result == 3 false, 3L, Double result, var dotted path, var default, absent versus null, list index in path, length, missing, missing_some, JsonLogicException, Unrecognized operation, multi-key map not evaluated, thread safe, JsonLogic() does not compile, evaluate method does not exist

`jsonlogic` evaluates a rule against some data and returns a value. This page covers what a rule and its data are, how a rule is read, how `var` finds values, what comes back, and the one thing that throws. The operators themselves are in [Standard Operators](DOC-0010-standard-operators.md).

## The call

```kotlin
val rule = mapOf(
    "and" to listOf(
        mapOf(">=" to listOf(mapOf("var" to "user.age"), 18)),
        mapOf("in" to listOf(mapOf("var" to "user.country"), listOf("CA", "US"))),
    )
)
val data = mapOf("user" to mapOf("age" to 21, "country" to "CA"))

val allowed = JsonLogic.Default.apply(rule, data) == true
```

`apply(rule, data)` returns the result directly; `data` defaults to `null`. There is no result wrapper, no `evaluate` on `JsonLogic`, and no method that takes a rule as a string.

`JsonLogic.Default` holds the standard operators. For custom operators or logging, build an instance with `JsonLogic.builder()` and keep it; see [Custom Operators](DOC-0011-custom-operators.md). There is no public constructor, so `JsonLogic()` does not compile. An instance is immutable once built and safe to share between threads.

## Rules and data are plain values

A rule and its data are made of `Map<String, Any?>`, `List<Any?>`, `String`, `Boolean`, any `Number`, and `null`. The module parses no JSON and depends on nothing beyond the Kotlin standard library. Turning JSON text into those values is the caller's choice: kotlinx.serialization, org.json, a bridge from Swift. Why is in [Plain Values Instead of a JSON Library](ADR-0005-plain-values-instead-of-a-json-library.md).

Two mistakes compile and run:

- **JSON text is not a rule.** `JsonLogic.Default.apply("""{"==": [1, 1]}""")` returns the string itself, not `true`. A string is a literal. Decode it first.
- **kotlinx.serialization's `JsonElement` has to be converted, not passed.** A `JsonObject` is a `Map`, so it is accepted, but its values are `JsonPrimitive`s, which the evaluator reads as unknown objects: comparisons come out `false`, arithmetic gives `NaN`, and `cat` puts quote marks into strings. Convert each element to a plain value first:

```kotlin
fun JsonElement.toPlain(): Any? = when (this) {
    JsonNull -> null
    is JsonPrimitive -> if (isString) content else booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
    is JsonArray -> map { it.toPlain() }
    is JsonObject -> mapValues { it.value.toPlain() }
}
```

Map keys are strings. `var` looks a key up by its string form, so a map keyed by anything else is never matched.

## How a rule is read

The evaluator walks the rule as data, the way the JavaScript reference implementation does:

- **A map with exactly one key is an operation:** `{"op": args}`. The key names the operator.
- **A map with zero keys, or more than one, is a literal** and is returned unevaluated. So `mapOf(">" to …, "<" to …)` silently does neither; each operation is its own single-key map.
- **A list is evaluated element by element.**
- **Anything else is a literal** and is returned as it is.
- **A single argument need not be in a list.** `{"var": "x"}` is read as `{"var": ["x"]}`.

## Reading data with var

`{"var": path}` or `{"var": [path, default]}` reads from the data.

- **The path uses dots,** and a number in it indexes a list: `"items.0.price"`. A number on its own works too: `{"var": 1}` against a list is its second element.
- **An empty or null path returns the whole data.** That is how `map` and `filter` read the current element, with `{"var": ""}`.
- **A missing key and a `null` value differ.** `{"var": ["a", "fallback"]}` gives `"fallback"` when `a` is absent, but `null` when `a` is present with a `null` value. A `null` partway down the path counts as absent, so the default is used.
- **Only a canonical index matches.** `"a.1"` is element 1, but `"a.01"` and `"a.+1"` are absent, as `arr["01"]` is in JavaScript.
- **`length` works on lists and strings,** as the JavaScript property does: `"a.length"` is the list's size. On a map, a key called `length` wins.
- **A string can be indexed:** `"s.1"` on `"abc"` is `"b"`.

`{"missing": [keys…]}` returns the keys that are absent, `null`, or `""`. If its first argument evaluates to a list, that list is the keys, so `{"missing": {"merge": …}}` works. `{"missing_some": [n, [keys…]]}` returns an empty list when at least `n` of the keys are present, and otherwise the missing ones.

## What comes back

Arithmetic happens in `Double`, as in JavaScript. On the way out of `apply`, every whole number is normalized to `Long`, on every target, and inside lists and maps too:

- `{"+": [1, 2]}` is `3L`, and `{"/": [4, 2]}` is `2L`.
- A fractional result stays `Double`: `{"+": [1, 2.5]}` is `3.5`.
- `NaN`, the infinities, and whole values beyond 2^53 stay `Double`, since they have no exact `Long`.
- An `Int`, `Short` or `Byte` that passes through, such as a literal from the rule, also comes back as `Long`.

So compare with `3L`, not `3`: `result == 3` is `false` when the result is `3L`. Or compare `(result as Number).toDouble()`.

Normalizing happens only at the end. A custom operator sees numbers as they are mid-evaluation; see [Custom Operators](DOC-0011-custom-operators.md).

Because arithmetic is in `Double`, a `Long` above 2^53 loses precision as soon as it goes through an arithmetic operator. Passed through untouched, or into `cat`, it keeps its digits.

## The one thing that throws

A rule that names an operator that is not registered throws `JsonLogicException`, a `RuntimeException`, with the message "Unrecognized operation: " and the name. It is the only structural error a rule can contain.

Everything else, such as the wrong number of arguments or mismatched types, gives a value by JavaScript's coercion rules instead of failing: `{"+": ["a", 1]}` is `NaN`, and `{"*": []}` is `null`. Check a result's type where it matters, rather than expecting an exception.
