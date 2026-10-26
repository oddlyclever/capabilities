package com.oddlyclever.platform

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The activity in front, for the calls that need one: asking for a permission, starting an activity for a result,
 * showing a system sheet.
 *
 * There is one per process, reached with [get], and [PlatformInitializer] starts it at process start, before any
 * activity is created. That is the reason it cannot be constructed: one made later would miss the activity already
 * running and report none until the next resume, and two would each register their own lifecycle callbacks.
 */
class ActivityProvider private constructor(application: Application) {

    private val _current = MutableStateFlow<ComponentActivity?>(null)

    /** The activity created or resumed most recently and not yet destroyed, or null when there is none. */
    val current: StateFlow<ComponentActivity?> = _current.asStateFlow()

    init {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is ComponentActivity) _current.value = activity
            }

            override fun onActivityResumed(activity: Activity) {
                if (activity is ComponentActivity) _current.value = activity
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (_current.value === activity) _current.value = null
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        })
    }

    companion object {

        @Volatile
        private var instance: ActivityProvider? = null

        /**
         * The process's activity provider. Started by [PlatformInitializer]; if that never ran, it is started here
         * from [AppContext], and reports no activity until one is next created or resumed.
         */
        fun get(): ActivityProvider = instance ?: install(AppContext.get())

        /** Starts the provider for [context]'s application, once; later calls return the one already started. */
        internal fun install(context: Context): ActivityProvider = instance ?: synchronized(this) {
            instance ?: run {
                val application = context.applicationContext as? Application
                    ?: error("The application context is not an Application, so activities cannot be tracked.")
                ActivityProvider(application).also { instance = it }
            }
        }
    }
}
