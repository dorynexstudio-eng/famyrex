package com.famyrex.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AiSummaryCard(summary: AiDailySummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Análisis inteligente")
            Text(summary.headline)
            Text(summary.body)
            summary.insights.forEach { insight ->
                Text("• ${insight.title}: ${insight.summary}")
                Text("  Confianza del análisis: ${insight.confidence}%")
            }
            Text("El análisis usa señales de uso disponibles; no interpreta conversaciones privadas ni diagnostica estados psicológicos.")
        }
    }
}
