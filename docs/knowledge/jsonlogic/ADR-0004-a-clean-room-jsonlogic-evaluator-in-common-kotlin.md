# A Clean-Room JSONLogic Evaluator in Common Kotlin

ADR-0004 · 2026-11-04 · Status: accepted
Keywords: rules engine Kotlin Multiplatform, business rules as data, feature flag rules, eligibility rules, rule stored in a database, same result on client and server, why JSONLogic, expression language parser, grammar, json-logic-kmp, json-logic-java, json-logic-js, platform-specific implementation, expect actual rule engine, clean-room, no code copied, dependency-free

## Context

Some rules have to live outside the code: whether a user qualifies, whether a form passes, whether a feature is on. Stored as data, a rule can be sent between a client and a server or changed without a release, and then the same rule must give the same result wherever it runs, or a check made on a phone and repeated on a server disagree. A rule language with its own syntax needs a parser and a grammar on every target. JSONLogic does not: a rule is a JSON tree, `{"op": [args…]}`, so the whole implementation is an evaluator over plain data. An implementation that leans on each platform for parsing or formatting numbers answers differently on the JVM than in a browser.

## Decision

The module is a JSONLogic evaluator written once, in common Kotlin, with no platform-specific code and no dependency beyond the standard library.

- **JSONLogic as the rule format,** because a rule is data and there is nothing to parse.
- **One implementation in `commonMain`, for every target:** jvm, android, iosArm64, iosSimulatorArm64, js and wasmJs. A rule gives the same result everywhere by construction, not by keeping several implementations in step.
- **Nothing delegated to the platform where platforms disagree.** Coercion and number formatting are done by algorithm in common code; see [JavaScript Semantics in Common Kotlin](ADR-0006-javascript-semantics-in-common-kotlin.md).
- **Clean-room.** It implements the specification at jsonlogic.com. Its evaluation approach was informed by allegro/json-logic-kmp, and its semantics cross-checked against jamsesso/json-logic-java and the reference json-logic-js. No code was copied from any of them.
- **The shared conformance suite is the definition of correct,** vendored and run on every target.
- **Only the standard operators ship.** Domain operators belong to the app, registered through the builder.

## Consequences

- The same rule gives the same answer on a phone, in a browser and on a JVM server.
- The module carries no third-party code and no third-party dependency.
- Every edge of JavaScript coercion that the reference implementation has, this module has to reproduce by hand, and test on every target.
- It is called from Kotlin only. There is no Swift or JavaScript surface.
- An app that needs its own operators has to register the same ones on every side that evaluates its rules.
