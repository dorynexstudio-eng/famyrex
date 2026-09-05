package com.famyrex.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FamyrexAlertsScreen(context: Context, modifier: Modifier = Modifier) {
    val vm: AlertsViewModel = viewModel(factory = remember { AlertsViewModelFactory(context) })
    val alerts by vm.alerts.collectAsState()

    LazyColumn(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Alertas", style = MaterialTheme.typography.headlineSmall) }
        item {
            Text("Aquí aparecen las alertas reales generadas por Famyrex. Son señales para revisar el contexto, no diagnósticos.")
        }
        if (alerts.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Sin alertas", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("No hay alertas registradas por ahora.")
                    }
                }
            }
        }
        items(alerts, key = { it.id }) { alert ->
            val incident = remember(alert.id, alert.lifecycleStatus) {
                if (alert.type == AlertType.COMMUNICATION_RISK) {
                    val incidentId = alert.id.removePrefix("communication_risk_")
                    CommunicationRiskIncidentStore(context).load().firstOrNull { it.id == incidentId }
                } else null
            }
            AlertLifecycleCard(
                alert = alert,
                incident = incident,
                onStatus = { status -> vm.updateStatus(alert, status) }
            )
        }
        item {
            OutlinedButton(onClick = vm::refresh, Modifier.fillMaxWidth()) { Text("Actualizar") }
        }
    }
}

@Composable
private fun AlertLifecycleCard(
    alert: SmartAlert,
    incident: CommunicationRiskIncident?,
    onStatus: (AlertLifecycleStatus) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(alert.title, style = MaterialTheme.typography.titleMedium)
            Text("${alert.severity.displayName()} · ${alert.lifecycleStatus.displayName()}")
            Spacer(Modifier.height(8.dp))

            if (incident != null) {
                CommunicationRiskDetails(incident)
                Spacer(Modifier.height(8.dp))
            }

            Text(alert.message)
            Spacer(Modifier.height(6.dp))
            Text(alert.date, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (alert.lifecycleStatus == AlertLifecycleStatus.DETECTED) {
                    Button(onClick = { onStatus(AlertLifecycleStatus.REVIEWED) }) { Text("Revisada") }
                    OutlinedButton(onClick = { onStatus(AlertLifecycleStatus.DISMISSED) }) { Text("Falso positivo") }
                }
                if (alert.lifecycleStatus == AlertLifecycleStatus.REVIEWED) {
                    Button(onClick = { onStatus(AlertLifecycleStatus.CONFIRMED) }) { Text("Confirmar") }
                    OutlinedButton(onClick = { onStatus(AlertLifecycleStatus.DISMISSED) }) { Text("Descartar") }
                }
                if (alert.lifecycleStatus == AlertLifecycleStatus.CONFIRMED) {
                    Button(onClick = { onStatus(AlertLifecycleStatus.RESOLVED) }) { Text("Resolver") }
                }
            }
        }
    }
}

@Composable
private fun CommunicationRiskDetails(incident: CommunicationRiskIncident) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Detalle de la señal", style = MaterialTheme.typography.labelLarge)
        Text("Gravedad: ${incident.score}/100 · ${incident.score.severityLabel()}")
        Text("Confianza: ${incident.confidence.displayName()}")
        Text("Dirección: ${incident.direction.displayName()}")
        Text("Estado: ${incident.status.displayName()}")
        Text("Evolución: ${incident.evolutionLabel()}")
        Text("Señales relacionadas: ${incident.reasons.size}")
        Spacer(Modifier.height(4.dp))
        Text("Evolución del estado", style = MaterialTheme.typography.labelLarge)
        StatusHistory(incident)
        Spacer(Modifier.height(4.dp))
        Text("No se muestra ni se guarda la conversación completa.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StatusHistory(incident: CommunicationRiskIncident) {
    val history = buildList {
        add(RiskIncidentStatusChange(RiskIncidentStatus.DETECTED, incident.createdAtMs))
        addAll(incident.statusHistory)
    }.distinctBy { "${it.status}:${it.timestampMs}" }

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        history.forEachIndexed { index, change ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Text("${index + 1}.", style = MaterialTheme.typography.bodySmall)
                Column {
                    Text(change.status.displayName(), style = MaterialTheme.typography.bodySmall)
                    Text(change.timestampMs.formatHistoryDate(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun Long.formatHistoryDate(): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(this))

private fun AlertSeverity.displayName(): String = when (this) {
    AlertSeverity.INFO -> "Información"
    AlertSeverity.ATTENTION -> "Atención"
    AlertSeverity.IMPORTANT -> "Importante"
}

private fun RiskConfidence.displayName(): String = when (this) {
    RiskConfidence.LOW -> "Baja"
    RiskConfidence.MEDIUM -> "Media"
    RiskConfidence.HIGH -> "Alta"
}

private fun CommunicationDirection.displayName(): String = when (this) {
    CommunicationDirection.INCOMING -> "Recibido"
    CommunicationDirection.OUTGOING -> "Enviado desde el dispositivo"
    CommunicationDirection.UNKNOWN -> "No determinada"
}

private fun RiskIncidentStatus.displayName(): String = when (this) {
    RiskIncidentStatus.DETECTED -> "Detectada"
    RiskIncidentStatus.REVIEWED -> "Revisada"
    RiskIncidentStatus.CONFIRMED -> "Confirmada"
    RiskIncidentStatus.DISMISSED -> "Descartada"
    RiskIncidentStatus.AUTO_DISMISSED -> "Cerrada automáticamente"
    RiskIncidentStatus.RESOLVED -> "Resuelta"
}

private fun AlertLifecycleStatus.displayName(): String = when (this) {
    AlertLifecycleStatus.DETECTED -> "Detectada"
    AlertLifecycleStatus.REVIEWED -> "Revisada"
    AlertLifecycleStatus.CONFIRMED -> "Confirmada"
    AlertLifecycleStatus.DISMISSED -> "Descartada por el adulto"
    AlertLifecycleStatus.AUTO_DISMISSED -> "Cerrada automáticamente"
    AlertLifecycleStatus.RESOLVED -> "Resuelta"
}

private fun Int.severityLabel(): String = when {
    this >= 85 -> "alta"
    this >= 70 -> "moderada-alta"
    else -> "moderada"
}

private fun CommunicationRiskIncident.evolutionLabel(): String = when {
    type == CommunicationRiskType.SELF_HARM && confidence == RiskConfidence.HIGH ->
        "Intervención prioritaria"
    type == CommunicationRiskType.SEXUAL_REQUEST && confidence == RiskConfidence.HIGH ->
        "Intervención prioritaria"
    reasons.size >= 3 && confidence == RiskConfidence.HIGH ->
        "Escalada: varias señales relacionadas"
    reasons.size >= 2 ->
        "Acumulación de varias señales relacionadas"
    else ->
        "Señal temprana; observar evolución"
}
