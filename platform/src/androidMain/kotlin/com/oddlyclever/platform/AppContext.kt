package com.oddlyclever.platform

import android.content.Context

/**
 * The application context, captured once at process start, so no other module has to take a `Context` it does not
 * otherwise need.
 *
 * Held statically, which is safe because it is the *application* context: it lives as long as the process and leaks
 * nothing, unlike an activity or a service. Anything else held this way would be a bug.
 *
 * Filled by [PlatformInitializer] through App Startup, which runs from a content provider, and content providers are
 * created before `Application.onCreate`. So it is filled before any application code runs, which is what makes it
 * safe for a class to read in its constructor:
 *
 * ```kotlin
 * class SomeCapability(private val context: Context = AppContext.get())
 * ```
 *
 * There is one per process, deliberately. A second static holder in another library would mean two initializers
 * filling two statics, and whichever a module reads next may be the empty one, with nothing erroring until much later.
 */
object AppContext {

    @Volatile
    private var captured: Context? = null

    /** Captures [context]'s application context. Done by [PlatformInitializer]; harmless to repeat. */
    fun install(context: Context) {
        captured = context.applicationContext
    }

    /**
     * The captured context, or an error saying how it was supposed to arrive. For anything that cannot continue
     * without one: the message is more use than a null pointer three frames later.
     */
    fun get(): Context = captured ?: error(
        "The application context was never captured. PlatformInitializer runs from " +
            "androidx.startup.InitializationProvider; if the app removes that provider from its merged manifest, " +
            "call PlatformInitializer.install(context) from Application.onCreate instead."
    )

    /** The captured context, or null, for a caller that can do without one rather than fail. */
    fun getOrNull(): Context? = captured
}
