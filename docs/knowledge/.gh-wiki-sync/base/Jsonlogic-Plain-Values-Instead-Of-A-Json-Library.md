# Plain Values Instead of a JSON Library

ADR-0005 · 2026-11-04 · Status: accepted
Keywords: kotlinx.serialization JsonElement support, JsonObject input, org.json, Gson, Moshi, parse JSON string rule, serialization-agnostic, Map<String, Any?>, List<Any?>, Any? API, duck typing, why no JSON parser, dependency-free, Swift bridge

## Context

A JSONLogic rule arrives as JSON, and the JSON can come from many places: kotlinx.serialization, org.json, a value bridged from Swift. Taking one library's types in the API would make that library a dependency of the module and of every app that uses it.

## Decision

Rules and data are plain Kotlin values: `Map<String, Any?>`, `List<Any?>`, `String`, `Boolean`, `Number` and `null`, read by their shape as the reference implementations read JavaScript values. How JSON text becomes those values is the caller's concern. The module parses nothing and depends on the Kotlin standard library only.

## Consequences

- Any JSON library works, and the module adds no dependency to an app.
- The API is typed `Any?`. Nothing stops a caller passing JSON text, which evaluates to itself, or a kotlinx.serialization `JsonObject`, which is a `Map` but holds `JsonPrimitive`s the evaluator cannot read. Both compile and give wrong answers. [Rules, Data and Results](Jsonlogic-Rules-Data-And-Results) shows the conversion.
- Results come back as the same plain values, so the caller turns them back into JSON with its own library too.
