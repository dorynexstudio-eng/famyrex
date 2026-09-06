package com.famyrex.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class HourlyUsageWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val context = applicationContext
        if (!ProtectionHealthChecker.check(context).usageAccessGranted) {
            return Result.success()
        }
        HourlyUsageSnapshotStore(context).save(UsageRepository.loadCurrentHour(context))
        Result.success()
    }.getOrElse { Result.retry() }
}
