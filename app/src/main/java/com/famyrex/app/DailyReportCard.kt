package com.famyrex.app

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DailyReportCard(context: Context, modifier: Modifier = Modifier) {
    var report by remember { mutableStateOf(ReportStore(context).load(ReportPeriod.DAILY)) }

    fun generate() {
        val history = UsageSnapshotStore(context).loadHistory()
        val alerts = AlertStore(context).load()
        val generated = ReportEngine.build(history, alerts, ReportPeriod.DAILY)
        ReportStore(context).save(generated)
        report = generated
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Informe diario", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            val current = report
            if (current == null) {
                Text("⚪ SIN DATOS SUFICIENTES", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text("Todavía no hay un informe diario guardado. Puedes generarlo cuando existan datos de uso.")
            } else {
                Text(current.narrative)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Uso total")
                        Text("${current.totalMinutes} min", style = MaterialTheme.typography.titleMedium)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Media diaria")
                        Text("${current.averageDailyMinutes} min", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Alertas: ${current.alertCount} · Importantes: ${current.importantAlertCount}")
                current.trendPercent?.let { trend ->
                    Text("Tendencia: ${if (trend >= 0) "+" else ""}$trend% frente al periodo anterior")
                }
                if (current.topApps.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Aplicaciones con más uso", style = MaterialTheme.typography.titleSmall)
                    current.topApps.take(3).forEach { app ->
                        Text("• ${app.label}: ${app.totalMinutes} min")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(onClick = { generate() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (report == null) "Generar informe diario" else "Actualizar informe diario")
            }
        }
    }
}
