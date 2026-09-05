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
        InstalledAppsReconciler.reconcile(context)

        val store = ProtectionHealthStore(context)
        val previous = store.load()
        val health = ProtectionHealthChecker.check(context)
        store.save(health)

        when {
            !health.active -> {
                val signature = health.reasons.sorted().joinToString("|").hashCode().toUInt().toString(16)
                val alert = SmartAlert("protection_degraded_$signature", AlertType.PROTECTION_DEGRADED, AlertSeverity.IMPORTANT, "Protección degradada", "Famyrex no puede garantizar todas las funciones de protección: ${health.reasons.joinToString(" ")}", now())
                if (AlertStore(context).appendIfNew(alert)) FamyrexNotificationManager.notify(context, alert)
            }
            previous != null && !previous.active && health.active -> {
                val alert = SmartAlert("protection_restored_${health.checkedAtMs}", AlertType.PROTECTION_RESTORED, AlertSeverity.INFO, "Protección restablecida", "Famyrex vuelve a disponer de las funciones de protección comprobadas.", now())
                if (AlertStore(context).appendIfNew(alert)) FamyrexNotificationManager.notify(context, alert)
            }
        }

        // Correlacionamos las señales técnicas antes de alertar para reducir falsos positivos.
        val evasionSignals = EvasionSignalChecker.check(context)
        val evasion = EvasionRiskEngine.evaluate(evasionSignals)
        if (evasionSignals.isNotEmpty()) {
            val signature = evasionSignals.map { it.key }.sorted().joinToString("|").hashCode().toUInt().toString(16)
            val alert = SmartAlert(
                id = "evasion_assessment_$signature",
                type = AlertType.EVASION_SIGNAL,
                severity = if (evasion.shouldAlert) AlertSeverity.IMPORTANT else AlertSeverity.ATTENTION,
                title = evasion.title,
                message = evasion.message,
                date = now()
            )
            if (AlertStore(context).appendIfNew(alert)) FamyrexNotificationManager.notify(context, alert)
        }

        val history = UsageSnapshotStore(context).loadHistory()
        val behaviorAlerts = BehaviorPatternEngine.evaluate(history)
        behaviorAlerts.forEach { alert ->
            if (AlertStore(context).appendIfNew(alert)) FamyrexNotificationManager.notify(context, alert)
        }

        // Bienestar: solo eleva una señal cuando la tendencia es sostenida; no diagnostica ni acusa.
        val wellbeing = WellbeingTrendEngine.evaluate(history)
        if (wellbeing != null && wellbeing.score >= 35) {
            val alert = SmartAlert(
                id = "wellbeing_trend_${history.maxOf { it.date }}",
                type = AlertType.PATTERN_CHANGE,
                severity = if (wellbeing.score >= 70) AlertSeverity.IMPORTANT else AlertSeverity.ATTENTION,
                title = wellbeing.title,
                message = "${wellbeing.summary} ${wellbeing.recommendation}",
                date = now()
            )
            if (AlertStore(context).appendIfNew(alert)) FamyrexNotificationManager.notify(context, alert)
        }

        Result.success()
    }.getOrElse { Result.retry() }

    private fun now(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
}
