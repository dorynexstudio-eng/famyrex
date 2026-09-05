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
        assertNull(trend.anomaly)
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
        assertNull(trend.anomaly)
    }

    @Test
    fun unusuallyHighTodayIsMarkedAsAnomaly() {
        val trend = FamilyUsageTrendEvaluator.evaluate(listOf(100L, 110L, 120L, 210L))
        val anomaly = trend.anomaly!!
        assertEquals(FamilyUsageAnomalyType.HIGH, anomaly.type)
        assertEquals(160, anomaly.referenceAverageMinutes.toInt())
        assertEquals(31, anomaly.deviationPercent)
    }

    @Test
    fun unusuallyLowTodayIsMarkedAsAnomaly() {
        val trend = FamilyUsageTrendEvaluator.evaluate(listOf(100L, 110L, 120L, 50L))
        val anomaly = trend.anomaly!!
        assertEquals(FamilyUsageAnomalyType.LOW, anomaly.type)
        assertEquals(160, anomaly.referenceAverageMinutes.toInt())
        assertEquals(68, anomaly.deviationPercent)
    }
}
