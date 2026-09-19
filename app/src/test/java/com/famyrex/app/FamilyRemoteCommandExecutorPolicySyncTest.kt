package com.famyrex.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FamilyRemoteCommandExecutorPolicySyncTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun executor() = FamilyRemoteCommandExecutor(context) { null }

    @Before
    fun setUp() {
        context.getSharedPreferences("famyrex_family", Context.MODE_PRIVATE).edit().clear().commit()
        val family = FamilyStore(context)
        val child = family.ensureSupervisedChild("member-1", "Perfil infantil")
        family.addDeviceWithId("device-sync", "Dispositivo infantil", child.id)
        family.setDeviceState("device-sync", DeviceLinkState.LINKED)
        val secret = "0123456789abcdef0123456789abcdef"
        family.saveVerifiedFamilyIdentity("family-1", secret, OfflinePairingTokenCodec.fingerprint(secret))
        family.setAppMode(FamyrexAppMode.SUPERVISED)
    }

    @Test
    fun `remote sync policy is decoded and applied`() {
        ParentalControlStore(context).clear()
        context.getSharedPreferences("famyrex_command_gate", Context.MODE_PRIVATE).edit().clear().commit()
        val identity = FamyrexDeviceIdentity("device-sync", "member-1", "family-1")
        val snapshot = DevicePolicySnapshot(
            deviceId = "device-sync",
            dailyLimitMinutes = 120,
            bedtimeStartMinutes = 1320,
            bedtimeEndMinutes = 420,
            webPolicyVersion = 0L,
            revision = 7L,
            apps = listOf(
                AppPolicy("com.example.blocked", "Blocked", blocked = true),
                AppPolicy("com.example.limited", "Limited", dailyLimitMinutes = 30)
            )
        )
        val command = FamilyControlCommand(
            commandId = "sync-1",
            familyId = "family-1",
            memberId = "member-1",
            deviceId = "device-sync",
            action = FamilyControlAction.SYNC_POLICY,
            issuedAtMs = 1_000L,
            expiresAtMs = 10_000L,
            value = DevicePolicySnapshotCodec.encode(snapshot)
        )

        val receipt = executor().execute(command, identity, nowMs = 2_000L)
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
        val snapshot = DevicePolicySnapshot(
            deviceId = "device-other",
            dailyLimitMinutes = 120,
            bedtimeStartMinutes = 1320,
            bedtimeEndMinutes = 420,
            revision = 7L,
            apps = emptyList()
        )
        val command = FamilyControlCommand(
            commandId = "sync-wrong-device",
            familyId = "family-1",
            memberId = "member-1",
            deviceId = "device-real",
            action = FamilyControlAction.SYNC_POLICY,
            issuedAtMs = 1_000L,
            expiresAtMs = 10_000L,
            value = DevicePolicySnapshotCodec.encode(snapshot)
        )

        val receipt = executor().execute(command, identity, nowMs = 2_000L)

        assertFalse(receipt.success)
        assertTrue(receipt.reason.orEmpty().contains("deviceId"))
    }
}
