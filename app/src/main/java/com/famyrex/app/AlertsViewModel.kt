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

    init { refresh() }

    fun refresh() {
        _alerts.value = AlertStore(context).load()
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

        if (!alertStore.replace(alert.copy(lifecycleStatus = status))) {
            // This should only happen if the alert disappeared concurrently.
            // Refresh instead of exposing an in-memory status that was not persisted.
            refresh()
            return
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
