package com.famyrex.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FamilyRemoteCommandExecutorPolicySyncTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `remote sync policy is decoded and applied`() {
        ParentalControlStore(context).clear()
        context.getSharedPreferences("famyrex_command_gate", Context.MODE_PRIVATE).edit().clear().commit()
        val identity = FamyrexDeviceIdentity("device-sync", "member-1", "family-1")
        val command = FamilyControlCommand(
            commandId = "sync-1",
            familyId = "family-1",
            memberId = "member-1",
            deviceId = "device-sync",
            action = FamilyControlAction.SYNC_POLICY,
            issuedAtMs = 1_000L,
            expiresAtMs = 10_000L,
            value = "device-sync|120|1320|420|7|com.example.blocked,true,null;com.example.limited,false,30"
        )

        val receipt = FamilyRemoteCommandExecutor(context).execute(command, identity, nowMs = 2_000L)
        val config = ParentalControlStore(context).load()

        assertTrue(receipt.success)
        assertEquals(120, config.screenTimeLimit?.dailyMinutes)
        assertTrue(config.appRestrictions.any { it.packageName == "com.example.blocked" && it.blocked })
        assertTrue(config.appRestrictions.any { it.packageName == "com.example.limited" && it.dailyMinutes == 30 })
    }

    @Test
    fun `sync policy with wrong device is rejected`() {
        ParentalControlStore(context).clear()
        context.getSharedPreferences("famyrex_command_gate", Context.MODE_PRIVATE).edit().clear().commit()
        val identity = FamyrexDeviceIdentity("device-real", "member-1", "family-1")
        val command = FamilyControlCommand(
            commandId = "sync-wrong-device",
            familyId = "family-1",
            memberId = "member-1",
            deviceId = "device-real",
            action = FamilyControlAction.SYNC_POLICY,
            issuedAtMs = 1_000L,
            expiresAtMs = 10_000L,
            value = "device-other|120|1320|420|7|"
        )

        val receipt = FamilyRemoteCommandExecutor(context).execute(command, identity, nowMs = 2_000L)

        assertFalse(receipt.success)
        assertTrue(receipt.reason.orEmpty().contains("deviceId"))
    }
}
