package com.oddlyclever.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Pins what the visibility helpers answer for a family listed and one not listed. */
class OsFamilyTest {

    @Test
    fun visibleIfListed() {
        assertTrue(OsFamily.IOS.isVisibleIf(OsFamily.IOS, OsFamily.MACOSX), "iOS among iOS and macOS")
        assertFalse(OsFamily.ANDROID.isVisibleIf(OsFamily.IOS), "Android among iOS only")
    }

    @Test
    fun visibleUnlessListed() {
        assertFalse(OsFamily.WEB.isVisibleUnless(OsFamily.WEB), "web unless web")
        assertTrue(OsFamily.LINUX.isVisibleUnless(OsFamily.WEB, OsFamily.WASM), "Linux unless web or wasm")
    }
}
