package com.famyrex.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
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
fun FamyrexAlertsScreen(context: android.content.Context, modifier: Modifier = Modifier) {
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
            AlertLifecycleCard(alert, onStatus = { status -> vm.updateStatus(alert, status) })
        }
        item {
            OutlinedButton(onClick = vm::refresh, Modifier.fillMaxWidth()) { Text("Actualizar") }
        }
    }
}

@Composable
private fun AlertLifecycleCard(alert: SmartAlert, onStatus: (AlertLifecycleStatus) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(alert.title, style = MaterialTheme.typography.titleMedium)
            Text("${alert.severity.name} · ${alert.lifecycleStatus.displayName()}")
            Spacer(Modifier.height(6.dp))
            Text(alert.message)
            Spacer(Modifier.height(6.dp))
            Text(alert.date, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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

private fun AlertLifecycleStatus.displayName(): String = when (this) {
    AlertLifecycleStatus.DETECTED -> "Detectada"
    AlertLifecycleStatus.REVIEWED -> "Revisada"
    AlertLifecycleStatus.CONFIRMED -> "Confirmada"
    AlertLifecycleStatus.DISMISSED -> "Descartada por el adulto"
    AlertLifecycleStatus.AUTO_DISMISSED -> "Cerrada automáticamente"
    AlertLifecycleStatus.RESOLVED -> "Resuelta"
}
