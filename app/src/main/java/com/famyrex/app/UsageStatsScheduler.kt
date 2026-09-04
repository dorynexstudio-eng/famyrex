package com.famyrex.app

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object UsageStatsScheduler {
    private const val UNIQUE_WORK_NAME = "famyrex_usage_periodic"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<UsageStatsWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
