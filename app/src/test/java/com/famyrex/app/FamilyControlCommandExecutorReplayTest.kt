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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class FamilyControlCommandExecutorReplayTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("famyrex_command_gate", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("famyrex_parental_controls", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun unsupportedCommandIsNotConsumedAndCanBeRetried() {
        val commandId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val command = FamilyControlCommand(
            commandId = commandId,
            familyId = "family-test",
            memberId = "member-test-1234",
            deviceId = "device-test-1234",
            action = FamilyControlAction.REFRESH_LOCATION,
            issuedAtMs = now,
            expiresAtMs = now + 60_000L
        )
        val identity = FamyrexDeviceIdentity(
            deviceId = command.deviceId,
            famyrexMemberId = command.memberId,
            familyId = command.familyId,
            isSupervised = true
        )

        val first = FamilyControlCommandExecutor(context).execute(command, identity, now)
        val second = FamilyControlCommandExecutor(context).execute(command, identity, now + 1)

        assertFalse(first.success)
        assertFalse(second.success)
        assertTrue(first.reason?.contains("soport", ignoreCase = true) == true)
        assertEquals(first.reason, second.reason)
    }
}
