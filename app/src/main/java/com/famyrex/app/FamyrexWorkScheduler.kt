package com.famyrex.app

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object FamyrexWorkScheduler {
    private const val PROTECTION_WORK = "famyrex_protection_health"

    fun scheduleProtectionHealth(context: Context) {
        val request = PeriodicWorkRequestBuilder<ProtectionHealthWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PROTECTION_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
