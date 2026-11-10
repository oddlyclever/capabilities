package com.oddlyclever.wayfinder

import io.github.aughtone.types.locale.localeFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * Pins how the ornaments' words are chosen: wayfinder's own for the device's language, English for a language it has no
 * words in, and an app's [MapOrnamentLabels] over them field by field, leaving the rest alone.
 */
class MapOrnamentLabelsTest {

    @Test
    fun `a language is chosen by its code whatever region follows it`() {
        assertEquals("Zoom avant", ornamentWordsFor("fr").zoomIn, "French")
        assertEquals("Zoom avant", ornamentWordsFor("fr-CA").zoomIn, "French in Canada")
        assertEquals("Vergrößern", ornamentWordsFor("de_AT").zoomIn, "German in Austria")
    }

    @Test
    fun `Chinese is traditional where it is written that way`() {
        assertEquals("縮小", ornamentWordsFor("zh", "TW").zoomOut, "Taiwan")
        assertEquals("縮小", ornamentWordsFor("zh", "HK").zoomOut, "Hong Kong")
        assertEquals("缩小", ornamentWordsFor("zh", "CN").zoomOut, "mainland China")
        assertEquals("缩小", ornamentWordsFor("zh").zoomOut, "no region")
    }

    @Test
    fun `every language wayfinder translates has words and Canadian Indigenous languages are English`() {
        // The 53 languages wayfinder has words for, listed here so a dropped entry fails.
        val languages = listOf(
            "af", "nl", "de", "da", "nb", "nn", "sv", "is", "fr", "es", "it", "pt", "ca", "gl", "ro", "ru", "uk", "be",
            "pl", "cs", "sk", "bg", "hr", "sr", "mk", "sl", "lt", "lv", "ja", "zh", "ko", "th", "vi", "id", "ms", "hi",
            "hy", "ka", "kk", "uz", "ar", "he", "fa", "tr", "az", "sw", "fi", "et", "hu", "el", "eu", "sq",
        )
        for (language in languages) {
            assertNotEquals(EnglishOrnamentWords, ornamentWordsFor(language), "words for \"$language\"")
        }
        assertEquals("Zoom avant", ornamentWordsFor(requireNotNull(localeFor("fr-CH"))).zoomIn, "a Locale from aughtone-types")
        assertEquals(EnglishOrnamentWords, ornamentWordsFor("iu", "CA"), "Inuktitut")
        assertEquals(EnglishOrnamentWords, ornamentWordsFor("cr", "CA"), "Cree, until a translation checked by speakers is added")
    }

    @Test
    fun `a language wayfinder has no words in gets English`() {
        assertEquals(EnglishOrnamentWords, ornamentWordsFor("yo"), "Yoruba falls back to English")
        assertEquals(EnglishOrnamentWords, ornamentWordsFor(""), "no language at all falls back to English")
    }

    @Test
    fun `every language has eight compass points`() {
        for ((language, words) in OrnamentTranslations) {
            assertEquals(8, words.compassPoints.size, "compass points in \"$language\": ${words.compassPoints}")
        }
    }

    @Test
    fun `an app's label replaces only the word it sets`() {
        val words = ornamentWordsFor("fr").overriddenBy(MapOrnamentLabels(credits = "Sources"))
        assertEquals("Sources", words.credits, "the app's own word")
        assertEquals("Zoom avant", words.zoomIn, "wayfinder's French for the rest")
    }

    @Test
    fun `no labels leave wayfinder's words as they are`() {
        assertEquals(ornamentWordsFor("es"), ornamentWordsFor("es").overriddenBy(null))
        assertEquals(ornamentWordsFor("es"), ornamentWordsFor("es").overriddenBy(MapOrnamentLabels()))
    }

    @Test
    fun `compass points other than eight are refused`() {
        assertFailsWith<IllegalArgumentException> { MapOrnamentLabels(compassPoints = listOf("N", "E", "S", "W")) }
    }
}
