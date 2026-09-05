package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentalPolicyEngineTest {
    @Test
    fun `daily screen limit restricts when reached`() {
        val config = ParentalControlConfig(screenTimeLimit = ScreenTimeLimit(60))
        assertFalse(ParentalPolicyEngine.evaluate(config, "com.example", 59).restricted)
        assertTrue(ParentalPolicyEngine.evaluate(config, "com.example", 60).restricted)
    }

    @Test
    fun `disabled daily screen limit does not restrict`() {
        val config = ParentalControlConfig(screenTimeLimit = ScreenTimeLimit(60, enabled = false))
        assertFalse(ParentalPolicyEngine.evaluate(config, "com.example", 120).restricted)
    }

    @Test
    fun `blocked app is restricted`() {
        val config = ParentalControlConfig(
            appRestrictions = listOf(AppRestriction("com.example", blocked = true))
        )
        assertTrue(ParentalPolicyEngine.evaluate(config, "com.example", 0).restricted)
        assertFalse(ParentalPolicyEngine.evaluate(config, "com.other", 0).restricted)
    }

    @Test
    fun `app daily limit restricts only the matching app`() {
        val config = ParentalControlConfig(
            appRestrictions = listOf(AppRestriction("com.example", dailyMinutes = 30))
        )
        assertFalse(ParentalPolicyEngine.evaluate(config, "com.example", 29).restricted)
        assertTrue(ParentalPolicyEngine.evaluate(config, "com.example", 30).restricted)
        assertFalse(ParentalPolicyEngine.evaluate(config, "com.other", 30).restricted)
    }

    @Test
    fun `app daily limit and block produce distinct reasons`() {
        val config = ParentalControlConfig(
            appRestrictions = listOf(
                AppRestriction("com.example", dailyMinutes = 30, blocked = true)
            )
        )
        val result = ParentalPolicyEngine.evaluate(config, "com.example", 30)
        assertTrue(result.restricted)
        assertTrue(result.reasons.any { it.contains("bloqueada") })
        assertTrue(result.reasons.any { it.contains("límite diario") })
    }

    @Test
    fun `global screen limit uses total screen time independently of app time`() {
        val config = ParentalControlConfig(screenTimeLimit = ScreenTimeLimit(60))
        assertTrue(
            ParentalPolicyEngine.evaluate(
                config,
                "com.example",
                appUsedTodayMinutes = 5,
                totalScreenTodayMinutes = 60
            ).restricted
        )
    }

    @Test
    fun `normal schedule restricts only inside interval`() {
        val config = ParentalControlConfig(
            pauseSchedules = listOf(PauseSchedule(9 * 60, 14 * 60))
        )
        assertFalse(ParentalPolicyEngine.evaluate(config, "com.example", 0, nowMs = timestamp(8, 59)).restricted)
        assertTrue(ParentalPolicyEngine.evaluate(config, "com.example", 0, nowMs = timestamp(9, 0)).restricted)
        assertTrue(ParentalPolicyEngine.evaluate(config, "com.example", 0, nowMs = timestamp(13, 59)).restricted)
        assertFalse(ParentalPolicyEngine.evaluate(config, "com.example", 0, nowMs = timestamp(14, 0)).restricted)
    }

    @Test
    fun `overnight schedule crosses midnight`() {
        val config = ParentalControlConfig(
            pauseSchedules = listOf(PauseSchedule(22 * 60, 7 * 60))
        )
        assertTrue(ParentalPolicyEngine.evaluate(config, "com.example", 0, nowMs = timestamp(23, 0)).restricted)
        assertTrue(ParentalPolicyEngine.evaluate(config, "com.example", 0, nowMs = timestamp(6, 0)).restricted)
        assertFalse(ParentalPolicyEngine.evaluate(config, "com.example", 0, nowMs = timestamp(12, 0)).restricted)
    }

    @Test
    fun `multiple schedules restrict inside either interval`() {
        val config = ParentalControlConfig(
            pauseSchedules = listOf(
                PauseSchedule(8 * 60, 9 * 60),
                PauseSchedule(20 * 60, 21 * 60)
            )
        )
        assertTrue(ParentalPolicyEngine.evaluate(config, "com.example", 0, nowMs = timestamp(8, 30)).restricted)
        assertTrue(ParentalPolicyEngine.evaluate(config, "com.example", 0, nowMs = timestamp(20, 30)).restricted)
        assertFalse(ParentalPolicyEngine.evaluate(config, "com.example", 0, nowMs = timestamp(12, 0)).restricted)
    }

    @Test
    fun `equal schedule endpoints mean full day pause`() {
        val config = ParentalControlConfig(
            pauseSchedules = listOf(PauseSchedule(0, 0))
        )
        assertTrue(ParentalPolicyEngine.evaluate(config, "com.example", 0, nowMs = timestamp(0, 0)).restricted)
        assertTrue(ParentalPolicyEngine.evaluate(config, "com.example", 0, nowMs = timestamp(23, 59)).restricted)
    }

    @Test
    fun `disabled schedule does not restrict`() {
        val config = ParentalControlConfig(
            pauseSchedules = listOf(PauseSchedule(9 * 60, 14 * 60, enabled = false))
        )
        assertFalse(ParentalPolicyEngine.evaluate(config, "com.example", 0, nowMs = timestamp(10, 0)).restricted)
    }

    private fun timestamp(hour: Int, minute: Int): Long =
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
}
