package com.famyrex.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object FamyrexWorkScheduler {
    private const val PROTECTION_WORK = "famyrex_protection_health"
    private const val DEVICE_SECURITY_WORK = "famyrex_device_security"
    private const val HOURLY_USAGE_WORK = "famyrex_hourly_usage"
    private const val REPORT_WORK = "famyrex_reports"
    private const val AI_ANALYSIS_WORK = "famyrex_ai_analysis_daily"
    private const val REMOTE_COMMAND_WORK = "famyrex_remote_command_recovery"
    private const val FAMILY_LOCATION_WORK = "famyrex_family_location_sync"
    private const val DEVICE_TOKEN_WORK = "famyrex_device_token_registration"

    fun scheduleProtectionHealth(context: Context) {
        val appContext = context.applicationContext
        GeofenceBootstrap.sync(appContext)
        UsageStatsScheduler.schedule(appContext)

        val workManager = WorkManager.getInstance(appContext)

        val protectionRequest = PeriodicWorkRequestBuilder<ProtectionHealthWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(PROTECTION_WORK, ExistingPeriodicWorkPolicy.KEEP, protectionRequest)

        val deviceSecurityRequest = PeriodicWorkRequestBuilder<DeviceSecurityWorker>(30, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(DEVICE_SECURITY_WORK, ExistingPeriodicWorkPolicy.KEEP, deviceSecurityRequest)

        val hourlyUsageRequest = PeriodicWorkRequestBuilder<HourlyUsageWorker>(1, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(HOURLY_USAGE_WORK, ExistingPeriodicWorkPolicy.KEEP, hourlyUsageRequest)

        val reportRequest = PeriodicWorkRequestBuilder<ReportWorker>(24, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(REPORT_WORK, ExistingPeriodicWorkPolicy.KEEP, reportRequest)

        val aiRequest = PeriodicWorkRequestBuilder<AiAnalysisWorker>(24, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(AI_ANALYSIS_WORK, ExistingPeriodicWorkPolicy.KEEP, aiRequest)

        val remoteConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val remoteRequest = PeriodicWorkRequestBuilder<RemoteCommandQueueWorker>(15, TimeUnit.MINUTES)
            .setConstraints(remoteConstraints)
            .build()
        workManager.enqueueUniquePeriodicWork(REMOTE_COMMAND_WORK, ExistingPeriodicWorkPolicy.KEEP, remoteRequest)

        val tokenRequest = PeriodicWorkRequestBuilder<DeviceTokenRegistrationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(remoteConstraints)
            .build()
        workManager.enqueueUniquePeriodicWork(DEVICE_TOKEN_WORK, ExistingPeriodicWorkPolicy.KEEP, tokenRequest)

        val locationRequest = PeriodicWorkRequestBuilder<FamilyLocationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(remoteConstraints)
            .build()
        workManager.enqueueUniquePeriodicWork(FAMILY_LOCATION_WORK, ExistingPeriodicWorkPolicy.KEEP, locationRequest)
    }
}
