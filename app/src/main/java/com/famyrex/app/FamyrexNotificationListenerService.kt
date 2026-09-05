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
        private const val MAX_OBSERVATIONS_PER_SOURCE = 40
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
            pruneSource(sbn.packageName)

            val sourceObservations = CommunicationObservationScope.forSource(
                observations = observations.toList(),
                sourcePackage = sbn.packageName
            )
            val summary = CommunicationSignalDetector.detect(sourceObservations)
            if (!summary.shouldAlert) return

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
    }

    private fun pruneSource(sourcePackage: String) {
        var sourceCount = observations.count { it.sourcePackage == sourcePackage }
        if (sourceCount <= MAX_OBSERVATIONS_PER_SOURCE) return

        val iterator = observations.iterator()
        while (iterator.hasNext() && sourceCount > MAX_OBSERVATIONS_PER_SOURCE) {
            if (iterator.next().sourcePackage == sourcePackage) {
                iterator.remove()
                sourceCount--
            }
        }
    }

    private fun findRecentEquivalentIncident(
        summary: CommunicationRiskSummary,
        sourcePackage: String,
        now: Long
    ): CommunicationRiskIncident? {
        return CommunicationRiskIncidentStore(this).load()
            .asSequence()
            .filter { incident ->
                CommunicationRiskEpisodeMatcher.isSameEpisode(
                    summary = summary,
                    incident = incident,
                    sourcePackage = sourcePackage,
                    nowMs = now
                )
            }
            .maxByOrNull { it.createdAtMs }
    }

    private fun persistIncident(
        summary: CommunicationRiskSummary,
        sourcePackage: String,
        timestampMs: Long
    ) {
        val signals = summary.signals
        val store = CommunicationRiskIncidentStore(this)
        val existing = findRecentEquivalentIncident(summary, sourcePackage, timestampMs)
        val incidentId = existing?.id ?: "communication_${sourcePackage}_${timestampMs}_${summary.score}"
        val incident = CommunicationRiskIncident(
            id = incidentId,
            createdAtMs = existing?.createdAtMs ?: timestampMs,
            type = CommunicationRiskTypeSelector.select(signals) ?: return,
            confidence = summary.confidence,
            score = summary.score,
            reasons = signals.map(CommunicationRiskReasonCatalog::fromSignal)
                .distinctBy { it.code },
            sourcePackage = sourcePackage,
            direction = signals.firstOrNull()?.direction ?: existing?.direction ?: CommunicationDirection.UNKNOWN,
            status = existing?.status ?: RiskIncidentStatus.DETECTED,
            statusHistory = existing?.statusHistory ?: emptyList()
        )

        store.save(incident)
        val alert = CommunicationRiskAlertFactory.createIncidentAlert(incident)
        val alerts = AlertStore(this)
        if (existing != null) {
            if (alerts.replace(alert) && CommunicationRiskNotificationPolicy.shouldNotify(existing, incident)) {
                FamyrexNotificationManager.notify(this, alert)
            }
        } else if (alerts.appendIfNew(alert)) {
            FamyrexNotificationManager.notify(this, alert)
        }
    }
}
