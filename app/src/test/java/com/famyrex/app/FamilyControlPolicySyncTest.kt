package com.famyrex.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FamilyControlPolicySyncTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `complete snapshot is applied to local policy`() {
        val snapshot = DevicePolicySnapshot(
            deviceId = "device-sync",
            dailyLimitMinutes = 120,
            bedtimeStartMinutes = 22 * 60,
            bedtimeEndMinutes = 7 * 60,
            apps = listOf(
                AppPolicy("com.example.blocked", "Blocked", blocked = true),
                AppPolicy("com.example.limited", "Limited", dailyLimitMinutes = 30)
            ),
            revision = 7L
        )

        val receipt = FamilyControlPolicySync.apply(context, snapshot)

        assertTrue(receipt.success)
        val config = ParentalControlStore(context).load()
        assertTrue(config.screenTimeLimit?.dailyMinutes == 120)
        assertTrue(config.appRestrictions.any { it.packageName == "com.example.blocked" && it.blocked })
        assertTrue(config.appRestrictions.any { it.packageName == "com.example.limited" && it.dailyMinutes == 30 })
    }
}
