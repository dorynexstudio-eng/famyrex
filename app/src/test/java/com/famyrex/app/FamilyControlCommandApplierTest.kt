package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyControlCommandApplierTest {
    private val base = FamilyControlCommand(
        commandId = "cmd-1",
        familyId = "family-1",
        memberId = "member-1",
        deviceId = "device-1",
        action = FamilyControlAction.SET_DAILY_LIMIT,
        issuedAtMs = 1L,
        expiresAtMs = 10_000L
    )

    @Test
    fun `sets daily limit`() {
        val result = FamilyControlCommandApplier.apply(base.copy(value = "120"), ParentalControlConfig())
        assertEquals(120, (result as CommandApplicationResult.Applied).config.screenTimeLimit?.dailyMinutes)
    }

    @Test
    fun `rejects invalid daily limit`() {
        val result = FamilyControlCommandApplier.apply(base.copy(value = "0"), ParentalControlConfig())
        assertTrue(result is CommandApplicationResult.Rejected)
    }

    @Test
    fun `sets schedule`() {
        val command = base.copy(action = FamilyControlAction.SET_SCHEDULE, value = "1320-420")
        val result = FamilyControlCommandApplier.apply(command, ParentalControlConfig())
        val schedule = (result as CommandApplicationResult.Applied).config.pauseSchedules.single()
        assertEquals(1320, schedule.startMinuteOfDay)
        assertEquals(420, schedule.endMinuteOfDay)
    }

    @Test
    fun `blocks and allows app without losing its limit`() {
        val current = ParentalControlConfig(appRestrictions = listOf(AppRestriction("com.example.app", dailyMinutes = 30)))
        val blocked = FamilyControlCommandApplier.apply(
            base.copy(action = FamilyControlAction.BLOCK_APP, value = "com.example.app"), current
        ) as CommandApplicationResult.Applied
        assertTrue(blocked.config.appRestrictions.single().blocked)
        assertEquals(30, blocked.config.appRestrictions.single().dailyMinutes)

        val allowed = FamilyControlCommandApplier.apply(
            base.copy(action = FamilyControlAction.ALLOW_APP, value = "com.example.app"), blocked.config
        ) as CommandApplicationResult.Applied
        assertTrue(!allowed.config.appRestrictions.single().blocked)
        assertEquals(30, allowed.config.appRestrictions.single().dailyMinutes)
    }

    @Test
    fun `sets app limit`() {
        val command = base.copy(action = FamilyControlAction.SET_APP_LIMIT, value = "com.example.app:45")
        val result = FamilyControlCommandApplier.apply(command, ParentalControlConfig()) as CommandApplicationResult.Applied
        assertEquals(45, result.config.appRestrictions.single().dailyMinutes)
    }

    @Test
    fun `unsupported actions are explicit`() {
        val command = base.copy(action = FamilyControlAction.LOCK_DEVICE)
        assertTrue(FamilyControlCommandApplier.apply(command, ParentalControlConfig()) is CommandApplicationResult.Unsupported)
    }
}
