# Standard Operators

DOC-0010 · 2026-11-04
Keywords: JSONLogic operators list, truthiness, empty list falsy, empty map truthy, == versus ===, loose equality, type coercion, between, < with three arguments, parseFloat, ToNumber, NaN, string concatenation cat, substr negative, number to string 1.0E-4, scientific notation, in operator, merge, if elseif else, ?: operator, and or return value, map filter reduce, all some none, empty list all false, reduce initial accumulator, log operator, divergences from json-logic-js, differences from reference implementation, TypeError

`JsonLogic.Default` has the standard operator set of [jsonlogic.com](https://jsonlogic.com/operations.html), with the JavaScript semantics of its reference implementation, json-logic-js. This page covers what each group does here, including the edges, and the three places this module departs from json-logic-js. Why the semantics are JavaScript's is in [JavaScript Semantics in Common Kotlin](ADR-0006-javascript-semantics-in-common-kotlin.md).

The set: `var`, `missing`, `missing_some`, `==`, `===`, `!=`, `!==`, `<`, `<=`, `>`, `>=`, `and`, `or`, `!`, `!!`, `if`, `?:`, `in`, `map`, `filter`, `reduce`, `all`, `some`, `none`, `merge`, `cat`, `substr`, `+`, `-`, `*`, `/`, `%`, `min`, `max`, `log`. The data operators, `var`, `missing` and `missing_some`, are in [Rules, Data and Results](DOC-0009-rules-data-and-results.md).

## Truthiness

`null`, `false`, `0`, `NaN`, `""` and an empty list are falsy. Everything else is truthy, including an empty map and the string `"0"`. The empty list is JSONLogic's own rule; the rest is JavaScript's. `!` negates truthiness, and `!!` converts to it.

## Equality and comparison

- **`==` and `!=` are JavaScript's loose equality.** `{"==": [1, "1"]}` is `true`. A boolean is compared as a number, a list or map is turned into its string form when compared with a string or number, and `null` equals only `null`. Two lists or maps are equal only if they are the same object, so two equal-looking lists are not `==`.
- **`===` and `!==` are strict equality:** the same type and value. Numbers compare by value whatever their Kotlin type, so `1 === 1.0` is `true`.
- **`<`, `<=`, `>` and `>=`** first turn a list or map into its string form. Then two strings compare as strings, and anything else as numbers. When either side cannot become a number, every comparison is `false`, as in JavaScript.
- **`<` and `<=` take a third argument** for "between": `{"<": [1, {"var": "x"}, 10]}` is whether `x` is strictly between 1 and 10. `{"<=": [1, x, 10]}` includes the ends.

## Arithmetic

- **`+` and `*` read their arguments with `parseFloat`,** which takes the longest numeric prefix: `"3 apples"` is 3. **`-`, `/`, `%`, `min` and `max` use JavaScript's `ToNumber`**, which reads the whole string or gives `NaN`. This split follows json-logic-js.
- **`+` with one argument** converts it to a number, and with none is `0`.
- **`-` with one argument** negates it. With more than two, only the first two are used. With none it is `null`.
- **`*` with one argument** returns it unconverted, as JavaScript's `Array.reduce` does. With none it is `null`; see the divergences below.
- **`/` and `%`** follow IEEE arithmetic: dividing by zero gives an infinity or `NaN`, not an error.
- **`min` and `max`** behave like `Math.min` and `Math.max`: `NaN` wins, `min` of `0` and `-0` is `-0`, and with no arguments `min` is `Infinity` and `max` is `-Infinity`.

## Strings

- **`cat`** joins its arguments with nothing between them. A `null` argument is `""`, as in `Array.prototype.join`.
- **Numbers are written as JavaScript writes them,** on every target: `3`, not `3.0`; `0.0001`, not the JVM's `1.0E-4`; `12345678.5`; exponential from `1e+21` up and from `1e-7` down. `-0` is `"0"`. Booleans are `"true"` and `"false"`, a list is its elements joined with commas, and a map is `"[object Object]"`.
- **`substr`** is JavaScript's `String.prototype.substr(start, length)`. A negative start counts from the end. A negative length trims that many characters from the end of what remains; a fractional one, such as `-1.5`, has the sum truncated, as JavaScript's `ToInteger` does, so `{"substr": ["jsonlogic", 0, -1.5]}` is `"jsonlog"`.
- **`in`** with a string is whether the string contains the first argument, turned into a string. With a list it is whether any element is strictly equal to it. With anything else it is `false`.

## Lists

- **`merge`** flattens its arguments by one level into a single list. A non-list argument becomes one element.
- **`map`, `filter`, `all`, `some` and `none`** evaluate their second argument once per element, with the element as the data, so `{"var": ""}` is the element itself. A first argument that is not a list is treated as an empty list.
- **`all` of an empty list is `false`.** `some` of an empty list is `false`, and `none` is `true`.
- **`reduce`** evaluates its second argument against `{"current": element, "accumulator": value}` for each element. Its third argument, the starting accumulator, is evaluated against the data; with none, it starts at `null`. Over something that is not a list, it returns the starting value.

Together they allow count-style rules, such as "at least three items are checked":

```json
{">=": [{"reduce": [{"filter": [{"var": "items"}, {"var": "checked"}]}, {"+": [{"var": "accumulator"}, 1]}, 0]}, 3]}
```

## Control flow

- **`if`** takes `[condition, then, condition, then, …, else]`: the first true condition's value, or the trailing `else`, or `null` when there is none. `?:` is the same operator.
- **`and`** returns the first falsy operand, or the last one. **`or`** returns the first truthy operand, or the last one. Neither returns a plain boolean unless its operands are. With no operands both are `null`.
- **`if`, `and` and `or` short-circuit.** An operand or branch they do not need is never evaluated, which matters when a custom operator has a side effect.

## log

`{"log": value}` passes its value through and writes it to the instance's logger, which is `println` by default. `JsonLogic.builder().logger { … }` replaces it.

## Where it departs from json-logic-js

The shared conformance suite passes with all three; see [Conformance and Tests](DOC-0012-conformance-and-tests.md).

1. **`reduce` evaluates its starting accumulator** against the data. json-logic-js passes it through unevaluated, which is widely considered a bug in the reference.
2. **`*` with no arguments returns `null`.** json-logic-js throws a `TypeError`. The suite does not test it.
3. **`all` and `some` treat a first argument that is not a list as an empty list.** json-logic-js can crash there, on `null.length`.
