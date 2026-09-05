package com.famyrex.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AlertsViewModel(private val context: Context) : ViewModel() {
    private val _alerts = MutableStateFlow<List<SmartAlert>>(emptyList())
    val alerts: StateFlow<List<SmartAlert>> = _alerts.asStateFlow()

    private val _recommendations = MutableStateFlow<List<FamilyRecommendation>>(emptyList())
    val recommendations: StateFlow<List<FamilyRecommendation>> = _recommendations.asStateFlow()

    init { refresh() }

    fun refresh() {
        val loadedAlerts = AlertStore(context).load()
        _alerts.value = loadedAlerts
        _recommendations.value = RecommendationEngine.evaluate(loadedAlerts)
    }

    fun updateStatus(alert: SmartAlert, status: AlertLifecycleStatus) {
        if (alert.lifecycleStatus == status) return

        val alertStore = AlertStore(context)
        if (alertStore.load().none { it.id == alert.id }) {
            refresh()
            return
        }

        if (alert.type == AlertType.COMMUNICATION_RISK) {
            val incidentId = alert.id.removePrefix("communication_risk_")
            val incidentStatus = status.toRiskIncidentStatus()
            val incidentStore = CommunicationRiskIncidentStore(context)

            // The incident is the source of truth for communication-risk lifecycle.
            // Do not let the alert UI move ahead if the incident is missing or the
            // requested transition is invalid.
            if (!incidentStore.updateStatus(incidentId, incidentStatus)) {
                refresh()
                return
            }
        }

        val updatedAlert = alert.copy(lifecycleStatus = status)
        if (!alertStore.replace(updatedAlert)) {
            // The alert may have disappeared between the initial check and replace.
            // Recover it instead of leaving a persisted incident without its UI alert.
            if (!alertStore.appendIfNew(updatedAlert)) {
                refresh()
                return
            }
        }

        refresh()
    }

    private fun AlertLifecycleStatus.toRiskIncidentStatus(): RiskIncidentStatus = when (this) {
        AlertLifecycleStatus.DETECTED -> RiskIncidentStatus.DETECTED
        AlertLifecycleStatus.REVIEWED -> RiskIncidentStatus.REVIEWED
        AlertLifecycleStatus.CONFIRMED -> RiskIncidentStatus.CONFIRMED
        AlertLifecycleStatus.DISMISSED -> RiskIncidentStatus.DISMISSED
        AlertLifecycleStatus.AUTO_DISMISSED -> RiskIncidentStatus.AUTO_DISMISSED
        AlertLifecycleStatus.RESOLVED -> RiskIncidentStatus.RESOLVED
    }
}

class AlertsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AlertsViewModel(context.applicationContext) as T
    }
}
