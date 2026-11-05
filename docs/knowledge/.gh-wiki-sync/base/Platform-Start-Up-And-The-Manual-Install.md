# Start-Up and the Manual Install

DOC-0021 · 2026-11-04
Keywords: PlatformInitializer, App Startup, androidx.startup.InitializationProvider, initializer did not run, The application context was never captured, IllegalStateException AppContext, The application context is not an Application, removed InitializationProvider from merged manifest, tools:node remove, tools:node merge, Application.onCreate order, content provider before onCreate, manual install, AppContext.install, activity not seen, ActivityProvider reports null, JVM unit test Context, Robolectric, connectedAndroidDeviceTest, init(context) does not exist

`AppContext` and `ActivityProvider` are filled by `PlatformInitializer`, an App Startup initializer that arrives in the library's manifest. An app normally does nothing. This page is how that works, what to do when an app removes it, and what each failure looks like.

## How it runs

The library's manifest adds `PlatformInitializer` as a `<meta-data>` entry on App Startup's `androidx.startup.InitializationProvider`. It does not declare the provider itself: other libraries contribute their own copy of it, so the entry uses `tools:node="merge"` and joins whichever copy wins the manifest merge instead of colliding with it.

App Startup runs its initializers from that content provider, and content providers are created before `Application.onCreate`. So:

- the context is captured before any application code runs, which is what makes `AppContext.get()` safe in a constructor;
- the activity provider registers its lifecycle callbacks before the first activity is created, so it sees that activity.

`PlatformInitializer` names no dependencies. It has to be able to run first, and naming another initializer would tie their orders together. It returns the application context, so another library's App Startup initializer can list `PlatformInitializer` among its own dependencies and receive the context, which is the contract App Startup expects.

There is no `init(context)` to call from the application in the ordinary case.

## When App Startup's provider is removed

Some apps strip App Startup's provider from their merged manifest to control when initializers run. That removes this initializer too, and so does removing just this initializer's `<meta-data>` entry. Then install by hand, first thing:

```kotlin
class AcmeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PlatformInitializer.install(this)
    }
}
```

- **Call it before constructing anything that reads `AppContext`.** Until then, `AppContext.get()` throws.
- **Activities created before the call are not seen until they next resume.** Calling it first in `Application.onCreate` means none has been created yet.
- **It is harmless when the provider did run.** The context is captured again, which changes nothing, and the activity provider that already exists is kept.
- **Use `PlatformInitializer.install`, not `AppContext.install`.** `AppContext.install` is public but fills only the context. The activity provider is then started on the first `ActivityProvider.get()`, and reports `null` until an activity is next created or resumed, so an activity already on screen goes unseen.

## When it fails

| What you see | Why |
|---|---|
| `IllegalStateException`: "The application context was never captured." from `AppContext.get()` or `ActivityProvider.get()` | The initializer never ran. The app removed App Startup's provider and did not call `PlatformInitializer.install`, or no app process started the provider at all, as in a plain JVM unit test. |
| `IllegalStateException`: "The application context is not an Application, so activities cannot be tracked." | The context that was installed has an application context that is not an `android.app.Application`, as some test contexts do. |
| `ActivityProvider.get().current.value` is `null` although an activity is on screen | The provider was started after that activity was created: by a late `PlatformInitializer.install`, or by `ActivityProvider.get()` after only `AppContext.install`. It catches up when an activity is next created or resumed. |
| `current` names an activity that is no longer on screen | It is the last `ComponentActivity` created or resumed and not yet destroyed. See [The Application Context and the Activity in Front](Platform-The-Application-Context-And-The-Activity-In-Front). |

In a JVM unit test, where nothing starts the provider, have the test install a context itself or pass one to the class under test. That is the case the default-parameter pattern is meant for: the class takes `context: Context = AppContext.get()`, and the test passes its own.

## Testing it

The initializer is the manifest merge and App Startup's provider at work, and neither exists in a JVM test, so the module's Android tests run on a device or an emulator:

```bash
./gradlew :platform:connectedAndroidDeviceTest
```

`PlatformInitializerTest` pins four things: the context is captured before any test code runs, with nothing installed by the test; App Startup reports the initializer as eagerly initialized, meaning it was reached through the manifest rather than on demand; installing by hand after the provider ran keeps the same context and the same activity provider; and the provider follows a launched `ComponentActivity`, then reports `null` once it is destroyed. The device-test manifest declares a `ComponentActivity` for that last test to launch.

`OsFamily`'s checks are tested in `commonTest` and run on every target with `./gradlew :platform:check`.
