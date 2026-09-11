package com.famyrex.app

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WellbeingTrendEngineTest {
    private fun day(index: Int, minutes: Int): DailyUsage = DailyUsage(
        date = LocalDate.of(2026, 8, 1).plusDays(index.toLong()).toString(),
        totalTimeMs = minutes * 60_000L,
        topApps = emptyList()
    )

    private val testToday = LocalDate.of(2026, 8, 9)

    @Test
    fun `insufficient history returns no assessment`() {
        assertEquals(null, WellbeingTrendEngine.evaluate((0 until 6).map { day(it, 120) }, testToday))
    }

    @Test
    fun `stable usage produces stable assessment`() {
        val history = (0 until 8).map { day(it, 120) }
        val result = WellbeingTrendEngine.evaluate(history, testToday)
        assertNotNull(result)
        assertEquals("Patrón estable", result!!.title)
        assertEquals(0, result.score)
    }

    @Test
    fun `persistent increase raises wellbeing score`() {
        val history = listOf(
            day(0, 120), day(1, 120), day(2, 120), day(3, 120),
            day(4, 180), day(5, 190), day(6, 200), day(7, 210)
        )
        val result = WellbeingTrendEngine.evaluate(history, testToday)
        assertNotNull(result)
        assertTrue(result!!.score >= 35)
        assertEquals("Tendencia de uso elevada", result.title)
        assertTrue(result.sustainedDays >= 2)
    }

    @Test
    fun `single high day does not trigger elevated trend`() {
        val history = listOf(
            day(0, 120), day(1, 120), day(2, 120), day(3, 120),
            day(4, 120), day(5, 120), day(6, 120), day(7, 240)
        )
        val result = WellbeingTrendEngine.evaluate(history, testToday)
        assertNotNull(result)
        assertEquals("Patrón estable", result!!.title)
    }

    @Test
    fun `missing completed day does not become a false zero usage day`() {
        val history = listOf(
            day(0, 120), day(1, 120), day(2, 120),
            day(4, 120), day(5, 120), day(6, 120), day(7, 120)
        )
        assertEquals(null, WellbeingTrendEngine.evaluate(history, testToday))
    }

    @Test
    fun `current partial day is excluded from trend`() {
        val history = (0 until 8).map { day(it, if (it == 7) 600 else 120) }
        val result = WellbeingTrendEngine.evaluate(history, testToday)
        assertNotNull(result)
        assertEquals("Patrón estable", result!!.title)
    }
}
