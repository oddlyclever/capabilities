# The dependency rules file

`gradle/dependency-rules.toml` says what the version catalog's versions must agree with, beyond "the newest stable release". It sits beside `libs.versions.toml` because it describes the build, not any tool: a person or a CI check can read it as well as the planner can.

Every table is optional, and a key that no table names is independent: it moves to its newest stable release. Every key a table names must exist in the catalog's `[versions]`, or the planner refuses the file. Give every entry a reason. The catalog's comment says why to a reader of the build; the reason here is what the maintainer sees in every plan.

## Followers

A follower takes the version that a module published at its leader's version requires of it.

```toml
[followers.skiko]
leader = "compose-multiplatform"               # a [versions] key
via = "org.jetbrains.compose.ui:ui-graphics"   # group:artifact, published at the leader's version
group = "org.jetbrains.skiko"                  # the follower's group among that module's dependencies
module = "skiko"                               # which module of the group, when it has several
```

The planner reads `via` at the leader's *proposed* version, from its Gradle module metadata or, failing that, its POM, and proposes exactly what it asks for. If the answer is not a single version, the key comes back `unknown` with what was found. A follower may lead another; the planner resolves leaders first.

Choose `via` as the module that actually depends on the follower. For Compose that is `ui-graphics`, not the Gradle plugin, which declares no Skiko dependency. Confirm the choice with `scripts/versions.py requires` before writing the rule.

## Held

A held key is never proposed. It is listed in every plan with its reason.

```toml
[held.android-minSdk]
reason = "Kept low so consuming apps are not forced up."

[held.webpack]
reason = "Overrides the webpack the Kotlin toolchain ships; remove the override once Kotlin's own is fixed."
npm = "webpack"                                # optional: report the package's newest anyway
```

Hold the project's own settings too (its version, group and package prefix), so they read as decided rather than as unknown.

## Sets

A set moves only when every member has the same newer stable release. Otherwise every member holds, and the plan says which member is behind.

```toml
[sets.h3]
keys = ["uberH3", "h3Js"]
npm = { h3Js = "h3-js" }                       # members whose version is an npm package
vendored = "geo/native"                        # moves by that directory's own update script
reason = "Every target must run the same H3 release, or they answer differently for the same input."
```

`vendored` names native source committed to the repository. When the set moves, its update script re-vendors it, and the plan carries the path so that step is not forgotten.

## Verify

```toml
[verify]
commands = ["./gradlew clean check"]
```

The commands that prove a round, run in order, each of which must succeed. Use what the project's own instructions call verification. For a multiplatform library that means every target, not only the JVM.
