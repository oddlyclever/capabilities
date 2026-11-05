# jsonlogic

The full documentation of [`jsonlogic`](https://github.com/oddlyclever/capabilities/blob/HEAD/jsonlogic/README.md): a clean-room JSONLogic rule evaluator in pure common Kotlin, giving the same result on every target. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [Rules, Data and Results](Jsonlogic-Rules-Data-And-Results): plain values in, converting JSON, how a rule is read, `var` paths, results as `Long` and `Double`, and the one thing that throws.
- [Standard Operators](Jsonlogic-Standard-Operators): every standard operator with its JavaScript semantics and edges, and the three places it departs from json-logic-js.
- [Custom Operators](Jsonlogic-Custom-Operators): eager and lazy operators, registration and overriding, reading arguments with `JsCoercion`, and where an app's operators belong.
- [Conformance and Tests](Jsonlogic-Conformance-And-Tests): the vendored conformance suite, the generated tests, what the hand-written tests pin, and running them on every target.

## Why it is built this way

- [A Clean-Room JSONLogic Evaluator in Common Kotlin](Jsonlogic-A-Clean-Room-Jsonlogic-Evaluator-In-Common-Kotlin): why JSONLogic, and why one implementation in common code with no platform code.
- [Plain Values Instead of a JSON Library](Jsonlogic-Plain-Values-Instead-Of-A-Json-Library): why rules and data are maps and lists, and JSON is the caller's concern.
- [JavaScript Semantics in Common Kotlin](Jsonlogic-Javascript-Semantics-In-Common-Kotlin): why coercion and number formatting are JavaScript's, done by algorithm, and why results are `Long`.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
