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
        context.getSharedPreferences("famyrex_parental_controls", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("famyrex_command_gate", Context.MODE_PRIVATE).edit().clear().commit()

        val identity = FamyrexDeviceIdentity(
            deviceId = "device-1",
            famyrexMemberId = "member-1",
            familyId = "family-1"
        )
        val command = FamilyControlCommand(
            commandId = "cmd-1",
            familyId = "family-1",
            memberId = "member-1",
            deviceId = "device-1",
            action = FamilyControlAction.BLOCK_APP,
            issuedAtMs = 1_000L,
            expiresAtMs = 10_000L,
            value = "com.example.app"
        )

        val executor = FamilyRemoteCommandExecutor(context)
        val first = executor.execute(command, identity, nowMs = 2_000L)
        val second = executor.execute(command, identity, nowMs = 3_000L)

        assertTrue(first.success)
        assertFalse(second.success)
        assertTrue(second.reason.orEmpty().contains("procesado"))
        assertTrue(
            ParentalControlStore(context).load().appRestrictions
                .any { it.packageName == "com.example.app" && it.blocked }
        )
    }

    @Test
    fun `unsupported command produces explicit failure`() {
        context.getSharedPreferences("famyrex_command_gate", Context.MODE_PRIVATE).edit().clear().commit()
        val identity = FamyrexDeviceIdentity("device-2", "member-2", "family-2")
        val command = FamilyControlCommand(
            commandId = "cmd-unsupported",
            familyId = "family-2",
            memberId = "member-2",
            deviceId = "device-2",
            action = FamilyControlAction.LOCK_DEVICE,
            issuedAtMs = 1_000L,
            expiresAtMs = 10_000L
        )

        val receipt = FamilyRemoteCommandExecutor(context).execute(command, identity, nowMs = 2_000L)

        assertFalse(receipt.success)
        assertEquals(FamilyControlAction.LOCK_DEVICE, receipt.action)
        assertTrue(receipt.reason.orEmpty().contains("ejecución local segura"))
    }
}
