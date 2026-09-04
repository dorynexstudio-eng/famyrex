package com.famyrex.app

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Puente entre las notificaciones permitidas por Android y el detector local.
 *
 * El texto original solo vive en memoria durante el análisis. No se persiste.
 * El resultado persistido contiene únicamente señales estructuradas.
 */
class FamyrexNotificationListenerService : NotificationListenerService() {
    companion object {
        private const val WINDOW_MS = 30 * 60 * 1000L
        private const val MAX_OBSERVATIONS = 40
        private const val INCIDENT_COOLDOWN_MS = 30 * 60 * 1000L
    }

    private val observations = ArrayDeque<CommunicationObservation>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        if (!CommunicationMonitoringSettings.isNotificationListenerEnabled(this)) return

        val text = extractNotificationText(sbn.notification)
        if (text.isBlank()) return

        val now = System.currentTimeMillis().coerceAtLeast(sbn.postTime)
        synchronized(observations) {
            observations.addLast(
                CommunicationObservation(
                    timestampMs = now,
                    sourcePackage = sbn.packageName,
                    normalizedText = text,
                    isIncoming = true
                )
            )
            prune(now)

            val summary = CommunicationSignalDetector.detect(observations.toList())
            if (!summary.shouldAlert) return

            if (hasRecentEquivalentIncident(summary, sbn.packageName, now)) return
            persistIncident(summary, sbn.packageName, now)
        }
    }

    private fun extractNotificationText(notification: Notification): String {
        val extras = notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()

        // Prefer el cuerpo; BIG_TEXT puede ser una versión expandida del mismo aviso.
        // No añadimos el título salvo que no exista cuerpo, para reducir falsos positivos.
        return when {
            text.isNotBlank() -> text
            bigText.isNotBlank() -> bigText
            else -> extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        }.trim()
    }

    private fun prune(now: Long) {
        while (observations.isNotEmpty() && now - observations.first().timestampMs > WINDOW_MS) {
            observations.removeFirst()
        }
        while (observations.size > MAX_OBSERVATIONS) {
            observations.removeFirst()
        }
    }

    private fun hasRecentEquivalentIncident(
        summary: CommunicationRiskSummary,
        sourcePackage: String,
        now: Long
    ): Boolean {
        val types = summary.signals.map { it.type }.distinct().sortedBy { it.name }
        return CommunicationRiskIncidentStore(this).load().any { incident ->
            incident.sourcePackage == sourcePackage &&
                now - incident.createdAtMs in 0..INCIDENT_COOLDOWN_MS &&
                incident.reasons.map { it.code }.sorted() ==
                    types.map { type -> CommunicationRiskReasonCatalog.fromSignal(
                        CommunicationRiskSignal(type, RiskConfidence.LOW, "")
                    ).code }.sorted()
        }
    }

    private fun persistIncident(
        summary: CommunicationRiskSummary,
        sourcePackage: String,
        timestampMs: Long
    ) {
        val signals = summary.signals
        val incident = CommunicationRiskIncident(
            id = "communication_${sourcePackage}_${timestampMs}_${summary.score}",
            createdAtMs = timestampMs,
            type = signals.maxByOrNull { confidenceWeight(it.confidence) }?.type ?: return,
            confidence = summary.confidence,
            score = summary.score,
            reasons = signals.map(CommunicationRiskReasonCatalog::fromSignal)
                .distinctBy { it.code },
            sourcePackage = sourcePackage
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
