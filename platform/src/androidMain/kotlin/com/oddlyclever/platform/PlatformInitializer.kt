package com.oddlyclever.platform

import android.content.Context
import androidx.startup.Initializer

/**
 * Captures the application context into [AppContext] and starts the [ActivityProvider] at process start, so nothing
 * else has to ask for either.
 *
 * Declared in this library's manifest and merged into the app, so an app does nothing to enable it. App Startup runs
 * it from a content provider, before `Application.onCreate`, so no application code can run and find either missing,
 * and the activity provider sees the first activity created.
 *
 * Returns the context so that another library's initializer can depend on this one and receive it, which is the
 * contract App Startup expects.
 */
class PlatformInitializer : Initializer<Context> {

    override fun create(context: Context): Context {
        install(context)
        return context.applicationContext
    }

    /** None: this has to be able to run first, and naming another initializer would tie their orders together. */
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    companion object {

        /**
         * The manual path, for an app that removes App Startup's provider from its merged manifest to control when
         * initializers run; that removes this one too. Call it first thing in `Application.onCreate`, before
         * constructing anything that reads [AppContext]. Harmless when the provider did run.
         */
        @JvmStatic
        fun install(context: Context) {
            AppContext.install(context)
            ActivityProvider.install(context)
        }
    }
}
