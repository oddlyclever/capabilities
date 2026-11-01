package com.oddlyclever.jsonlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The public extension point: registering custom eager and lazy operators, overriding
 * standard ones, and the unknown-operator failure mode. This is the contract an app's own
 * operators build on.
 */
class OperatorRegistrationTest {

    @Test
    fun customEagerOperatorReceivesEvaluatedArgs() {
        val logic = JsonLogic.builder()
            .operator("double") { args, _ -> JsCoercion.toNumber(args.firstOrNull()) * 2 }
            .build()
        // The inner {"var": "n"} must arrive already evaluated.
        val rule = mapOf("double" to listOf(mapOf("var" to "n")))
        assertEquals(42L, logic.apply(rule, mapOf("n" to 21)))
    }

    @Test
    fun customEagerOperatorScalarArgIsWrapped() {
        val logic = JsonLogic.builder()
            .operator("double") { args, _ -> JsCoercion.toNumber(args.firstOrNull()) * 2 }
            .build()
        assertEquals(42L, logic.apply(mapOf("double" to 21)))
    }

    @Test
    fun customLazyOperatorControlsEvaluation() {
        // An "unless" that only evaluates the branch it needs.
        var branchesEvaluated = 0
        val probe = Operator { _, _ -> branchesEvaluated++; "evaluated" }
        val logic = JsonLogic.builder()
            .operator("probe", probe)
            .lazyOperator("unless") { args, data, ev ->
                if (!ev.truthy(ev.evaluate(args.getOrNull(0), data))) {
                    ev.evaluate(args.getOrNull(1), data)
                } else {
                    null
                }
            }
            .build()
        val rule = mapOf("unless" to listOf(true, mapOf("probe" to emptyList<Any?>())))
        assertEquals(null, logic.apply(rule))
        assertEquals(0, branchesEvaluated, "lazy operator must not evaluate the skipped branch")
    }

    @Test
    fun customOperatorOverridesStandardOne() {
        val logic = JsonLogic.builder()
            .operator("+") { _, _ -> "overridden" }
            .build()
        assertEquals("overridden", logic.apply(mapOf("+" to listOf(1, 2))))
        // The default instance is untouched.
        assertEquals(3L, JsonLogic.Default.apply(mapOf("+" to listOf(1, 2))))
    }

    @Test
    fun lazyRegistrationDisplacesEagerStandardOperator() {
        // Overriding an eager standard operator with a lazy one must not leave the
        // eager registration shadowing dispatch.
        val logic = JsonLogic.builder()
            .lazyOperator("cat") { args, _, _ -> "lazy:${args.size}" }
            .build()
        assertEquals("lazy:2", logic.apply(mapOf("cat" to listOf("a", "b"))))
    }

    @Test
    fun unknownOperatorThrows() {
        val error = assertFailsWith<JsonLogicException> {
            JsonLogic.Default.apply(mapOf("frobnicate" to listOf(1)))
        }
        assertTrue("frobnicate" in (error.message ?: ""))
    }

    @Test
    fun customLoggerReceivesLogValues() {
        val logged = mutableListOf<Any?>()
        val logic = JsonLogic.builder()
            .logger { logged.add(it) }
            .build()
        assertEquals("value", logic.apply(mapOf("log" to "value")))
        assertEquals(listOf<Any?>("value"), logged)
    }
}
