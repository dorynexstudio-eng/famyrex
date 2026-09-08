package com.famyrex.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteCommandAuditStoreTest {
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("famyrex_remote_command_audit", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun recordsSuccessfulAndFailedReceiptsWithoutPayload() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = RemoteCommandAuditStore(context)
        val commandId = "audit-test-${System.nanoTime()}"
        store.record(FamilyControlReceipt(commandId, FamilyControlAction.LOCK_DEVICE, 1L, 2L, true))
        store.record(FamilyControlReceipt(commandId + "-failed", FamilyControlAction.SET_DAILY_LIMIT, 3L, 4L, false, "bad value"))

        val recent = store.recent(2)
        assertEquals(2, recent.size)
        assertTrue(recent.any { it.commandId == commandId && it.success })
        assertTrue(recent.any { it.commandId == commandId + "-failed" && it.reason == "bad value" })
    }
}
