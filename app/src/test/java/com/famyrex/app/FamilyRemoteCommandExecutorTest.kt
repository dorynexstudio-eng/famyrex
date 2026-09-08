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
class FamilyRemoteCommandExecutorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `accepted command changes local policy and replay is rejected`() {
        clearState()
        val identity = FamyrexDeviceIdentity("device-1", "member-1", "family-1")
        val command = FamilyControlCommand(
            commandId = "cmd-1", familyId = "family-1", memberId = "member-1", deviceId = "device-1",
            action = FamilyControlAction.BLOCK_APP, issuedAtMs = 1_000L, expiresAtMs = 10_000L,
            value = "com.example.app"
        )

        val executor = FamilyRemoteCommandExecutor(context)
        val first = executor.execute(command, identity, nowMs = 2_000L)
        val second = executor.execute(command, identity, nowMs = 3_000L)

        assertTrue(first.success)
        assertFalse(second.success)
        assertTrue(second.reason.orEmpty().contains("procesado"))
        assertTrue(ParentalControlStore(context).load().appRestrictions.any { it.packageName == "com.example.app" && it.blocked })
    }

    @Test
    fun `remote lock is applied and unlock clears it`() {
        clearState()
        val identity = FamyrexDeviceIdentity("device-lock", "member-lock", "family-lock")
        val executor = FamilyRemoteCommandExecutor(context)

        val lock = FamilyControlCommand(
            commandId = "cmd-lock", familyId = "family-lock", memberId = "member-lock", deviceId = "device-lock",
            action = FamilyControlAction.LOCK_DEVICE, issuedAtMs = 1_000L, expiresAtMs = 10_000L
        )
        val unlock = lock.copy(commandId = "cmd-unlock", action = FamilyControlAction.UNLOCK_DEVICE)

        assertTrue(executor.execute(lock, identity, nowMs = 2_000L).success)
        assertTrue(DeviceEmergencyLockStore(context).isLocked())
        assertTrue(executor.execute(unlock, identity, nowMs = 3_000L).success)
        assertFalse(DeviceEmergencyLockStore(context).isLocked())
    }

    @Test
    fun `replayed lock command is rejected without changing state twice`() {
        clearState()
        val identity = FamyrexDeviceIdentity("device-replay", "member-replay", "family-replay")
        val command = FamilyControlCommand(
            commandId = "cmd-lock-replay", familyId = "family-replay", memberId = "member-replay", deviceId = "device-replay",
            action = FamilyControlAction.LOCK_DEVICE, issuedAtMs = 1_000L, expiresAtMs = 10_000L
        )
        val executor = FamilyRemoteCommandExecutor(context)

        val first = executor.execute(command, identity, nowMs = 2_000L)
        val second = executor.execute(command, identity, nowMs = 3_000L)

        assertTrue(first.success)
        assertFalse(second.success)
        assertTrue(DeviceEmergencyLockStore(context).isLocked())
    }

    @Test
    fun `sync policy accepts nullable fields and multiple apps`() {
        clearState()
        val identity = FamyrexDeviceIdentity("device-sync", "member-sync", "family-sync")
        val snapshot = DevicePolicySnapshot(
            deviceId = "device-sync",
            dailyLimitMinutes = null,
            bedtimeStartMinutes = null,
            bedtimeEndMinutes = null,
            apps = listOf(
                AppPolicy("com.example.one", "One", blocked = true, dailyLimitMinutes = null),
                AppPolicy("com.example.two", "Dos, con coma", dailyLimitMinutes = 45, approvalRequired = true)
            ),
            webPolicyVersion = 4L,
            revision = 8L
        )
        val command = FamilyControlCommand(
            commandId = "cmd-sync", familyId = "family-sync", memberId = "member-sync", deviceId = "device-sync",
            action = FamilyControlAction.SYNC_POLICY, issuedAtMs = 1_000L, expiresAtMs = 10_000L,
            value = DevicePolicySnapshotCodec.encode(snapshot)
        )

        val receipt = FamilyRemoteCommandExecutor(context).execute(command, identity, nowMs = 2_000L)
        val stored = ParentalControlStore(context).load()

        assertTrue(receipt.success)
        assertEquals(2, stored.appRestrictions.size)
        assertTrue(stored.appRestrictions.any { it.packageName == "com.example.one" && it.blocked })
        assertTrue(stored.appRestrictions.any { it.packageName == "com.example.two" && it.dailyMinutes == 45 })
    }

    @Test
    fun `sync policy rejects snapshot for another device`() {
        clearState()
        val identity = FamyrexDeviceIdentity("device-real", "member-1", "family-1")
        val snapshot = DevicePolicySnapshot(deviceId = "device-other", revision = 1L)
        val command = FamilyControlCommand(
            commandId = "cmd-wrong-device", familyId = "family-1", memberId = "member-1", deviceId = "device-real",
            action = FamilyControlAction.SYNC_POLICY, issuedAtMs = 1_000L, expiresAtMs = 10_000L,
            value = DevicePolicySnapshotCodec.encode(snapshot)
        )

        val receipt = FamilyRemoteCommandExecutor(context).execute(command, identity, nowMs = 2_000L)
        assertFalse(receipt.success)
        assertTrue(receipt.reason.orEmpty().contains("no coincide"))
    }

    private fun clearState() {
        context.getSharedPreferences("famyrex_parental_controls", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("famyrex_command_gate", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("famyrex_emergency_lock", Context.MODE_PRIVATE).edit().clear().commit()
    }
}
