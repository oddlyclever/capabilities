# platform

What must exist exactly once in a process. On Android, that is the application context and the activity in front, captured at process start so no library has to ask for them. On every target, it is `OsFamily`.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:platform:0.0.7")
```

There is nothing to call and no dependency-injection container. On Android the initializer arrives in the library's manifest and runs by itself.

## The application context

```kotlin
class SomeCapability(private val context: Context = AppContext.get())
```

`AppContext.get()` returns the application context, captured before `Application.onCreate`. The default parameter is the point. Dependency injection can still hand a class a context, but a caller without it writes `SomeCapability()` and passes no context around. Without this, a `Context` parameter on one constructor becomes a `Context` parameter on whatever builds it, and on whatever builds that.

- **`AppContext.get()`** throws, with a message saying how the context was meant to arrive, when it never did. **`AppContext.getOrNull()`** is for a caller that can do without one.
- **Safe to read in a constructor.** `PlatformInitializer` runs through App Startup, from a content provider, and content providers are created before `Application.onCreate`, so no application code can run first.
- **Held statically, safely,** because it is the application context: it lives as long as the process and leaks nothing.

## The activity in front

```kotlin
val activity: ComponentActivity? = ActivityProvider.get().current.value
```

`ActivityProvider.get().current` is a `StateFlow` of the activity created or resumed most recently and not yet destroyed, for a permission request, an activity result or a system sheet.

- **One per process, started with the context.** It is registered before the first activity is created, so it sees that activity. It cannot be constructed: one made later would report no activity until the next resume, and two would each register their own lifecycle callbacks.
- **In a dependency-injection container,** bind the instance: `single { ActivityProvider.get() }`.

## Why these live in one module

Two static application-context holders in one process mean two initializers filling two statics. Nothing errors. Whichever one a module reads next may be the empty one, and that module fails much later, somewhere unrelated. The same goes for two activity trackers. So there is one holder and one initializer, and every library that needs either depends on this module rather than keeping its own.

That is also the rule for what belongs here: only what has to exist exactly once in a process. Anything else is a library of its own.

## When App Startup's provider is removed

Some apps strip App Startup's provider from their merged manifest to control when initializers run. That removes this one too, so install it by hand, first thing:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PlatformInitializer.install(this)
    }
}
```

It is harmless to call when the provider did run. Activities created before the call are not seen until they next resume.

## OsFamily

`OsFamily` names the operating system family, for the few decisions that depend on it. `isVisibleIf(…)` and `isVisibleUnless(…)` answer whether something shown on, or hidden on, some families applies here.

Its members are upper case (`IOS`, `ANDROID`, `WEB`…), an exception to this repository's PascalCase rule. They are kept that way until the apps that use them move with a rename. Compare members; do not store or send their names.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-platform`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-platform/SKILL.md](src/commonMain/skills/com-oddlyclever-platform/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-platform/`.

## Tests

The initializer is the manifest merge and App Startup at work, which only a device has:

```bash
./gradlew :platform:connectedAndroidDeviceTest
```
