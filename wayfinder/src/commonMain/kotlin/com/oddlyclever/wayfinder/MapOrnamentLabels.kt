package com.oddlyclever.wayfinder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import io.github.aughtone.types.locale.Locale

/**
 * Words of the app's own for the map's ornaments, in place of wayfinder's: what a screen reader says for each control,
 * and the compass tape's points. Set it on [MapOrnaments.labels], or pass it to [MapCompassTape] and
 * [OrientationToggle] where a screen places those itself.
 *
 * Every field is null unless set, and a null field keeps wayfinder's own word in the device's language, as aughtone's
 * `Locale.current` reports it. wayfinder has its words in 53 languages and uses English for any language it does not
 * have. Inuktitut and the other Canadian Indigenous languages aughtone-types knows, such as Cree and Ojibwe, are English
 * for now, until translations checked by speakers are added.
 * Set a field to use the app's own term for a control, or to cover a language wayfinder does not. The scale
 * bar's distance is not here: it is written in the device's locale on its own.
 *
 * @property zoomIn and [zoomOut] are the zoom buttons.
 * @property north is the rosette compass, a button that turns the map back to north.
 * @property compass is the compass tape.
 * @property credits is the "i" that the collapsed credits shrink to.
 * @property northUp is the orientation toggle in north-up, saying what a tap does; [courseUp] is the same in course-up.
 * @property compassPoints are the compass tape's eight points, starting at north and going clockwise.
 */
@Immutable
data class MapOrnamentLabels(
    val zoomIn: String? = null,
    val zoomOut: String? = null,
    val north: String? = null,
    val compass: String? = null,
    val credits: String? = null,
    val northUp: String? = null,
    val courseUp: String? = null,
    val compassPoints: List<String>? = null,
) {
    init {
        require(compassPoints == null || compassPoints.size == 8) {
            "compassPoints needs the eight points from north clockwise, got ${compassPoints?.size}"
        }
    }
}

/** Every word the ornaments use, resolved: wayfinder's own for the language, with the app's [MapOrnamentLabels] over them. */
@Immutable
internal data class OrnamentWords(
    val zoomIn: String,
    val zoomOut: String,
    val north: String,
    val compass: String,
    val credits: String,
    val northUp: String,
    val courseUp: String,
    val compassPoints: List<String>,
)

/** wayfinder's own words for the device's language, with whatever these labels set over them. */
@Composable
internal fun MapOrnamentLabels?.words(): OrnamentWords = ornamentWords().overriddenBy(this)

/** wayfinder's own words in the device's language. */
@Composable
internal fun ornamentWords(): OrnamentWords = remember { ornamentWordsFor(Locale.current) }

internal fun OrnamentWords.overriddenBy(labels: MapOrnamentLabels?): OrnamentWords = if (labels == null) this else OrnamentWords(
    zoomIn = labels.zoomIn ?: zoomIn,
    zoomOut = labels.zoomOut ?: zoomOut,
    north = labels.north ?: north,
    compass = labels.compass ?: compass,
    credits = labels.credits ?: credits,
    northUp = labels.northUp ?: northUp,
    courseUp = labels.courseUp ?: courseUp,
    compassPoints = labels.compassPoints ?: compassPoints,
)

/** wayfinder's own words for [locale]; English for a language it has no words in. */
internal fun ornamentWordsFor(locale: Locale): OrnamentWords = ornamentWordsFor(locale.languageCode, locale.regionCode)

/**
 * wayfinder's own words for [language], an ISO 639 code such as "fr", in [region]. The region matters only where it
 * changes the writing: Chinese in Taiwan, Hong Kong and Macau is traditional, and simplified everywhere else.
 */
internal fun ornamentWordsFor(language: String, region: String? = null): OrnamentWords {
    val code = language.substringBefore('-').substringBefore('_').lowercase()
    val written = if (code == "zh" && region?.uppercase() in TraditionalChineseRegions) "zh-TW" else code
    return OrnamentTranslations[written] ?: EnglishOrnamentWords
}

private val TraditionalChineseRegions = setOf("TW", "HK", "MO")
