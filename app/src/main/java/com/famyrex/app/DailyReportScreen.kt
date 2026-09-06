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

/** Local family report: daily facts plus the weekly summary that gives the family a reason to return. */
@Composable
fun DailyReportScreen(context: Context, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var report by remember { mutableStateOf<UsageReport?>(null) }
    var weekly by remember { mutableStateOf<UsageReport?>(null) }
    var agreementStatus by remember { mutableStateOf<AgreementStatus?>(null) }

    LaunchedEffect(Unit) {
        val history = UsageSnapshotStore(context).loadHistory()
        val alerts = AlertStore(context).load()
        report = runCatching { ReportEngine.build(history, alerts, ReportPeriod.DAILY) }.getOrNull()
        weekly = runCatching { ReportEngine.build(history, alerts, ReportPeriod.WEEKLY) }.getOrNull()
        val agreement = FamilyAgreementStore(context).load()
        val usage = ParentalUsageMonitor(context)
        val start = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val minutes = if (usage.hasUsageAccess()) {
            usage.queryUsage(start, System.currentTimeMillis()).sumOf { it.totalTimeInForeground } / 60_000L
        } else null
        agreementStatus = FamilyAgreementEngine.evaluate(agreement, minutes)
    }

    LazyColumn(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Informe familiar", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onBack) { Text("Volver") }
            }
        }

        val data = report
        val week = weekly
        if (data == null || week == null) {
            item { Text("Preparando el resumen con los datos locales disponibles…") }
        } else {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Resumen de hoy", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Uso total: ${data.totalMinutes} min")
                        Text("Alertas: ${data.alertCount} · importantes: ${data.importantAlertCount}")
                        data.trendPercent?.let { Text("Tendencia: ${if (it >= 0) "+" else ""}$it%") }
                    }
                }
            }
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Resumen de la semana", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("${week.startDate} → ${week.endDate}")
                        Text("Uso total: ${week.totalMinutes} min")
                        Text("Media diaria: ${week.averageDailyMinutes} min")
                        Text("Alertas: ${week.alertCount} · importantes: ${week.importantAlertCount}")
                        Spacer(Modifier.height(6.dp))
                        Text(week.narrative)
                    }
                }
            }
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Acuerdo familiar", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        when (agreementStatus?.state) {
                            AgreementState.ON_TRACK -> Text("🟢 El uso de hoy va dentro de lo acordado.")
                            AgreementState.ATTENTION -> Text("🟠 El uso de hoy se acerca al límite acordado.")
                            AgreementState.EXCEEDED -> Text("🟠 Hoy se ha superado el límite pactado. La familia decide qué hacer según el acuerdo.")
                            AgreementState.INSUFFICIENT_DATA, null -> Text("⚪ No hay datos suficientes para valorar el cumplimiento.")
                        }
                    }
                }
            }
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Lectura del día", style = MaterialTheme.typography.titleMedium)
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
                Text("Famyrex muestra hechos y tendencias a partir de los datos disponibles; no convierte la ausencia de datos en una garantía de seguridad.")
            }
        }
    }
}
