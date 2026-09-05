package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WellbeingTrendEngineTest {
    private fun day(index: Int, minutes: Int): DailyUsage = DailyUsage(
        date = "2026-08-${(index + 1).toString().padStart(2, '0')}",
        totalTimeMs = minutes * 60_000L,
        topApps = emptyList()
    )

    @Test
    fun `insufficient history returns no assessment`() {
        assertEquals(null, WellbeingTrendEngine.evaluate((0 until 6).map { day(it, 120) }))
    }

    @Test
    fun `stable usage produces stable assessment`() {
        val history = (0 until 7).map { day(it, 120) }
        val result = WellbeingTrendEngine.evaluate(history)
        assertNotNull(result)
        assertEquals("Patrón estable", result!!.title)
        assertEquals(0, result.score)
    }

    @Test
    fun `persistent increase raises wellbeing score`() {
        val history = listOf(
            day(0, 120), day(1, 120), day(2, 120), day(3, 120),
            day(4, 180), day(5, 190), day(6, 200)
        )
        val result = WellbeingTrendEngine.evaluate(history)
        assertNotNull(result)
        assertTrue(result!!.score >= 35)
        assertEquals("Tendencia de uso elevada", result.title)
        assertTrue(result.sustainedDays >= 2)
    }

    @Test
    fun `single high day does not trigger elevated trend`() {
        val history = listOf(
            day(0, 120), day(1, 120), day(2, 120), day(3, 120),
            day(4, 120), day(5, 120), day(6, 240)
        )
        val result = WellbeingTrendEngine.evaluate(history)
        assertNotNull(result)
        assertEquals("Patrón estable", result!!.title)
    }
}
