# platform

What must exist exactly once in a process. On Android, that is the application context and the activity in front, captured at process start so no library has to ask for them. On every target, it is `OsFamily`.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs. The application context and the activity are Android only; the other targets have `OsFamily`.

## Install

```kotlin
implementation("com.oddlyclever:platform:0.0.8")
```

There is nothing to call and no dependency-injection container. On Android the initializer arrives in the library's manifest and runs by itself, before `Application.onCreate`. An app that removes App Startup's provider installs it by hand, as [Start-Up and the Manual Install](../docs/knowledge/platform/DOC-0021-start-up-and-the-manual-install.md) describes.

## Use

```kotlin
// The application context, as a default parameter: callers write SomeCapability() and pass nothing.
class SomeCapability(private val context: Context = AppContext.get())

// The activity in front, for a permission request, an activity result or a system sheet.
val activity: ComponentActivity? = ActivityProvider.get().current.value

// The operating system family, compared rather than detected.
val showsShare = family.isVisibleUnless(OsFamily.WEB, OsFamily.WASM)
```

`AppContext.get()` is safe to call in a constructor, because the context is captured before any application code runs. `AppContext.getOrNull()` is for a caller that can do without one.

## Documentation

The full documentation is in the module's section of the [knowledge base](../docs/knowledge/platform/README.md), which is mirrored to the repository's wiki:

- [The Application Context and the Activity in Front](../docs/knowledge/platform/DOC-0020-the-application-context-and-the-activity-in-front.md): `AppContext` and `ActivityProvider` in detail, and what the activity provider reports.
- [Start-Up and the Manual Install](../docs/knowledge/platform/DOC-0021-start-up-and-the-manual-install.md): how the initializer runs, installing it by hand, the failures, and the device tests.
- [OsFamily](../docs/knowledge/platform/DOC-0022-osfamily.md): the members, the visibility checks, and why the members are upper case.
- [One Holder per Process](../docs/knowledge/platform/ADR-0012-one-holder-per-process.md): why these live in one module, and what belongs in it.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-platform`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-platform/SKILL.md](src/commonMain/skills/com-oddlyclever-platform/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-platform/`.
