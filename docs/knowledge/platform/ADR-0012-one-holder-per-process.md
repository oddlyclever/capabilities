# One Holder per Process

ADR-0012 · 2026-11-04 · Status: accepted
Keywords: why a platform module, static context holder in each library, two context holders, empty static, Context threaded through constructors, Context parameter spreading up the call graph, App Startup initializer, content provider before Application.onCreate, why ActivityProvider cannot be constructed, private constructor, duplicate lifecycle callbacks, what belongs in platform, initializer dependencies

## Context

Code deep inside an Android app or library keeps needing a `Context`, or the activity on screen to ask for a permission or show a system sheet. Passing them in spreads a `Context` parameter up the call graph: a parameter on one constructor becomes a parameter on whatever builds it, and on whatever builds that. The common way out is a static holder filled at start-up, and when every library keeps its own, a process ends up with two initializers filling two statics. Nothing errors. Whichever one a module reads next may be the empty one, and that module fails much later, somewhere unrelated. Two activity trackers go wrong the same way.

## Decision

The application context and the activity in front are held once per process, in this module, and every library that needs either depends on it rather than keeping its own.

- **One holder, one initializer.** `AppContext` holds the application context statically, which is safe only because it is the application context. `PlatformInitializer` fills it.
- **Filled by App Startup, from the library's manifest.** App Startup runs from a content provider, and providers are created before `Application.onCreate`, so the context is there before any application code runs and is safe to read in a constructor. The app does nothing to enable it.
- **Read as a default parameter.** A class takes `context: Context = AppContext.get()`, so its callers pass nothing and a test or a container can still pass one.
- **The activity provider cannot be constructed.** There is one, started with the context. One made later would miss the activity already running and report none until the next resume, and two would each register their own lifecycle callbacks.
- **The initializer names no dependencies,** because it has to be able to run first, and naming another initializer would tie their orders together.
- **Only what has to exist exactly once in a process belongs in this module.** Anything else is a library of its own.

## Consequences

- A library gets a context or the activity in front with no constructor parameter and no setup in the app.
- There is one place for both to be missing, and one error message that says how they were meant to arrive.
- An app that strips App Startup's provider has to call `PlatformInitializer.install` itself, first thing in `Application.onCreate`. [Start-Up and the Manual Install](DOC-0021-start-up-and-the-manual-install.md) covers it.
- A plain JVM unit test has no provider, so a class under test needs its context passed in.
- The module's scope stays small by rule, so it is not a place for general platform utilities.
