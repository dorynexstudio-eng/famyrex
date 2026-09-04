package com.famyrex.app

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Fuente opcional y transparente de observaciones.
 * Solo analiza texto que llega en notificaciones y descarta el contenido original.
 */
class FamyrexNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val combined = listOf(title, text).filter { it.isNotBlank() }.joinToString(" — ")
        if (combined.isBlank()) return

        val signals = NotificationRiskAnalyzer.analyze(combined)
        if (signals.isEmpty()) return

        val summary = CommunicationRiskEngine.evaluate(signals)
        if (!summary.shouldAlert) return

        val incident = CommunicationRiskIncident(
            id = "${sbn.packageName}_${sbn.postTime}_${summary.score}",
            createdAtMs = sbn.postTime,
            type = signals.maxByOrNull { confidenceWeight(it.confidence) }?.type ?: return,
            confidence = summary.confidence,
            score = summary.score,
            reasons = signals.map(CommunicationRiskReasonCatalog::fromSignal).distinctBy { it.code },
            sourcePackage = sbn.packageName
        )

        CommunicationRiskIncidentStore(this).save(incident)
        val alert = CommunicationRiskAlertFactory.createIncidentAlert(incident)
        if (AlertStore(this).appendIfNew(alert)) {
            FamyrexNotificationManager.notify(this, alert)
        }
    }

    private fun confidenceWeight(confidence: RiskConfidence): Int = when (confidence) {
        RiskConfidence.LOW -> 1
        RiskConfidence.MEDIUM -> 2
        RiskConfidence.HIGH -> 3
    }
}
