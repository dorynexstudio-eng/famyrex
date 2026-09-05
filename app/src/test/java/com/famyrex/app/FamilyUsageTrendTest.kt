package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FamilyUsageTrendTest {
    @Test
    fun insufficientHistoryDoesNotInventDirection() {
        val trend = FamilyUsageTrendEvaluator.evaluate(listOf(90L))
        assertEquals(FamilyUsageTrendDirection.INSUFFICIENT_DATA, trend.direction)
        assertNull(trend.previousAverageMinutes)
    }

    @Test
    fun meaningfulIncreaseIsDetected() {
        val trend = FamilyUsageTrendEvaluator.evaluate(listOf(100L, 110L, 120L, 150L))
        assertEquals(FamilyUsageTrendDirection.INCREASING, trend.direction)
        assertEquals(110.0, trend.previousAverageMinutes!!, 0.01)
    }

    @Test
    fun meaningfulDecreaseIsDetected() {
        val trend = FamilyUsageTrendEvaluator.evaluate(listOf(100L, 110L, 120L, 85L))
        assertEquals(FamilyUsageTrendDirection.DECREASING, trend.direction)
    }

    @Test
    fun smallVariationRemainsStable() {
        val trend = FamilyUsageTrendEvaluator.evaluate(listOf(100L, 110L, 120L, 105L))
        assertEquals(FamilyUsageTrendDirection.STABLE, trend.direction)
    }
}
