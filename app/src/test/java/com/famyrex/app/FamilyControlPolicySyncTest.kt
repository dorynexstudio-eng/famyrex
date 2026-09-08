package com.famyrex.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FamilyControlPolicySyncTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `complete snapshot is applied to local policy`() {
        ParentalControlStore(context).clear()
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
        val config = ParentalControlStore(context).load()

        assertTrue(receipt.success)
        assertTrue(config.screenTimeLimit?.dailyMinutes == 120)
        assertTrue(config.appRestrictions.any { it.packageName == "com.example.blocked" && it.blocked })
        assertTrue(config.appRestrictions.any { it.packageName == "com.example.limited" && it.dailyMinutes == 30 })
    }

    @Test
    fun `invalid snapshot is rejected without replacing current policy`() {
        ParentalControlStore(context).clear()
        val valid = DevicePolicySnapshot("device-sync", dailyLimitMinutes = 90, revision = 1L)
        FamilyControlPolicySync.apply(context, valid)

        val invalid = valid.copy(dailyLimitMinutes = 0, revision = 2L)
        val receipt = FamilyControlPolicySync.apply(context, invalid)
        val config = ParentalControlStore(context).load()

        assertFalse(receipt.success)
        assertTrue(config.screenTimeLimit?.dailyMinutes == 90)
    }

    @Test
    fun `older revision is rejected without replacing newer policy`() {
        ParentalControlStore(context).clear()
        val newer = DevicePolicySnapshot("device-sync", dailyLimitMinutes = 180, revision = 8L)
        val older = DevicePolicySnapshot("device-sync", dailyLimitMinutes = 30, revision = 7L)

        assertTrue(FamilyControlPolicySync.apply(context, newer).success)
        val receipt = FamilyControlPolicySync.apply(context, older)
        val config = ParentalControlStore(context).load()

        assertFalse(receipt.success)
        assertTrue(config.screenTimeLimit?.dailyMinutes == 180)
    }
}
