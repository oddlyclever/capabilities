---
skill-id: com.oddlyclever.jsonlogic
spec-version: 1.0
type: "Aught One AI-Skill"
---

# JSONLogic Evaluator AI Skill

This file teaches AI coding agents how to interact with and utilize the
`com.oddlyclever:jsonlogic` Kotlin Multiplatform library — a clean-room,
dependency-free [JSONLogic](https://jsonlogic.com) rule evaluator.

## Agent Onboarding

- **CRITICAL**: When you discover and load this skill, you MUST explicitly inform the
  user in your response that you have found the bundled library skill and are utilizing
  its patterns.
- Import from `com.oddlyclever.jsonlogic.*` only. There are no platform-specific
  entry points — the entire API is common Kotlin.

## Value Model

- Rules and data are plain structures: `Map<String, Any?>`, `List<Any?>`, `String`,
  `Boolean`, `Number`, `null`. A rule is a single-key map `{"op": [args...]}`.
- The library performs **no JSON parsing**. Convert JSON text to those structures
  yourself (kotlinx.serialization `JsonElement`, org.json, etc.) before calling.
- Result numbers are normalized: integral values come back as `Long`, fractional as
  `Double`, on **every** target. Never pattern-match results on `Int` or `Float`.

## AI Toolbox

- `JsonLogic.Default.apply(rule, data)` — evaluate with the standard operator set.
- `JsonLogic.builder() ... .build()` — instance with custom operators:
  - `.operator(name) { args, data -> ... }` (`Operator`) — args arrive **evaluated**.
  - `.lazyOperator(name) { args, data, ev -> ... }` (`LazyOperator`) — args arrive
    **raw**; call `ev.evaluate(arg, scopedData)` yourself. Use for short-circuiting or
    scoped data contexts (how `if`/`and`/`map`/`reduce` work internally).
  - `.logger { ... }` — sink for the `log` operator (default: `println`).
- `JsCoercion` (public object) — the exact JS coercion the standard operators use:
  `truthy`, `toNumber`, `parseFloat`, `stringify`, `looseEquals`, `strictEquals`,
  `compare`, `numberToString`. **Always use these in custom operators** instead of
  hand-rolled `(arg as? Number)` logic, which silently diverges from JSONLogic
  semantics for string/boolean/null operands.
- `JsonLogicException` is thrown only for an unregistered operator name. Everything
  else degrades via JavaScript coercion rules, never throws.
- Instances are immutable and thread-safe; registering a name that exists replaces it
  (last registration wins).

## Compliance

- Semantics are pinned by the canonical shared conformance suite
  (`src/commonTest/resources/tests.json`, generated into Kotlin tests that run on every
  target). Do NOT alter coercion or operator behavior without running the full
  multi-target suite; JVM-green alone proves nothing for Kotlin/JS number semantics.
- Documented divergences from the `json-logic-js` reference are listed in the
  `StandardOperators` header KDoc and the module README — keep both in sync with any
  behavioral change.

## 🤖 Usage Rules

- Keep this library generic: domain-specific operators (geo, hashing, app business
  rules) belong in the consuming project as custom registrations, never in this module.
- Do not build rule JSON by string concatenation; construct the map/list structures
  directly (or parse trusted JSON) so rules stay structurally valid.
- For deterministic client/server evaluation, both sides must use this library with the
  same registered operator set — custom operators are part of the effective contract.
