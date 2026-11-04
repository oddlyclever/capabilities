# The Application Context and the Activity in Front

DOC-0020 · 2026-11-04
Keywords: get Context anywhere Android, static context holder, Context parameter in every constructor, AppContext.get, AppContext.getOrNull, AppContext.context does not exist, current activity, top activity, ActivityProvider, ActivityLifecycleCallbacks, StateFlow of activity, permission request needs an activity, activity result, system sheet, currentActivity, getActivity, ComponentActivity, activity leak, rotation, Koin binding, Unresolved reference AppContext in commonMain

On Android, `platform` holds two things that exist once per process: the application context, in `AppContext`, and the activity in front, in `ActivityProvider`. Both are filled at process start, before any application code runs. This page covers how each is read and what each one reports. How they get filled, and what happens when they don't, is in [Start-Up and the Manual Install](DOC-0021-start-up-and-the-manual-install.md).

## Which targets get what

`AppContext`, `ActivityProvider` and `PlatformInitializer` are in the Android source set only. The JVM, iOS, JS and wasmJs targets have only [`OsFamily`](DOC-0022-osfamily.md). Referring to the Android types from `commonMain`, or from a desktop JVM target, fails with "Unresolved reference". Use them from `androidMain`, behind an `expect`/`actual` of your own where common code needs the result.

## The application context

```kotlin
class AcmeUploader(private val context: Context = AppContext.get())
```

The default parameter is the point. A caller writes `AcmeUploader()` and passes nothing, while a test or a dependency-injection container can still hand the class a context. Without it, a `Context` parameter on one constructor becomes a `Context` parameter on whatever builds that class, and on whatever builds that.

- **`AppContext.get()`** returns the context, or throws an `IllegalStateException` when it was never captured. The message begins "The application context was never captured." and says how the context was meant to arrive, which is more use than a null pointer three frames later.
- **`AppContext.getOrNull()`** returns the context or `null`, for a caller that can carry on without one.
- **It is a call, not a property.** There is no `AppContext.context` or `appContext`.
- **Reading it in a constructor or a property initializer is safe.** The initializer runs from a content provider, and content providers are created before `Application.onCreate`, so no application code runs before it.
- **Holding it statically is safe** because it is the application context: it lives as long as the process and leaks nothing. An activity or a service held the same way would leak.

## The activity in front

```kotlin
fun openSettings(intent: Intent) {
    val activity = ActivityProvider.get().current.value ?: return
    activity.startActivity(intent)
}
```

`ActivityProvider.get().current` is a `StateFlow<ComponentActivity?>`, for the calls that need an activity: asking for a permission, starting an activity for a result, showing a system sheet. Read `current.value` at the moment of use, or collect `current` to react when it changes.

What it reports follows from the lifecycle callbacks it registers:

- **It is the activity created or resumed most recently and not yet destroyed,** not necessarily the one on screen. It stays set while the app is in the background, until that activity is destroyed.
- **Only a `ComponentActivity` is followed.** With a plain `android.app.Activity` in front, `current` still names the `ComponentActivity` behind it.
- **It is `null` for a moment while an activity is recreated,** as on rotation: the old one is destroyed before the new one is created.
- **Don't keep the activity in a field.** It leaks across recreation. Read `current.value` again each time.

There is exactly one provider per process, reached with `ActivityProvider.get()`. Its constructor is private, so `ActivityProvider(application)` does not compile. Why is in [One Holder per Process](ADR-0012-one-holder-per-process.md).

It is not `currentActivity` or a `getActivity()` returning `Activity`: it is `ActivityProvider.get().current.value`, typed `ComponentActivity`.

## In a dependency-injection container

Nothing here needs a container, and no Koin module ships; Koin is not a dependency. To put the provider in one, bind the single instance rather than constructing anything:

```kotlin
single { ActivityProvider.get() }
```

The context needs no binding of its own. A class that takes `context: Context = AppContext.get()` can be given one by the container, or left to its default.

## What the module brings to an Android build

The Android target exposes App Startup (`androidx.startup:startup-runtime`), `androidx.activity:activity` and `kotlinx-coroutines-core` as `api` dependencies, because `PlatformInitializer`'s supertype is App Startup's and `ActivityProvider` exposes a `StateFlow` of `ComponentActivity`. An app that depends on the module gets all three. The versions are in [`gradle/libs.versions.toml`](../../../gradle/libs.versions.toml).

Its manifest adds no activity, service or receiver, and exports nothing. The only thing it adds to the app is the initializer.
