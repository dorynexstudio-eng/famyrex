package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectionTransitionEngineTest {
    private fun component(key: String, status: ProtectionComponentStatus) =
        ProtectionComponent(key, key, status, "detalle")

    @Test
    fun firstSnapshotEstablishesBaselineWithoutAlert() {
        val current = listOf(component("notifications", ProtectionComponentStatus.ACTIVE))
        assertEquals(emptyList<ProtectionTransitionEvent>(), ProtectionTransitionEngine.evaluate(null, current, 100L))
    }

    @Test
    fun activeToDegradedCreatesSingleDegradationTransition() {
        val previous = listOf(component("notifications", ProtectionComponentStatus.ACTIVE))
        val current = listOf(component("notifications", ProtectionComponentStatus.DEGRADED))
        val events = ProtectionTransitionEngine.evaluate(previous, current, 200L)
        assertEquals(1, events.size)
        assertEquals(ProtectionTransition.DEGRADED, events.single().transition)
        assertEquals("notifications", events.single().component.key)
    }

    @Test
    fun repeatedDegradedStateDoesNotCreateTransition() {
        val previous = listOf(component("notifications", ProtectionComponentStatus.DEGRADED))
        val current = listOf(component("notifications", ProtectionComponentStatus.DEGRADED))
        assertEquals(emptyList<ProtectionTransitionEvent>(), ProtectionTransitionEngine.evaluate(previous, current, 200L))
    }

    @Test
    fun degradedToActiveCreatesRestorationTransition() {
        val previous = listOf(component("notifications", ProtectionComponentStatus.DEGRADED))
        val current = listOf(component("notifications", ProtectionComponentStatus.ACTIVE))
        val events = ProtectionTransitionEngine.evaluate(previous, current, 300L)
        assertEquals(1, events.size)
        assertEquals(ProtectionTransition.RESTORED, events.single().transition)
    }

    @Test
    fun componentsAreTrackedIndependently() {
        val previous = listOf(
            component("notifications", ProtectionComponentStatus.ACTIVE),
            component("location", ProtectionComponentStatus.DEGRADED)
        )
        val current = listOf(
            component("notifications", ProtectionComponentStatus.DEGRADED),
            component("location", ProtectionComponentStatus.ACTIVE)
        )
        val events = ProtectionTransitionEngine.evaluate(previous, current, 400L)
        assertEquals(setOf(ProtectionTransition.DEGRADED, ProtectionTransition.RESTORED), events.map { it.transition }.toSet())
    }
}
