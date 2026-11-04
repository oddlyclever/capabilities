---
name: com-oddlyclever-platform
description: >-
  Get the Android application Context and the activity currently in front from
  anywhere, without passing a Context through every constructor, in Kotlin
  Multiplatform. Use instead of writing a static context holder, an Application
  subclass that stores itself, or ActivityLifecycleCallbacks to track the top
  activity: a Context as a class's default parameter or inside a library that
  takes none, the current activity for a runtime permission request, an
  activity result or a system sheet, and that activity as a StateFlow to
  observe. Both are captured automatically at app start-up by an App Startup
  initializer that runs before Application.onCreate, with a manual install for
  apps that remove App Startup's provider. Also OsFamily, an enum of operating
  system families (Android, iOS, macOS, web, Wasm, Linux, Windows, tvOS,
  watchOS) with checks for whether a control is shown or hidden on some of
  them. Everything except OsFamily is Android only.
license: Apache-2.0
metadata:
  version: "0.0.7"
  repository: https://github.com/oddlyclever/capabilities
---

# platform

`com.oddlyclever:platform`, package `com.oddlyclever.platform`. Called from Kotlin only: there is no Swift or JavaScript surface.

## What it solves

On Android, code deep inside an app or a library keeps needing a `Context`, or the activity on screen to ask for a permission or show a system sheet, and threading them through every constructor spreads a `Context` parameter up the whole call graph. This module captures the application context and follows the activity in front once, at process start, so any class can take them as a default and its callers pass nothing. There is one holder per process on purpose: two libraries each keeping their own static context means two initializers and two statics, and whichever is read next may be the empty one.

What each target gets differs. Android has `AppContext`, `ActivityProvider`, `PlatformInitializer` and `OsFamily`. The JVM, iOS, JS and wasmJs targets have only `OsFamily` and its two checks, `isVisibleIf` and `isVisibleUnless`.

## How it is meant to be used

Take the context as a default parameter, in Android code:

```kotlin
import android.content.Context
import com.oddlyclever.platform.AppContext

class AcmeUploader(private val context: Context = AppContext.get())
```

A caller writes `AcmeUploader()`, and a test or a container can still pass a context. Reading `AppContext.get()` in a constructor or a property initializer is safe: App Startup runs the initializer from a content provider, and content providers are created before `Application.onCreate`, so no application code runs before it. Nothing needs to be called and nothing needs registering; the initializer arrives in the library's manifest.

Read the activity in front at the moment it is needed:

```kotlin
import android.content.Intent
import com.oddlyclever.platform.ActivityProvider

fun openSettings(intent: Intent) {
    val activity = ActivityProvider.get().current.value ?: return // a ComponentActivity, or null when there is none
    activity.startActivity(intent)
}
```

`current` is a `StateFlow<ComponentActivity?>`; collect it to react when the activity changes. In a dependency-injection container, bind the one instance, `single { ActivityProvider.get() }`. No Koin module ships, and Koin is not a dependency.

`OsFamily` is compared, never computed:

```kotlin
import com.oddlyclever.platform.OsFamily
import com.oddlyclever.platform.isVisibleIf
import com.oddlyclever.platform.isVisibleUnless

fun showsShareButton(family: OsFamily) = family.isVisibleUnless(OsFamily.WEB, OsFamily.WASM)
fun usesAppleIcons(family: OsFamily) = family.isVisibleIf(OsFamily.IOS, OsFamily.MACOSX)
```

If the app strips App Startup's `InitializationProvider` from its merged manifest, install by hand, first thing:

```kotlin
import android.app.Application
import com.oddlyclever.platform.PlatformInitializer

class AcmeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PlatformInitializer.install(this)
    }
}
```

It is harmless to call when the provider did run.

## Invariants and traps

**None of the Android API exists in common code.** `AppContext`, `ActivityProvider` and `PlatformInitializer` live in the Android source set only, so referring to them from `commonMain`, or from a desktop JVM target, fails with "Unresolved reference". Use them from `androidMain`, behind an `expect`/`actual` of your own where common code needs the result.

**There is no current `OsFamily`.** The module does not detect the platform: `OsFamily.current`, `currentOsFamily()` and `getPlatform()` do not exist. The app decides which family it is, typically with its own `expect val`, and passes it in.

**`OsFamily` members are exact, and overlapping ones are not related.** `isVisibleIf` and `isVisibleUnless` test whether this member is among those listed, and nothing more: `OsFamily.IOS.isVisibleIf(OsFamily.APPLE)` is `false`, and `WEB` and `WASM` are as separate as `IOS` and `ANDROID`. List every member that applies. With no arguments, `isVisibleIf()` is `false` and `isVisibleUnless()` is `true`.

**Members are upper case, and their names are not stable.** It is `OsFamily.IOS` and `OsFamily.ANDROID`; `OsFamily.Ios` does not compile. They are upper case against this repository's naming rule, kept only until the apps that use them move with a rename, so never store or send `name` or `ordinal`; compare members, and map to your own stored values.

**`AppContext.get()` throws when the initializer never ran.** The `IllegalStateException` begins "The application context was never captured." It happens when the app removes App Startup's provider and does not call `PlatformInitializer.install`, and wherever no app process starts the provider, such as a plain JVM unit test, where a test has to install a context itself or pass one. `AppContext.getOrNull()` returns `null` instead, for a caller that can do without one. `ActivityProvider.get()` throws the same error in the same situations.

**Install with `PlatformInitializer.install`, not `AppContext.install`.** `AppContext.install` is public but fills only the context. The activity provider is then started on the first `ActivityProvider.get()`, and reports `null` until an activity is next created or resumed, so an activity already on screen goes unseen. An install made after an activity exists has the same gap; that is why it goes first in `Application.onCreate`.

**`current` is the last activity created or resumed, not the visible one.** It stays set while the app is in the background, until that activity is destroyed. It is `null` for a moment while an activity is recreated, as on rotation, because the old one is destroyed before the new one is created. Only a `ComponentActivity` is followed: with a plain `android.app.Activity` in front, `current` still names the `ComponentActivity` behind it. Read `current.value` at the moment of use and do not keep the activity in a field, which leaks it across recreation.

**`ActivityProvider` cannot be constructed.** Its constructor is private; `ActivityProvider(application)` does not compile. One made later would miss the activity already running and two would each register lifecycle callbacks, so there is exactly one, from `ActivityProvider.get()`.

## What moved, and what it used to be called

`platform` was first released in 0.0.3, and its API has not changed since.

The shapes an agent will guess instead: the context is a call, `AppContext.get()`, not a property such as `AppContext.context` or `appContext`; the activity is `ActivityProvider.get().current.value`, not `currentActivity` or a `getActivity()` that returns `Activity`, and it is typed `ComponentActivity`. There is no `Platform` interface or `getPlatform()` function, as a new multiplatform project template generates, and no `init(context)` call to make from the application in the ordinary case.

## What it is not for

Only what must exist exactly once in a process belongs here. It does not detect the operating system, read device or OS versions, track the process lifecycle or foreground state, or provide a context or activity on any target but Android. It is not a dependency-injection container.
