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
        val updated = alert.copy(lifecycleStatus = status)
        AlertStore(context).replace(updated)
        if (alert.type == AlertType.COMMUNICATION_RISK) {
            val incidentId = alert.id.removePrefix("communication_risk_")
            val incidentStatus = when (status) {
                AlertLifecycleStatus.DETECTED -> RiskIncidentStatus.DETECTED
                AlertLifecycleStatus.REVIEWED -> RiskIncidentStatus.REVIEWED
                AlertLifecycleStatus.CONFIRMED -> RiskIncidentStatus.CONFIRMED
                AlertLifecycleStatus.DISMISSED -> RiskIncidentStatus.DISMISSED
                AlertLifecycleStatus.AUTO_DISMISSED -> RiskIncidentStatus.AUTO_DISMISSED
                AlertLifecycleStatus.RESOLVED -> RiskIncidentStatus.RESOLVED
            }
            CommunicationRiskIncidentStore(context).updateStatus(incidentId, incidentStatus)
        }
        refresh()
    }
}

class AlertsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AlertsViewModel(context.applicationContext) as T
    }
}
