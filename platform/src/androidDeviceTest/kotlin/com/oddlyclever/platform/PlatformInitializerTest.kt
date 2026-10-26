package com.oddlyclever.platform

import androidx.activity.ComponentActivity
import androidx.startup.AppInitializer
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pins that the context and the activity provider arrive on their own, from the merged manifest, before any test
 * code runs, and that the provider follows a real activity. Only testable on a device: the manifest merge and App
 * Startup's provider do the work, and neither exists in a JVM test.
 */
@RunWith(AndroidJUnit4::class)
class PlatformInitializerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theContextIsCapturedBeforeAnyTestRuns() {
        // Nothing here installed anything; had the initializer not run from the merged manifest, this would throw.
        assertNotNull(AppContext.get())
        assertSame(AppContext.get(), AppContext.getOrNull())
    }

    @Test
    fun theInitializerRunsEagerlyFromTheManifest() {
        assertTrue(
            "the initializer must be reached through the manifest, not on demand",
            AppInitializer.getInstance(context).isEagerlyInitialized(PlatformInitializer::class.java),
        )
    }

    @Test
    fun installingByHandIsHarmlessWhenTheProviderAlreadyRan() {
        val context = AppContext.get()
        val provider = ActivityProvider.get()
        PlatformInitializer.install(this.context)
        assertSame("the context", context, AppContext.get())
        assertSame("the activity provider", provider, ActivityProvider.get())
    }

    @Test
    fun theActivityProviderFollowsTheActivityInFront() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity -> assertSame("the launched activity", activity, ActivityProvider.get().current.value) }
        scenario.close()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertNull("no activity once it is destroyed", ActivityProvider.get().current.value)
    }
}
