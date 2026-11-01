/*
 * GENERATED FILE — DO NOT EDIT BY HAND.
 *
 * Generated from the canonical JSONLogic shared conformance suite:
 *   https://jsonlogic.com/tests.json (retrieved 2026-07-23; 278 cases)
 * A pristine copy of the suite is vendored at src/commonTest/resources/tests.json;
 * regenerate this file from it if the suite is ever updated.
 *
 * Each [rule, data, expected] triplet in the suite becomes one test function;
 * the suite's string entries are section markers, mirrored here as comments and
 * function-name prefixes. Passing this suite is the definition of spec-correct.
 */
package com.oddlyclever.jsonlogic

import kotlin.test.Test

class JsonLogicConformanceTest {

    // # Non-rules get passed through
    @Test
    fun nonRulesGetPassedThrough_001() = checkConformance(
        rule = true,
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun nonRulesGetPassedThrough_002() = checkConformance(
        rule = false,
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun nonRulesGetPassedThrough_003() = checkConformance(
        rule = 17,
        data = emptyMap<String, Any?>(),
        expected = 17,
    )

    @Test
    fun nonRulesGetPassedThrough_004() = checkConformance(
        rule = 3.14,
        data = emptyMap<String, Any?>(),
        expected = 3.14,
    )

    @Test
    fun nonRulesGetPassedThrough_005() = checkConformance(
        rule = "apple",
        data = emptyMap<String, Any?>(),
        expected = "apple",
    )

    @Test
    fun nonRulesGetPassedThrough_006() = checkConformance(
        rule = null,
        data = emptyMap<String, Any?>(),
        expected = null,
    )

    @Test
    fun nonRulesGetPassedThrough_007() = checkConformance(
        rule = listOf<Any?>("a", "b"),
        data = emptyMap<String, Any?>(),
        expected = listOf<Any?>("a", "b"),
    )

    // # Single operator tests
    @Test
    fun singleOperatorTests_008() = checkConformance(
        rule = mapOf<String, Any?>("==" to listOf<Any?>(1, 1)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_009() = checkConformance(
        rule = mapOf<String, Any?>("==" to listOf<Any?>(1, "1")),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_010() = checkConformance(
        rule = mapOf<String, Any?>("==" to listOf<Any?>(1, 2)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_011() = checkConformance(
        rule = mapOf<String, Any?>("===" to listOf<Any?>(1, 1)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_012() = checkConformance(
        rule = mapOf<String, Any?>("===" to listOf<Any?>(1, "1")),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_013() = checkConformance(
        rule = mapOf<String, Any?>("===" to listOf<Any?>(1, 2)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_014() = checkConformance(
        rule = mapOf<String, Any?>("!=" to listOf<Any?>(1, 2)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_015() = checkConformance(
        rule = mapOf<String, Any?>("!=" to listOf<Any?>(1, 1)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_016() = checkConformance(
        rule = mapOf<String, Any?>("!=" to listOf<Any?>(1, "1")),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_017() = checkConformance(
        rule = mapOf<String, Any?>("!==" to listOf<Any?>(1, 2)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_018() = checkConformance(
        rule = mapOf<String, Any?>("!==" to listOf<Any?>(1, 1)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_019() = checkConformance(
        rule = mapOf<String, Any?>("!==" to listOf<Any?>(1, "1")),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_020() = checkConformance(
        rule = mapOf<String, Any?>(">" to listOf<Any?>(2, 1)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_021() = checkConformance(
        rule = mapOf<String, Any?>(">" to listOf<Any?>(1, 1)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_022() = checkConformance(
        rule = mapOf<String, Any?>(">" to listOf<Any?>(1, 2)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_023() = checkConformance(
        rule = mapOf<String, Any?>(">" to listOf<Any?>("2", 1)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_024() = checkConformance(
        rule = mapOf<String, Any?>(">=" to listOf<Any?>(2, 1)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_025() = checkConformance(
        rule = mapOf<String, Any?>(">=" to listOf<Any?>(1, 1)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_026() = checkConformance(
        rule = mapOf<String, Any?>(">=" to listOf<Any?>(1, 2)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_027() = checkConformance(
        rule = mapOf<String, Any?>(">=" to listOf<Any?>("2", 1)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_028() = checkConformance(
        rule = mapOf<String, Any?>("<" to listOf<Any?>(2, 1)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_029() = checkConformance(
        rule = mapOf<String, Any?>("<" to listOf<Any?>(1, 1)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_030() = checkConformance(
        rule = mapOf<String, Any?>("<" to listOf<Any?>(1, 2)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_031() = checkConformance(
        rule = mapOf<String, Any?>("<" to listOf<Any?>("1", 2)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_032() = checkConformance(
        rule = mapOf<String, Any?>("<" to listOf<Any?>(1, 2, 3)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_033() = checkConformance(
        rule = mapOf<String, Any?>("<" to listOf<Any?>(1, 1, 3)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_034() = checkConformance(
        rule = mapOf<String, Any?>("<" to listOf<Any?>(1, 4, 3)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_035() = checkConformance(
        rule = mapOf<String, Any?>("<=" to listOf<Any?>(2, 1)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_036() = checkConformance(
        rule = mapOf<String, Any?>("<=" to listOf<Any?>(1, 1)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_037() = checkConformance(
        rule = mapOf<String, Any?>("<=" to listOf<Any?>(1, 2)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_038() = checkConformance(
        rule = mapOf<String, Any?>("<=" to listOf<Any?>("1", 2)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_039() = checkConformance(
        rule = mapOf<String, Any?>("<=" to listOf<Any?>(1, 2, 3)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_040() = checkConformance(
        rule = mapOf<String, Any?>("<=" to listOf<Any?>(1, 4, 3)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_041() = checkConformance(
        rule = mapOf<String, Any?>("!" to listOf<Any?>(false)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_042() = checkConformance(
        rule = mapOf<String, Any?>("!" to false),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_043() = checkConformance(
        rule = mapOf<String, Any?>("!" to listOf<Any?>(true)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_044() = checkConformance(
        rule = mapOf<String, Any?>("!" to true),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_045() = checkConformance(
        rule = mapOf<String, Any?>("!" to 0),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_046() = checkConformance(
        rule = mapOf<String, Any?>("!" to 1),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_047() = checkConformance(
        rule = mapOf<String, Any?>("or" to listOf<Any?>(true, true)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_048() = checkConformance(
        rule = mapOf<String, Any?>("or" to listOf<Any?>(false, true)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_049() = checkConformance(
        rule = mapOf<String, Any?>("or" to listOf<Any?>(true, false)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_050() = checkConformance(
        rule = mapOf<String, Any?>("or" to listOf<Any?>(false, false)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_051() = checkConformance(
        rule = mapOf<String, Any?>("or" to listOf<Any?>(false, false, true)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_052() = checkConformance(
        rule = mapOf<String, Any?>("or" to listOf<Any?>(false, false, false)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_053() = checkConformance(
        rule = mapOf<String, Any?>("or" to listOf<Any?>(false)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_054() = checkConformance(
        rule = mapOf<String, Any?>("or" to listOf<Any?>(true)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_055() = checkConformance(
        rule = mapOf<String, Any?>("or" to listOf<Any?>(1, 3)),
        data = emptyMap<String, Any?>(),
        expected = 1,
    )

    @Test
    fun singleOperatorTests_056() = checkConformance(
        rule = mapOf<String, Any?>("or" to listOf<Any?>(3, false)),
        data = emptyMap<String, Any?>(),
        expected = 3,
    )

    @Test
    fun singleOperatorTests_057() = checkConformance(
        rule = mapOf<String, Any?>("or" to listOf<Any?>(false, 3)),
        data = emptyMap<String, Any?>(),
        expected = 3,
    )

    @Test
    fun singleOperatorTests_058() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(true, true)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_059() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(false, true)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_060() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(true, false)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_061() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(false, false)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_062() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(true, true, true)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_063() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(true, true, false)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_064() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(false)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_065() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(true)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_066() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(1, 3)),
        data = emptyMap<String, Any?>(),
        expected = 3,
    )

    @Test
    fun singleOperatorTests_067() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(3, false)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_068() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(false, 3)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_069() = checkConformance(
        rule = mapOf<String, Any?>("?:" to listOf<Any?>(true, 1, 2)),
        data = emptyMap<String, Any?>(),
        expected = 1,
    )

    @Test
    fun singleOperatorTests_070() = checkConformance(
        rule = mapOf<String, Any?>("?:" to listOf<Any?>(false, 1, 2)),
        data = emptyMap<String, Any?>(),
        expected = 2,
    )

    @Test
    fun singleOperatorTests_071() = checkConformance(
        rule = mapOf<String, Any?>("in" to listOf<Any?>("Bart", listOf<Any?>("Bart", "Homer", "Lisa", "Marge", "Maggie"))),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_072() = checkConformance(
        rule = mapOf<String, Any?>("in" to listOf<Any?>("Milhouse", listOf<Any?>("Bart", "Homer", "Lisa", "Marge", "Maggie"))),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_073() = checkConformance(
        rule = mapOf<String, Any?>("in" to listOf<Any?>("Spring", "Springfield")),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun singleOperatorTests_074() = checkConformance(
        rule = mapOf<String, Any?>("in" to listOf<Any?>("i", "team")),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun singleOperatorTests_075() = checkConformance(
        rule = mapOf<String, Any?>("cat" to "ice"),
        data = emptyMap<String, Any?>(),
        expected = "ice",
    )

    @Test
    fun singleOperatorTests_076() = checkConformance(
        rule = mapOf<String, Any?>("cat" to listOf<Any?>("ice")),
        data = emptyMap<String, Any?>(),
        expected = "ice",
    )

    @Test
    fun singleOperatorTests_077() = checkConformance(
        rule = mapOf<String, Any?>("cat" to listOf<Any?>("ice", "cream")),
        data = emptyMap<String, Any?>(),
        expected = "icecream",
    )

    @Test
    fun singleOperatorTests_078() = checkConformance(
        rule = mapOf<String, Any?>("cat" to listOf<Any?>(1, 2)),
        data = emptyMap<String, Any?>(),
        expected = "12",
    )

    @Test
    fun singleOperatorTests_079() = checkConformance(
        rule = mapOf<String, Any?>("cat" to listOf<Any?>("Robocop", 2)),
        data = emptyMap<String, Any?>(),
        expected = "Robocop2",
    )

    @Test
    fun singleOperatorTests_080() = checkConformance(
        rule = mapOf<String, Any?>("cat" to listOf<Any?>("we all scream for ", "ice", "cream")),
        data = emptyMap<String, Any?>(),
        expected = "we all scream for icecream",
    )

    @Test
    fun singleOperatorTests_081() = checkConformance(
        rule = mapOf<String, Any?>("%" to listOf<Any?>(1, 2)),
        data = emptyMap<String, Any?>(),
        expected = 1,
    )

    @Test
    fun singleOperatorTests_082() = checkConformance(
        rule = mapOf<String, Any?>("%" to listOf<Any?>(2, 2)),
        data = emptyMap<String, Any?>(),
        expected = 0,
    )

    @Test
    fun singleOperatorTests_083() = checkConformance(
        rule = mapOf<String, Any?>("%" to listOf<Any?>(3, 2)),
        data = emptyMap<String, Any?>(),
        expected = 1,
    )

    @Test
    fun singleOperatorTests_084() = checkConformance(
        rule = mapOf<String, Any?>("max" to listOf<Any?>(1, 2, 3)),
        data = emptyMap<String, Any?>(),
        expected = 3,
    )

    @Test
    fun singleOperatorTests_085() = checkConformance(
        rule = mapOf<String, Any?>("max" to listOf<Any?>(1, 3, 3)),
        data = emptyMap<String, Any?>(),
        expected = 3,
    )

    @Test
    fun singleOperatorTests_086() = checkConformance(
        rule = mapOf<String, Any?>("max" to listOf<Any?>(3, 2, 1)),
        data = emptyMap<String, Any?>(),
        expected = 3,
    )

    @Test
    fun singleOperatorTests_087() = checkConformance(
        rule = mapOf<String, Any?>("max" to listOf<Any?>(1)),
        data = emptyMap<String, Any?>(),
        expected = 1,
    )

    @Test
    fun singleOperatorTests_088() = checkConformance(
        rule = mapOf<String, Any?>("min" to listOf<Any?>(1, 2, 3)),
        data = emptyMap<String, Any?>(),
        expected = 1,
    )

    @Test
    fun singleOperatorTests_089() = checkConformance(
        rule = mapOf<String, Any?>("min" to listOf<Any?>(1, 1, 3)),
        data = emptyMap<String, Any?>(),
        expected = 1,
    )

    @Test
    fun singleOperatorTests_090() = checkConformance(
        rule = mapOf<String, Any?>("min" to listOf<Any?>(3, 2, 1)),
        data = emptyMap<String, Any?>(),
        expected = 1,
    )

    @Test
    fun singleOperatorTests_091() = checkConformance(
        rule = mapOf<String, Any?>("min" to listOf<Any?>(1)),
        data = emptyMap<String, Any?>(),
        expected = 1,
    )

    @Test
    fun singleOperatorTests_092() = checkConformance(
        rule = mapOf<String, Any?>("+" to listOf<Any?>(1, 2)),
        data = emptyMap<String, Any?>(),
        expected = 3,
    )

    @Test
    fun singleOperatorTests_093() = checkConformance(
        rule = mapOf<String, Any?>("+" to listOf<Any?>(2, 2, 2)),
        data = emptyMap<String, Any?>(),
        expected = 6,
    )

    @Test
    fun singleOperatorTests_094() = checkConformance(
        rule = mapOf<String, Any?>("+" to listOf<Any?>(1)),
        data = emptyMap<String, Any?>(),
        expected = 1,
    )

    @Test
    fun singleOperatorTests_095() = checkConformance(
        rule = mapOf<String, Any?>("+" to listOf<Any?>("1", 1)),
        data = emptyMap<String, Any?>(),
        expected = 2,
    )

    @Test
    fun singleOperatorTests_096() = checkConformance(
        rule = mapOf<String, Any?>("*" to listOf<Any?>(3, 2)),
        data = emptyMap<String, Any?>(),
        expected = 6,
    )

    @Test
    fun singleOperatorTests_097() = checkConformance(
        rule = mapOf<String, Any?>("*" to listOf<Any?>(2, 2, 2)),
        data = emptyMap<String, Any?>(),
        expected = 8,
    )

    @Test
    fun singleOperatorTests_098() = checkConformance(
        rule = mapOf<String, Any?>("*" to listOf<Any?>(1)),
        data = emptyMap<String, Any?>(),
        expected = 1,
    )

    @Test
    fun singleOperatorTests_099() = checkConformance(
        rule = mapOf<String, Any?>("*" to listOf<Any?>("1", 1)),
        data = emptyMap<String, Any?>(),
        expected = 1,
    )

    @Test
    fun singleOperatorTests_100() = checkConformance(
        rule = mapOf<String, Any?>("-" to listOf<Any?>(2, 3)),
        data = emptyMap<String, Any?>(),
        expected = -1,
    )

    @Test
    fun singleOperatorTests_101() = checkConformance(
        rule = mapOf<String, Any?>("-" to listOf<Any?>(3, 2)),
        data = emptyMap<String, Any?>(),
        expected = 1,
    )

    @Test
    fun singleOperatorTests_102() = checkConformance(
        rule = mapOf<String, Any?>("-" to listOf<Any?>(3)),
        data = emptyMap<String, Any?>(),
        expected = -3,
    )

    @Test
    fun singleOperatorTests_103() = checkConformance(
        rule = mapOf<String, Any?>("-" to listOf<Any?>("1", 1)),
        data = emptyMap<String, Any?>(),
        expected = 0,
    )

    @Test
    fun singleOperatorTests_104() = checkConformance(
        rule = mapOf<String, Any?>("/" to listOf<Any?>(4, 2)),
        data = emptyMap<String, Any?>(),
        expected = 2,
    )

    @Test
    fun singleOperatorTests_105() = checkConformance(
        rule = mapOf<String, Any?>("/" to listOf<Any?>(2, 4)),
        data = emptyMap<String, Any?>(),
        expected = 0.5,
    )

    @Test
    fun singleOperatorTests_106() = checkConformance(
        rule = mapOf<String, Any?>("/" to listOf<Any?>("1", 1)),
        data = emptyMap<String, Any?>(),
        expected = 1,
    )

    // Substring
    @Test
    fun substring_107() = checkConformance(
        rule = mapOf<String, Any?>("substr" to listOf<Any?>("jsonlogic", 4)),
        data = null,
        expected = "logic",
    )

    @Test
    fun substring_108() = checkConformance(
        rule = mapOf<String, Any?>("substr" to listOf<Any?>("jsonlogic", -5)),
        data = null,
        expected = "logic",
    )

    @Test
    fun substring_109() = checkConformance(
        rule = mapOf<String, Any?>("substr" to listOf<Any?>("jsonlogic", 0, 1)),
        data = null,
        expected = "j",
    )

    @Test
    fun substring_110() = checkConformance(
        rule = mapOf<String, Any?>("substr" to listOf<Any?>("jsonlogic", -1, 1)),
        data = null,
        expected = "c",
    )

    @Test
    fun substring_111() = checkConformance(
        rule = mapOf<String, Any?>("substr" to listOf<Any?>("jsonlogic", 4, 5)),
        data = null,
        expected = "logic",
    )

    @Test
    fun substring_112() = checkConformance(
        rule = mapOf<String, Any?>("substr" to listOf<Any?>("jsonlogic", -5, 5)),
        data = null,
        expected = "logic",
    )

    @Test
    fun substring_113() = checkConformance(
        rule = mapOf<String, Any?>("substr" to listOf<Any?>("jsonlogic", -5, -2)),
        data = null,
        expected = "log",
    )

    @Test
    fun substring_114() = checkConformance(
        rule = mapOf<String, Any?>("substr" to listOf<Any?>("jsonlogic", 1, -5)),
        data = null,
        expected = "son",
    )

    // Merge arrays
    @Test
    fun mergeArrays_115() = checkConformance(
        rule = mapOf<String, Any?>("merge" to emptyList<Any?>()),
        data = null,
        expected = emptyList<Any?>(),
    )

    @Test
    fun mergeArrays_116() = checkConformance(
        rule = mapOf<String, Any?>("merge" to listOf<Any?>(listOf<Any?>(1))),
        data = null,
        expected = listOf<Any?>(1),
    )

    @Test
    fun mergeArrays_117() = checkConformance(
        rule = mapOf<String, Any?>("merge" to listOf<Any?>(listOf<Any?>(1), emptyList<Any?>())),
        data = null,
        expected = listOf<Any?>(1),
    )

    @Test
    fun mergeArrays_118() = checkConformance(
        rule = mapOf<String, Any?>("merge" to listOf<Any?>(listOf<Any?>(1), listOf<Any?>(2))),
        data = null,
        expected = listOf<Any?>(1, 2),
    )

    @Test
    fun mergeArrays_119() = checkConformance(
        rule = mapOf<String, Any?>("merge" to listOf<Any?>(listOf<Any?>(1), listOf<Any?>(2), listOf<Any?>(3))),
        data = null,
        expected = listOf<Any?>(1, 2, 3),
    )

    @Test
    fun mergeArrays_120() = checkConformance(
        rule = mapOf<String, Any?>("merge" to listOf<Any?>(listOf<Any?>(1, 2), listOf<Any?>(3))),
        data = null,
        expected = listOf<Any?>(1, 2, 3),
    )

    @Test
    fun mergeArrays_121() = checkConformance(
        rule = mapOf<String, Any?>("merge" to listOf<Any?>(listOf<Any?>(1), listOf<Any?>(2, 3))),
        data = null,
        expected = listOf<Any?>(1, 2, 3),
    )

    // Given non-array arguments, merge converts them to arrays
    @Test
    fun givenNonArrayArgumentsMergeConvertsThemToArrays_122() = checkConformance(
        rule = mapOf<String, Any?>("merge" to 1),
        data = null,
        expected = listOf<Any?>(1),
    )

    @Test
    fun givenNonArrayArgumentsMergeConvertsThemToArrays_123() = checkConformance(
        rule = mapOf<String, Any?>("merge" to listOf<Any?>(1, 2)),
        data = null,
        expected = listOf<Any?>(1, 2),
    )

    @Test
    fun givenNonArrayArgumentsMergeConvertsThemToArrays_124() = checkConformance(
        rule = mapOf<String, Any?>("merge" to listOf<Any?>(1, listOf<Any?>(2))),
        data = null,
        expected = listOf<Any?>(1, 2),
    )

    // Too few args
    @Test
    fun tooFewArgs_125() = checkConformance(
        rule = mapOf<String, Any?>("if" to emptyList<Any?>()),
        data = null,
        expected = null,
    )

    @Test
    fun tooFewArgs_126() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(true)),
        data = null,
        expected = true,
    )

    @Test
    fun tooFewArgs_127() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(false)),
        data = null,
        expected = false,
    )

    @Test
    fun tooFewArgs_128() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>("apple")),
        data = null,
        expected = "apple",
    )

    // Simple if/then/else cases
    @Test
    fun simpleIfThenElseCases_129() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(true, "apple")),
        data = null,
        expected = "apple",
    )

    @Test
    fun simpleIfThenElseCases_130() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(false, "apple")),
        data = null,
        expected = null,
    )

    @Test
    fun simpleIfThenElseCases_131() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(true, "apple", "banana")),
        data = null,
        expected = "apple",
    )

    @Test
    fun simpleIfThenElseCases_132() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(false, "apple", "banana")),
        data = null,
        expected = "banana",
    )

    // Empty arrays are falsey
    @Test
    fun emptyArraysAreFalsey_133() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(emptyList<Any?>(), "apple", "banana")),
        data = null,
        expected = "banana",
    )

    @Test
    fun emptyArraysAreFalsey_134() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(listOf<Any?>(1), "apple", "banana")),
        data = null,
        expected = "apple",
    )

    @Test
    fun emptyArraysAreFalsey_135() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(listOf<Any?>(1, 2, 3, 4), "apple", "banana")),
        data = null,
        expected = "apple",
    )

    // Empty strings are falsey, all other strings are truthy
    @Test
    fun emptyStringsAreFalseyAllOtherStringsAreTruthy_136() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>("", "apple", "banana")),
        data = null,
        expected = "banana",
    )

    @Test
    fun emptyStringsAreFalseyAllOtherStringsAreTruthy_137() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>("zucchini", "apple", "banana")),
        data = null,
        expected = "apple",
    )

    @Test
    fun emptyStringsAreFalseyAllOtherStringsAreTruthy_138() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>("0", "apple", "banana")),
        data = null,
        expected = "apple",
    )

    // You can cast a string to numeric with a unary + 
    @Test
    fun youCanCastAStringToNumericWithAUnary_139() = checkConformance(
        rule = mapOf<String, Any?>("===" to listOf<Any?>(0, "0")),
        data = null,
        expected = false,
    )

    @Test
    fun youCanCastAStringToNumericWithAUnary_140() = checkConformance(
        rule = mapOf<String, Any?>("===" to listOf<Any?>(0, mapOf<String, Any?>("+" to "0"))),
        data = null,
        expected = true,
    )

    @Test
    fun youCanCastAStringToNumericWithAUnary_141() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(mapOf<String, Any?>("+" to "0"), "apple", "banana")),
        data = null,
        expected = "banana",
    )

    @Test
    fun youCanCastAStringToNumericWithAUnary_142() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(mapOf<String, Any?>("+" to "1"), "apple", "banana")),
        data = null,
        expected = "apple",
    )

    // Zero is falsy, all other numbers are truthy
    @Test
    fun zeroIsFalsyAllOtherNumbersAreTruthy_143() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(0, "apple", "banana")),
        data = null,
        expected = "banana",
    )

    @Test
    fun zeroIsFalsyAllOtherNumbersAreTruthy_144() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(1, "apple", "banana")),
        data = null,
        expected = "apple",
    )

    @Test
    fun zeroIsFalsyAllOtherNumbersAreTruthy_145() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(3.1416, "apple", "banana")),
        data = null,
        expected = "apple",
    )

    @Test
    fun zeroIsFalsyAllOtherNumbersAreTruthy_146() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(-1, "apple", "banana")),
        data = null,
        expected = "apple",
    )

    // Truthy and falsy definitions matter in Boolean operations
    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_147() = checkConformance(
        rule = mapOf<String, Any?>("!" to listOf<Any?>(emptyList<Any?>())),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_148() = checkConformance(
        rule = mapOf<String, Any?>("!!" to listOf<Any?>(emptyList<Any?>())),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_149() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(emptyList<Any?>(), true)),
        data = emptyMap<String, Any?>(),
        expected = emptyList<Any?>(),
    )

    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_150() = checkConformance(
        rule = mapOf<String, Any?>("or" to listOf<Any?>(emptyList<Any?>(), true)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_151() = checkConformance(
        rule = mapOf<String, Any?>("!" to listOf<Any?>(0)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_152() = checkConformance(
        rule = mapOf<String, Any?>("!!" to listOf<Any?>(0)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_153() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(0, true)),
        data = emptyMap<String, Any?>(),
        expected = 0,
    )

    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_154() = checkConformance(
        rule = mapOf<String, Any?>("or" to listOf<Any?>(0, true)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_155() = checkConformance(
        rule = mapOf<String, Any?>("!" to listOf<Any?>("")),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_156() = checkConformance(
        rule = mapOf<String, Any?>("!!" to listOf<Any?>("")),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_157() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>("", true)),
        data = emptyMap<String, Any?>(),
        expected = "",
    )

    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_158() = checkConformance(
        rule = mapOf<String, Any?>("or" to listOf<Any?>("", true)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_159() = checkConformance(
        rule = mapOf<String, Any?>("!" to listOf<Any?>("0")),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_160() = checkConformance(
        rule = mapOf<String, Any?>("!!" to listOf<Any?>("0")),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_161() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>("0", true)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun truthyAndFalsyDefinitionsMatterInBooleanOperations_162() = checkConformance(
        rule = mapOf<String, Any?>("or" to listOf<Any?>("0", true)),
        data = emptyMap<String, Any?>(),
        expected = "0",
    )

    // If the conditional is logic, it gets evaluated
    @Test
    fun ifTheConditionalIsLogicItGetsEvaluated_163() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(mapOf<String, Any?>(">" to listOf<Any?>(2, 1)), "apple", "banana")),
        data = null,
        expected = "apple",
    )

    @Test
    fun ifTheConditionalIsLogicItGetsEvaluated_164() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(mapOf<String, Any?>(">" to listOf<Any?>(1, 2)), "apple", "banana")),
        data = null,
        expected = "banana",
    )

    // If the consequents are logic, they get evaluated
    @Test
    fun ifTheConsequentsAreLogicTheyGetEvaluated_165() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(true, mapOf<String, Any?>("cat" to listOf<Any?>("ap", "ple")), mapOf<String, Any?>("cat" to listOf<Any?>("ba", "na", "na")))),
        data = null,
        expected = "apple",
    )

    @Test
    fun ifTheConsequentsAreLogicTheyGetEvaluated_166() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(false, mapOf<String, Any?>("cat" to listOf<Any?>("ap", "ple")), mapOf<String, Any?>("cat" to listOf<Any?>("ba", "na", "na")))),
        data = null,
        expected = "banana",
    )

    // If/then/elseif/then cases
    @Test
    fun ifThenElseifThenCases_167() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(true, "apple", true, "banana")),
        data = null,
        expected = "apple",
    )

    @Test
    fun ifThenElseifThenCases_168() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(true, "apple", false, "banana")),
        data = null,
        expected = "apple",
    )

    @Test
    fun ifThenElseifThenCases_169() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(false, "apple", true, "banana")),
        data = null,
        expected = "banana",
    )

    @Test
    fun ifThenElseifThenCases_170() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(false, "apple", false, "banana")),
        data = null,
        expected = null,
    )

    @Test
    fun ifThenElseifThenCases_171() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(true, "apple", true, "banana", "carrot")),
        data = null,
        expected = "apple",
    )

    @Test
    fun ifThenElseifThenCases_172() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(true, "apple", false, "banana", "carrot")),
        data = null,
        expected = "apple",
    )

    @Test
    fun ifThenElseifThenCases_173() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(false, "apple", true, "banana", "carrot")),
        data = null,
        expected = "banana",
    )

    @Test
    fun ifThenElseifThenCases_174() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(false, "apple", false, "banana", "carrot")),
        data = null,
        expected = "carrot",
    )

    @Test
    fun ifThenElseifThenCases_175() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(false, "apple", false, "banana", false, "carrot")),
        data = null,
        expected = null,
    )

    @Test
    fun ifThenElseifThenCases_176() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(false, "apple", false, "banana", false, "carrot", "date")),
        data = null,
        expected = "date",
    )

    @Test
    fun ifThenElseifThenCases_177() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(false, "apple", false, "banana", true, "carrot", "date")),
        data = null,
        expected = "carrot",
    )

    @Test
    fun ifThenElseifThenCases_178() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(false, "apple", true, "banana", false, "carrot", "date")),
        data = null,
        expected = "banana",
    )

    @Test
    fun ifThenElseifThenCases_179() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(false, "apple", true, "banana", true, "carrot", "date")),
        data = null,
        expected = "banana",
    )

    @Test
    fun ifThenElseifThenCases_180() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(true, "apple", false, "banana", false, "carrot", "date")),
        data = null,
        expected = "apple",
    )

    @Test
    fun ifThenElseifThenCases_181() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(true, "apple", false, "banana", true, "carrot", "date")),
        data = null,
        expected = "apple",
    )

    @Test
    fun ifThenElseifThenCases_182() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(true, "apple", true, "banana", false, "carrot", "date")),
        data = null,
        expected = "apple",
    )

    @Test
    fun ifThenElseifThenCases_183() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(true, "apple", true, "banana", true, "carrot", "date")),
        data = null,
        expected = "apple",
    )

    // Arrays with logic
    @Test
    fun arraysWithLogic_184() = checkConformance(
        rule = listOf<Any?>(1, mapOf<String, Any?>("var" to "x"), 3),
        data = mapOf<String, Any?>("x" to 2),
        expected = listOf<Any?>(1, 2, 3),
    )

    @Test
    fun arraysWithLogic_185() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(mapOf<String, Any?>("var" to "x"), listOf<Any?>(mapOf<String, Any?>("var" to "y")), 99)),
        data = mapOf<String, Any?>("x" to true, "y" to 42),
        expected = listOf<Any?>(42),
    )

    // # Compound Tests
    @Test
    fun compoundTests_186() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(mapOf<String, Any?>(">" to listOf<Any?>(3, 1)), true)),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun compoundTests_187() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(mapOf<String, Any?>(">" to listOf<Any?>(3, 1)), false)),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun compoundTests_188() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(mapOf<String, Any?>(">" to listOf<Any?>(3, 1)), mapOf<String, Any?>("!" to true))),
        data = emptyMap<String, Any?>(),
        expected = false,
    )

    @Test
    fun compoundTests_189() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(mapOf<String, Any?>(">" to listOf<Any?>(3, 1)), mapOf<String, Any?>("<" to listOf<Any?>(1, 3)))),
        data = emptyMap<String, Any?>(),
        expected = true,
    )

    @Test
    fun compoundTests_190() = checkConformance(
        rule = mapOf<String, Any?>("?:" to listOf<Any?>(mapOf<String, Any?>(">" to listOf<Any?>(3, 1)), "visible", "hidden")),
        data = emptyMap<String, Any?>(),
        expected = "visible",
    )

    // # Data-Driven
    @Test
    fun dataDriven_191() = checkConformance(
        rule = mapOf<String, Any?>("var" to listOf<Any?>("a")),
        data = mapOf<String, Any?>("a" to 1),
        expected = 1,
    )

    @Test
    fun dataDriven_192() = checkConformance(
        rule = mapOf<String, Any?>("var" to listOf<Any?>("b")),
        data = mapOf<String, Any?>("a" to 1),
        expected = null,
    )

    @Test
    fun dataDriven_193() = checkConformance(
        rule = mapOf<String, Any?>("var" to listOf<Any?>("a")),
        data = null,
        expected = null,
    )

    @Test
    fun dataDriven_194() = checkConformance(
        rule = mapOf<String, Any?>("var" to "a"),
        data = mapOf<String, Any?>("a" to 1),
        expected = 1,
    )

    @Test
    fun dataDriven_195() = checkConformance(
        rule = mapOf<String, Any?>("var" to "b"),
        data = mapOf<String, Any?>("a" to 1),
        expected = null,
    )

    @Test
    fun dataDriven_196() = checkConformance(
        rule = mapOf<String, Any?>("var" to "a"),
        data = null,
        expected = null,
    )

    @Test
    fun dataDriven_197() = checkConformance(
        rule = mapOf<String, Any?>("var" to listOf<Any?>("a", 1)),
        data = null,
        expected = 1,
    )

    @Test
    fun dataDriven_198() = checkConformance(
        rule = mapOf<String, Any?>("var" to listOf<Any?>("b", 2)),
        data = mapOf<String, Any?>("a" to 1),
        expected = 2,
    )

    @Test
    fun dataDriven_199() = checkConformance(
        rule = mapOf<String, Any?>("var" to "a.b"),
        data = mapOf<String, Any?>("a" to mapOf<String, Any?>("b" to "c")),
        expected = "c",
    )

    @Test
    fun dataDriven_200() = checkConformance(
        rule = mapOf<String, Any?>("var" to "a.q"),
        data = mapOf<String, Any?>("a" to mapOf<String, Any?>("b" to "c")),
        expected = null,
    )

    @Test
    fun dataDriven_201() = checkConformance(
        rule = mapOf<String, Any?>("var" to listOf<Any?>("a.q", 9)),
        data = mapOf<String, Any?>("a" to mapOf<String, Any?>("b" to "c")),
        expected = 9,
    )

    @Test
    fun dataDriven_202() = checkConformance(
        rule = mapOf<String, Any?>("var" to 1),
        data = listOf<Any?>("apple", "banana"),
        expected = "banana",
    )

    @Test
    fun dataDriven_203() = checkConformance(
        rule = mapOf<String, Any?>("var" to "1"),
        data = listOf<Any?>("apple", "banana"),
        expected = "banana",
    )

    @Test
    fun dataDriven_204() = checkConformance(
        rule = mapOf<String, Any?>("var" to "1.1"),
        data = listOf<Any?>("apple", listOf<Any?>("banana", "beer")),
        expected = "beer",
    )

    @Test
    fun dataDriven_205() = checkConformance(
        rule = mapOf<String, Any?>("and" to listOf<Any?>(mapOf<String, Any?>("<" to listOf<Any?>(mapOf<String, Any?>("var" to "temp"), 110)), mapOf<String, Any?>("==" to listOf<Any?>(mapOf<String, Any?>("var" to "pie.filling"), "apple")))),
        data = mapOf<String, Any?>("temp" to 100, "pie" to mapOf<String, Any?>("filling" to "apple")),
        expected = true,
    )

    @Test
    fun dataDriven_206() = checkConformance(
        rule = mapOf<String, Any?>("var" to listOf<Any?>(mapOf<String, Any?>("?:" to listOf<Any?>(mapOf<String, Any?>("<" to listOf<Any?>(mapOf<String, Any?>("var" to "temp"), 110)), "pie.filling", "pie.eta")))),
        data = mapOf<String, Any?>("temp" to 100, "pie" to mapOf<String, Any?>("filling" to "apple", "eta" to "60s")),
        expected = "apple",
    )

    @Test
    fun dataDriven_207() = checkConformance(
        rule = mapOf<String, Any?>("in" to listOf<Any?>(mapOf<String, Any?>("var" to "filling"), listOf<Any?>("apple", "cherry"))),
        data = mapOf<String, Any?>("filling" to "apple"),
        expected = true,
    )

    @Test
    fun dataDriven_208() = checkConformance(
        rule = mapOf<String, Any?>("var" to "a.b.c"),
        data = null,
        expected = null,
    )

    @Test
    fun dataDriven_209() = checkConformance(
        rule = mapOf<String, Any?>("var" to "a.b.c"),
        data = mapOf<String, Any?>("a" to null),
        expected = null,
    )

    @Test
    fun dataDriven_210() = checkConformance(
        rule = mapOf<String, Any?>("var" to "a.b.c"),
        data = mapOf<String, Any?>("a" to mapOf<String, Any?>("b" to null)),
        expected = null,
    )

    @Test
    fun dataDriven_211() = checkConformance(
        rule = mapOf<String, Any?>("var" to ""),
        data = 1,
        expected = 1,
    )

    @Test
    fun dataDriven_212() = checkConformance(
        rule = mapOf<String, Any?>("var" to null),
        data = 1,
        expected = 1,
    )

    @Test
    fun dataDriven_213() = checkConformance(
        rule = mapOf<String, Any?>("var" to emptyList<Any?>()),
        data = 1,
        expected = 1,
    )

    // Missing
    @Test
    fun missing_214() = checkConformance(
        rule = mapOf<String, Any?>("missing" to emptyList<Any?>()),
        data = null,
        expected = emptyList<Any?>(),
    )

    @Test
    fun missing_215() = checkConformance(
        rule = mapOf<String, Any?>("missing" to listOf<Any?>("a")),
        data = null,
        expected = listOf<Any?>("a"),
    )

    @Test
    fun missing_216() = checkConformance(
        rule = mapOf<String, Any?>("missing" to "a"),
        data = null,
        expected = listOf<Any?>("a"),
    )

    @Test
    fun missing_217() = checkConformance(
        rule = mapOf<String, Any?>("missing" to "a"),
        data = mapOf<String, Any?>("a" to "apple"),
        expected = emptyList<Any?>(),
    )

    @Test
    fun missing_218() = checkConformance(
        rule = mapOf<String, Any?>("missing" to listOf<Any?>("a")),
        data = mapOf<String, Any?>("a" to "apple"),
        expected = emptyList<Any?>(),
    )

    @Test
    fun missing_219() = checkConformance(
        rule = mapOf<String, Any?>("missing" to listOf<Any?>("a", "b")),
        data = mapOf<String, Any?>("a" to "apple"),
        expected = listOf<Any?>("b"),
    )

    @Test
    fun missing_220() = checkConformance(
        rule = mapOf<String, Any?>("missing" to listOf<Any?>("a", "b")),
        data = mapOf<String, Any?>("b" to "banana"),
        expected = listOf<Any?>("a"),
    )

    @Test
    fun missing_221() = checkConformance(
        rule = mapOf<String, Any?>("missing" to listOf<Any?>("a", "b")),
        data = mapOf<String, Any?>("a" to "apple", "b" to "banana"),
        expected = emptyList<Any?>(),
    )

    @Test
    fun missing_222() = checkConformance(
        rule = mapOf<String, Any?>("missing" to listOf<Any?>("a", "b")),
        data = emptyMap<String, Any?>(),
        expected = listOf<Any?>("a", "b"),
    )

    @Test
    fun missing_223() = checkConformance(
        rule = mapOf<String, Any?>("missing" to listOf<Any?>("a", "b")),
        data = null,
        expected = listOf<Any?>("a", "b"),
    )

    @Test
    fun missing_224() = checkConformance(
        rule = mapOf<String, Any?>("missing" to listOf<Any?>("a.b")),
        data = null,
        expected = listOf<Any?>("a.b"),
    )

    @Test
    fun missing_225() = checkConformance(
        rule = mapOf<String, Any?>("missing" to listOf<Any?>("a.b")),
        data = mapOf<String, Any?>("a" to "apple"),
        expected = listOf<Any?>("a.b"),
    )

    @Test
    fun missing_226() = checkConformance(
        rule = mapOf<String, Any?>("missing" to listOf<Any?>("a.b")),
        data = mapOf<String, Any?>("a" to mapOf<String, Any?>("c" to "apple cake")),
        expected = listOf<Any?>("a.b"),
    )

    @Test
    fun missing_227() = checkConformance(
        rule = mapOf<String, Any?>("missing" to listOf<Any?>("a.b")),
        data = mapOf<String, Any?>("a" to mapOf<String, Any?>("b" to "apple brownie")),
        expected = emptyList<Any?>(),
    )

    @Test
    fun missing_228() = checkConformance(
        rule = mapOf<String, Any?>("missing" to listOf<Any?>("a.b", "a.c")),
        data = mapOf<String, Any?>("a" to mapOf<String, Any?>("b" to "apple brownie")),
        expected = listOf<Any?>("a.c"),
    )

    // Missing some
    @Test
    fun missingSome_229() = checkConformance(
        rule = mapOf<String, Any?>("missing_some" to listOf<Any?>(1, listOf<Any?>("a", "b"))),
        data = mapOf<String, Any?>("a" to "apple"),
        expected = emptyList<Any?>(),
    )

    @Test
    fun missingSome_230() = checkConformance(
        rule = mapOf<String, Any?>("missing_some" to listOf<Any?>(1, listOf<Any?>("a", "b"))),
        data = mapOf<String, Any?>("b" to "banana"),
        expected = emptyList<Any?>(),
    )

    @Test
    fun missingSome_231() = checkConformance(
        rule = mapOf<String, Any?>("missing_some" to listOf<Any?>(1, listOf<Any?>("a", "b"))),
        data = mapOf<String, Any?>("a" to "apple", "b" to "banana"),
        expected = emptyList<Any?>(),
    )

    @Test
    fun missingSome_232() = checkConformance(
        rule = mapOf<String, Any?>("missing_some" to listOf<Any?>(1, listOf<Any?>("a", "b"))),
        data = mapOf<String, Any?>("c" to "carrot"),
        expected = listOf<Any?>("a", "b"),
    )

    @Test
    fun missingSome_233() = checkConformance(
        rule = mapOf<String, Any?>("missing_some" to listOf<Any?>(2, listOf<Any?>("a", "b", "c"))),
        data = mapOf<String, Any?>("a" to "apple", "b" to "banana"),
        expected = emptyList<Any?>(),
    )

    @Test
    fun missingSome_234() = checkConformance(
        rule = mapOf<String, Any?>("missing_some" to listOf<Any?>(2, listOf<Any?>("a", "b", "c"))),
        data = mapOf<String, Any?>("a" to "apple", "c" to "carrot"),
        expected = emptyList<Any?>(),
    )

    @Test
    fun missingSome_235() = checkConformance(
        rule = mapOf<String, Any?>("missing_some" to listOf<Any?>(2, listOf<Any?>("a", "b", "c"))),
        data = mapOf<String, Any?>("a" to "apple", "b" to "banana", "c" to "carrot"),
        expected = emptyList<Any?>(),
    )

    @Test
    fun missingSome_236() = checkConformance(
        rule = mapOf<String, Any?>("missing_some" to listOf<Any?>(2, listOf<Any?>("a", "b", "c"))),
        data = mapOf<String, Any?>("a" to "apple", "d" to "durian"),
        expected = listOf<Any?>("b", "c"),
    )

    @Test
    fun missingSome_237() = checkConformance(
        rule = mapOf<String, Any?>("missing_some" to listOf<Any?>(2, listOf<Any?>("a", "b", "c"))),
        data = mapOf<String, Any?>("d" to "durian", "e" to "eggplant"),
        expected = listOf<Any?>("a", "b", "c"),
    )

    // Missing and If are friends, because empty arrays are falsey in JsonLogic
    @Test
    fun missingAndIfAreFriendsBecauseEmptyArraysAreFalseyInJsonlogic_238() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(mapOf<String, Any?>("missing" to "a"), "missed it", "found it")),
        data = mapOf<String, Any?>("a" to "apple"),
        expected = "found it",
    )

    @Test
    fun missingAndIfAreFriendsBecauseEmptyArraysAreFalseyInJsonlogic_239() = checkConformance(
        rule = mapOf<String, Any?>("if" to listOf<Any?>(mapOf<String, Any?>("missing" to "a"), "missed it", "found it")),
        data = mapOf<String, Any?>("b" to "banana"),
        expected = "missed it",
    )

    // Missing, Merge, and If are friends. VIN is always required, APR is only required if financing is true.
    @Test
    fun missingMergeAndIfAreFriendsVinIsAlwaysRequiredAprIsOnlyRequiredIfFinancingIsTrue_240() = checkConformance(
        rule = mapOf<String, Any?>("missing" to mapOf<String, Any?>("merge" to listOf<Any?>("vin", mapOf<String, Any?>("if" to listOf<Any?>(mapOf<String, Any?>("var" to "financing"), listOf<Any?>("apr"), emptyList<Any?>()))))),
        data = mapOf<String, Any?>("financing" to true),
        expected = listOf<Any?>("vin", "apr"),
    )

    @Test
    fun missingMergeAndIfAreFriendsVinIsAlwaysRequiredAprIsOnlyRequiredIfFinancingIsTrue_241() = checkConformance(
        rule = mapOf<String, Any?>("missing" to mapOf<String, Any?>("merge" to listOf<Any?>("vin", mapOf<String, Any?>("if" to listOf<Any?>(mapOf<String, Any?>("var" to "financing"), listOf<Any?>("apr"), emptyList<Any?>()))))),
        data = mapOf<String, Any?>("financing" to false),
        expected = listOf<Any?>("vin"),
    )

    // Filter, map, all, none, and some
    @Test
    fun filterMapAllNoneAndSome_242() = checkConformance(
        rule = mapOf<String, Any?>("filter" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), true)),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3)),
        expected = listOf<Any?>(1, 2, 3),
    )

    @Test
    fun filterMapAllNoneAndSome_243() = checkConformance(
        rule = mapOf<String, Any?>("filter" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), false)),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3)),
        expected = emptyList<Any?>(),
    )

    @Test
    fun filterMapAllNoneAndSome_244() = checkConformance(
        rule = mapOf<String, Any?>("filter" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>(">=" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 2)))),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3)),
        expected = listOf<Any?>(2, 3),
    )

    @Test
    fun filterMapAllNoneAndSome_245() = checkConformance(
        rule = mapOf<String, Any?>("filter" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("%" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 2)))),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3)),
        expected = listOf<Any?>(1, 3),
    )

    @Test
    fun filterMapAllNoneAndSome_246() = checkConformance(
        rule = mapOf<String, Any?>("map" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("*" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 2)))),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3)),
        expected = listOf<Any?>(2, 4, 6),
    )

    @Test
    fun filterMapAllNoneAndSome_247() = checkConformance(
        rule = mapOf<String, Any?>("map" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("*" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 2)))),
        data = null,
        expected = emptyList<Any?>(),
    )

    @Test
    fun filterMapAllNoneAndSome_248() = checkConformance(
        rule = mapOf<String, Any?>("map" to listOf<Any?>(mapOf<String, Any?>("var" to "desserts"), mapOf<String, Any?>("var" to "qty"))),
        data = mapOf<String, Any?>("desserts" to listOf<Any?>(mapOf<String, Any?>("name" to "apple", "qty" to 1), mapOf<String, Any?>("name" to "brownie", "qty" to 2), mapOf<String, Any?>("name" to "cupcake", "qty" to 3))),
        expected = listOf<Any?>(1, 2, 3),
    )

    @Test
    fun filterMapAllNoneAndSome_249() = checkConformance(
        rule = mapOf<String, Any?>("reduce" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("+" to listOf<Any?>(mapOf<String, Any?>("var" to "current"), mapOf<String, Any?>("var" to "accumulator"))), 0)),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3, 4)),
        expected = 10,
    )

    @Test
    fun filterMapAllNoneAndSome_250() = checkConformance(
        rule = mapOf<String, Any?>("reduce" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("+" to listOf<Any?>(mapOf<String, Any?>("var" to "current"), mapOf<String, Any?>("var" to "accumulator"))), mapOf<String, Any?>("var" to "start_with"))),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3, 4), "start_with" to 59),
        expected = 69,
    )

    @Test
    fun filterMapAllNoneAndSome_251() = checkConformance(
        rule = mapOf<String, Any?>("reduce" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("+" to listOf<Any?>(mapOf<String, Any?>("var" to "current"), mapOf<String, Any?>("var" to "accumulator"))), 0)),
        data = null,
        expected = 0,
    )

    @Test
    fun filterMapAllNoneAndSome_252() = checkConformance(
        rule = mapOf<String, Any?>("reduce" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("*" to listOf<Any?>(mapOf<String, Any?>("var" to "current"), mapOf<String, Any?>("var" to "accumulator"))), 1)),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3, 4)),
        expected = 24,
    )

    @Test
    fun filterMapAllNoneAndSome_253() = checkConformance(
        rule = mapOf<String, Any?>("reduce" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("*" to listOf<Any?>(mapOf<String, Any?>("var" to "current"), mapOf<String, Any?>("var" to "accumulator"))), 0)),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3, 4)),
        expected = 0,
    )

    @Test
    fun filterMapAllNoneAndSome_254() = checkConformance(
        rule = mapOf<String, Any?>("reduce" to listOf<Any?>(mapOf<String, Any?>("var" to "desserts"), mapOf<String, Any?>("+" to listOf<Any?>(mapOf<String, Any?>("var" to "accumulator"), mapOf<String, Any?>("var" to "current.qty"))), 0)),
        data = mapOf<String, Any?>("desserts" to listOf<Any?>(mapOf<String, Any?>("name" to "apple", "qty" to 1), mapOf<String, Any?>("name" to "brownie", "qty" to 2), mapOf<String, Any?>("name" to "cupcake", "qty" to 3))),
        expected = 6,
    )

    @Test
    fun filterMapAllNoneAndSome_255() = checkConformance(
        rule = mapOf<String, Any?>("all" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>(">=" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 1)))),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3)),
        expected = true,
    )

    @Test
    fun filterMapAllNoneAndSome_256() = checkConformance(
        rule = mapOf<String, Any?>("all" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("==" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 1)))),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3)),
        expected = false,
    )

    @Test
    fun filterMapAllNoneAndSome_257() = checkConformance(
        rule = mapOf<String, Any?>("all" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("<" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 1)))),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3)),
        expected = false,
    )

    @Test
    fun filterMapAllNoneAndSome_258() = checkConformance(
        rule = mapOf<String, Any?>("all" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("<" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 1)))),
        data = mapOf<String, Any?>("integers" to emptyList<Any?>()),
        expected = false,
    )

    @Test
    fun filterMapAllNoneAndSome_259() = checkConformance(
        rule = mapOf<String, Any?>("all" to listOf<Any?>(mapOf<String, Any?>("var" to "items"), mapOf<String, Any?>(">=" to listOf<Any?>(mapOf<String, Any?>("var" to "qty"), 1)))),
        data = mapOf<String, Any?>("items" to listOf<Any?>(mapOf<String, Any?>("qty" to 1, "sku" to "apple"), mapOf<String, Any?>("qty" to 2, "sku" to "banana"))),
        expected = true,
    )

    @Test
    fun filterMapAllNoneAndSome_260() = checkConformance(
        rule = mapOf<String, Any?>("all" to listOf<Any?>(mapOf<String, Any?>("var" to "items"), mapOf<String, Any?>(">" to listOf<Any?>(mapOf<String, Any?>("var" to "qty"), 1)))),
        data = mapOf<String, Any?>("items" to listOf<Any?>(mapOf<String, Any?>("qty" to 1, "sku" to "apple"), mapOf<String, Any?>("qty" to 2, "sku" to "banana"))),
        expected = false,
    )

    @Test
    fun filterMapAllNoneAndSome_261() = checkConformance(
        rule = mapOf<String, Any?>("all" to listOf<Any?>(mapOf<String, Any?>("var" to "items"), mapOf<String, Any?>("<" to listOf<Any?>(mapOf<String, Any?>("var" to "qty"), 1)))),
        data = mapOf<String, Any?>("items" to listOf<Any?>(mapOf<String, Any?>("qty" to 1, "sku" to "apple"), mapOf<String, Any?>("qty" to 2, "sku" to "banana"))),
        expected = false,
    )

    @Test
    fun filterMapAllNoneAndSome_262() = checkConformance(
        rule = mapOf<String, Any?>("all" to listOf<Any?>(mapOf<String, Any?>("var" to "items"), mapOf<String, Any?>(">=" to listOf<Any?>(mapOf<String, Any?>("var" to "qty"), 1)))),
        data = mapOf<String, Any?>("items" to emptyList<Any?>()),
        expected = false,
    )

    @Test
    fun filterMapAllNoneAndSome_263() = checkConformance(
        rule = mapOf<String, Any?>("none" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>(">=" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 1)))),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3)),
        expected = false,
    )

    @Test
    fun filterMapAllNoneAndSome_264() = checkConformance(
        rule = mapOf<String, Any?>("none" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("==" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 1)))),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3)),
        expected = false,
    )

    @Test
    fun filterMapAllNoneAndSome_265() = checkConformance(
        rule = mapOf<String, Any?>("none" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("<" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 1)))),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3)),
        expected = true,
    )

    @Test
    fun filterMapAllNoneAndSome_266() = checkConformance(
        rule = mapOf<String, Any?>("none" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("<" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 1)))),
        data = mapOf<String, Any?>("integers" to emptyList<Any?>()),
        expected = true,
    )

    @Test
    fun filterMapAllNoneAndSome_267() = checkConformance(
        rule = mapOf<String, Any?>("none" to listOf<Any?>(mapOf<String, Any?>("var" to "items"), mapOf<String, Any?>(">=" to listOf<Any?>(mapOf<String, Any?>("var" to "qty"), 1)))),
        data = mapOf<String, Any?>("items" to listOf<Any?>(mapOf<String, Any?>("qty" to 1, "sku" to "apple"), mapOf<String, Any?>("qty" to 2, "sku" to "banana"))),
        expected = false,
    )

    @Test
    fun filterMapAllNoneAndSome_268() = checkConformance(
        rule = mapOf<String, Any?>("none" to listOf<Any?>(mapOf<String, Any?>("var" to "items"), mapOf<String, Any?>(">" to listOf<Any?>(mapOf<String, Any?>("var" to "qty"), 1)))),
        data = mapOf<String, Any?>("items" to listOf<Any?>(mapOf<String, Any?>("qty" to 1, "sku" to "apple"), mapOf<String, Any?>("qty" to 2, "sku" to "banana"))),
        expected = false,
    )

    @Test
    fun filterMapAllNoneAndSome_269() = checkConformance(
        rule = mapOf<String, Any?>("none" to listOf<Any?>(mapOf<String, Any?>("var" to "items"), mapOf<String, Any?>("<" to listOf<Any?>(mapOf<String, Any?>("var" to "qty"), 1)))),
        data = mapOf<String, Any?>("items" to listOf<Any?>(mapOf<String, Any?>("qty" to 1, "sku" to "apple"), mapOf<String, Any?>("qty" to 2, "sku" to "banana"))),
        expected = true,
    )

    @Test
    fun filterMapAllNoneAndSome_270() = checkConformance(
        rule = mapOf<String, Any?>("none" to listOf<Any?>(mapOf<String, Any?>("var" to "items"), mapOf<String, Any?>(">=" to listOf<Any?>(mapOf<String, Any?>("var" to "qty"), 1)))),
        data = mapOf<String, Any?>("items" to emptyList<Any?>()),
        expected = true,
    )

    @Test
    fun filterMapAllNoneAndSome_271() = checkConformance(
        rule = mapOf<String, Any?>("some" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>(">=" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 1)))),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3)),
        expected = true,
    )

    @Test
    fun filterMapAllNoneAndSome_272() = checkConformance(
        rule = mapOf<String, Any?>("some" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("==" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 1)))),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3)),
        expected = true,
    )

    @Test
    fun filterMapAllNoneAndSome_273() = checkConformance(
        rule = mapOf<String, Any?>("some" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("<" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 1)))),
        data = mapOf<String, Any?>("integers" to listOf<Any?>(1, 2, 3)),
        expected = false,
    )

    @Test
    fun filterMapAllNoneAndSome_274() = checkConformance(
        rule = mapOf<String, Any?>("some" to listOf<Any?>(mapOf<String, Any?>("var" to "integers"), mapOf<String, Any?>("<" to listOf<Any?>(mapOf<String, Any?>("var" to ""), 1)))),
        data = mapOf<String, Any?>("integers" to emptyList<Any?>()),
        expected = false,
    )

    @Test
    fun filterMapAllNoneAndSome_275() = checkConformance(
        rule = mapOf<String, Any?>("some" to listOf<Any?>(mapOf<String, Any?>("var" to "items"), mapOf<String, Any?>(">=" to listOf<Any?>(mapOf<String, Any?>("var" to "qty"), 1)))),
        data = mapOf<String, Any?>("items" to listOf<Any?>(mapOf<String, Any?>("qty" to 1, "sku" to "apple"), mapOf<String, Any?>("qty" to 2, "sku" to "banana"))),
        expected = true,
    )

    @Test
    fun filterMapAllNoneAndSome_276() = checkConformance(
        rule = mapOf<String, Any?>("some" to listOf<Any?>(mapOf<String, Any?>("var" to "items"), mapOf<String, Any?>(">" to listOf<Any?>(mapOf<String, Any?>("var" to "qty"), 1)))),
        data = mapOf<String, Any?>("items" to listOf<Any?>(mapOf<String, Any?>("qty" to 1, "sku" to "apple"), mapOf<String, Any?>("qty" to 2, "sku" to "banana"))),
        expected = true,
    )

    @Test
    fun filterMapAllNoneAndSome_277() = checkConformance(
        rule = mapOf<String, Any?>("some" to listOf<Any?>(mapOf<String, Any?>("var" to "items"), mapOf<String, Any?>("<" to listOf<Any?>(mapOf<String, Any?>("var" to "qty"), 1)))),
        data = mapOf<String, Any?>("items" to listOf<Any?>(mapOf<String, Any?>("qty" to 1, "sku" to "apple"), mapOf<String, Any?>("qty" to 2, "sku" to "banana"))),
        expected = false,
    )

    @Test
    fun filterMapAllNoneAndSome_278() = checkConformance(
        rule = mapOf<String, Any?>("some" to listOf<Any?>(mapOf<String, Any?>("var" to "items"), mapOf<String, Any?>(">=" to listOf<Any?>(mapOf<String, Any?>("var" to "qty"), 1)))),
        data = mapOf<String, Any?>("items" to emptyList<Any?>()),
        expected = false,
    )
}
