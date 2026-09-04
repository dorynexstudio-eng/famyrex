package com.famyrex.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable

@Composable
fun ReportCard(report: UsageReport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                when (report.period) {
                    ReportPeriod.DAILY -> "Informe diario"
                    ReportPeriod.WEEKLY -> "Informe semanal"
                    ReportPeriod.MONTHLY -> "Informe mensual"
                }
            )
            Text("${report.startDate} → ${report.endDate}")
            Text("Uso total: ${report.totalMinutes} min")
            Text("Promedio diario: ${report.averageDailyMinutes} min")
            report.peakDate?.let {
                Text("Día de mayor uso: $it (${report.peakMinutes} min)")
            }
            report.trendPercent?.let {
                Text("Variación frente al periodo anterior: ${if (it >= 0) "+" else ""}$it%")
            }
            Text("Alertas: ${report.alertCount} · Importantes: ${report.importantAlertCount}")
            Text(report.narrative)
            if (report.topApps.isNotEmpty()) {
                Text("Aplicaciones con más uso:")
                report.topApps.take(5).forEach {
                    Text("• ${it.label}: ${it.totalMinutes} min")
                }
            }
        }
    }
}
