package com.oddlyclever.sundries

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pins raw text reading: the reader the app passes is asked for exactly the path given, its bytes come back as UTF-8
 * text, accents and all, and a reader that cannot find the file fails the call rather than giving empty text.
 */
class RawResourcesTest {

    private val files = mapOf("files/legal/terms.md" to "# Conditions générales\n\nÇa va.".encodeToByteArray())

    @Test
    fun theAppsReaderIsAskedForThePathAndItsBytesAreText() = runTest {
        val asked = mutableListOf<String>()
        val text = rawStringResource("files/legal/terms.md") { path ->
            asked += path
            files.getValue(path)
        }
        assertEquals(listOf("files/legal/terms.md"), asked)
        assertEquals("# Conditions générales\n\nÇa va.", text)
    }

    @Test
    fun aMissingFileFailsTheCall() = runTest {
        assertFailsWith<NoSuchElementException> { rawStringResource("files/missing.md") { files.getValue(it) } }
    }
}
