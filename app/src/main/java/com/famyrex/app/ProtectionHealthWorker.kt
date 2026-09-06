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
        val alertStore = AlertStore(context)
        GeofenceBootstrap.sync(context)
        InstalledAppsReconciler.reconcile(context)

        val healthStore = ProtectionHealthStore(context)
        val previousHealth = healthStore.load()
        val health = ProtectionHealthChecker.check(context)
        healthStore.save(health)

        val incidentStore = ProtectionIncidentStore(context)
        val previousComponents = incidentStore.loadComponents()
        val currentComponents = ProtectionComponentChecker.check(context)
        val transitions = ProtectionTransitionEngine.evaluate(previousComponents, currentComponents, health.checkedAtMs)

        transitions.forEach { event ->
            val critical = event.component.key == "notifications" ||
                (event.component.key == "location" && FamilyZoneStore(context).load().any { it.enabled }) ||
                event.component.key == "geofences"

            when (event.transition) {
                ProtectionTransition.DEGRADED -> {
                    incidentStore.markDegraded(event.component.key, event.sinceMs)
                    val since = incidentStore.degradedSince(event.component.key) ?: event.sinceMs
                    val alert = SmartAlert(
                        id = "protection_degraded_${event.component.key}_$since",
                        type = AlertType.PROTECTION_DEGRADED,
                        severity = if (critical) AlertSeverity.IMPORTANT else AlertSeverity.ATTENTION,
                        title = "Protección degradada: ${event.component.name}",
                        message = "${event.component.detail} El problema comenzó en ${formatTime(since)}.",
                        date = now()
                    )
                    deliverIfNeeded(context, alertStore, alert)
                }
                ProtectionTransition.RESTORED -> {
                    val since = incidentStore.degradedSince(event.component.key)
                    val duration = since?.let { formatDuration(event.sinceMs - it) }
                    val alert = SmartAlert(
                        id = "protection_restored_${event.component.key}_${event.sinceMs}",
                        type = AlertType.PROTECTION_RESTORED,
                        severity = AlertSeverity.INFO,
                        title = "Protección restablecida: ${event.component.name}",
                        message = if (duration != null) "Famyrex vuelve a disponer de esta función. La incidencia duró aproximadamente $duration." else "Famyrex vuelve a disponer de esta función de protección.",
                        date = now()
                    )
                    incidentStore.clearDegraded(event.component.key)
                    deliverIfNeeded(context, alertStore, alert)
                }
            }
        }
        incidentStore.saveComponents(currentComponents)

        if (previousHealth != null && !previousHealth.active && health.active && transitions.none { it.transition == ProtectionTransition.RESTORED }) {
            val alert = SmartAlert(
                "protection_restored_global_${health.checkedAtMs}", AlertType.PROTECTION_RESTORED, AlertSeverity.INFO,
                "Protección restablecida", "Famyrex vuelve a disponer de las funciones de protección comprobadas.", now()
            )
            deliverIfNeeded(context, alertStore, alert)
        }

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
            deliverIfNeeded(context, alertStore, alert)
        }

        val history = UsageSnapshotStore(context).loadHistory()
        BehaviorPatternEngine.evaluate(history).forEach { alert ->
            deliverIfNeeded(context, alertStore, alert)
        }

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
            deliverIfNeeded(context, alertStore, alert)
        }

        // Solo marcamos latido correcto al completar todas las comprobaciones sin excepción.
        ProtectionWatchdog(context).recordSuccess(health.checkedAtMs)
        Result.success()
    }.getOrElse { Result.retry() }

    private fun deliverIfNeeded(context: Context, store: AlertStore, alert: SmartAlert) {
        store.appendIfNew(alert)
        if (alert.lifecycleStatus.isTerminal() || store.isNotificationDelivered(alert.id)) return
        if (FamyrexNotificationManager.notify(context, alert)) {
            store.markNotificationDelivered(alert.id)
        }
    }

    private fun AlertLifecycleStatus.isTerminal(): Boolean = when (this) {
        AlertLifecycleStatus.DISMISSED,
        AlertLifecycleStatus.AUTO_DISMISSED,
        AlertLifecycleStatus.RESOLVED -> true
        AlertLifecycleStatus.DETECTED,
        AlertLifecycleStatus.REVIEWED,
        AlertLifecycleStatus.CONFIRMED -> false
    }

    private fun now(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    private fun formatTime(timestampMs: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestampMs))
    private fun formatDuration(durationMs: Long): String {
        val minutes = (durationMs / 60_000L).coerceAtLeast(0L)
        return when {
            minutes < 60 -> "$minutes min"
            minutes < 1440 -> "${minutes / 60} h ${minutes % 60} min"
            else -> "${minutes / 1440} d ${(minutes % 1440) / 60} h"
        }
    }
}
