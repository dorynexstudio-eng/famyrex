package com.famyrex.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ProtectionHealthWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val health = ProtectionHealthChecker.check(applicationContext)
        ProtectionHealthStore(applicationContext).save(health)
        Result.success()
    }.getOrElse { Result.retry() }
}
