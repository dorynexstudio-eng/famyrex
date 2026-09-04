package com.famyrex.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProtectionHealthWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val context = applicationContext
        GeofenceBootstrap.sync(context)

        val store = ProtectionHealthStore(context)
        val previous = store.load()
        val health = ProtectionHealthChecker.check(context)
        store.save(health)

        when {
            !health.active -> {
                val signature = health.reasons.sorted().joinToString("|").hashCode().toUInt().toString(16)
                val alert = SmartAlert(
                    id = "protection_degraded_$signature",
                    type = AlertType.PROTECTION_DEGRADED,
                    severity = AlertSeverity.IMPORTANT,
                    title = "Protección degradada",
                    message = "Famyrex no puede garantizar todas las funciones de protección: ${health.reasons.joinToString(" ")}",
                    date = now()
                )
                if (AlertStore(context).appendIfNew(alert)) FamyrexNotificationManager.notify(context, alert)
            }
            previous != null && !previous.active && health.active -> {
                val alert = SmartAlert(
                    id = "protection_restored_${health.checkedAtMs}",
                    type = AlertType.PROTECTION_RESTORED,
                    severity = AlertSeverity.INFO,
                    title = "Protección restablecida",
                    message = "Famyrex vuelve a disponer de las funciones de protección comprobadas.",
                    date = now()
                )
                if (AlertStore(context).appendIfNew(alert)) FamyrexNotificationManager.notify(context, alert)
            }
        }
        Result.success()
    }.getOrElse { Result.retry() }

    private fun now(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
}
