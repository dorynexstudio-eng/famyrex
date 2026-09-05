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
    fun `blocked app is restricted`() {
        val config = ParentalControlConfig(
            appRestrictions = listOf(AppRestriction("com.example", blocked = true))
        )
        assertTrue(ParentalPolicyEngine.evaluate(config, "com.example", 0).restricted)
        assertFalse(ParentalPolicyEngine.evaluate(config, "com.other", 0).restricted)
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

    private fun timestamp(hour: Int, minute: Int): Long =
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
}
