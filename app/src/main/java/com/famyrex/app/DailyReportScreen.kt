package com.famyrex.app

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Detailed local daily report. It intentionally reads only Famyrex local stores.
 */
@Composable
fun DailyReportScreen(context: Context, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var report by remember { mutableStateOf<UsageReport?>(null) }

    LaunchedEffect(Unit) {
        report = runCatching {
            ReportEngine.build(
                history = UsageSnapshotStore(context).loadHistory(),
                alerts = AlertStore(context).load(),
                period = ReportPeriod.DAILY
            )
        }.getOrNull()
    }

    LazyColumn(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Informe diario", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onBack) { Text("Volver") }
            }
        }

        val data = report
        if (data == null) {
            item { Text("Preparando informe con los datos locales disponibles…") }
        } else {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Resumen", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Periodo: ${data.startDate}")
                        Text("Uso total: ${data.totalMinutes} min")
                        Text("Media diaria: ${data.averageDailyMinutes} min")
                        Text("Alertas: ${data.alertCount} · importantes: ${data.importantAlertCount}")
                        data.trendPercent?.let { Text("Tendencia: ${if (it >= 0) "+" else ""}$it%") }
                    }
                }
            }
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Lectura del periodo", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(data.narrative)
                        data.peakDate?.let {
                            Spacer(Modifier.height(6.dp))
                            Text("Pico de uso: $it · ${data.peakMinutes} min")
                        }
                    }
                }
            }
            if (data.topApps.isNotEmpty()) {
                item { Text("Aplicaciones con más uso", style = MaterialTheme.typography.titleMedium) }
                items(data.topApps.take(10), key = { it.packageName }) { app ->
                    Text("${app.label}: ${app.totalMinutes} min")
                }
            }
            item {
                Text("Este informe representa únicamente las señales disponibles localmente. La ausencia de datos no implica que todo esté bien.")
            }
        }
    }
}
