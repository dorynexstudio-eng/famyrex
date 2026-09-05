package com.famyrex.app

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun RiskCard(context: Context, modifier: Modifier = Modifier) {
    var assessment by remember { mutableStateOf<RiskAssessment?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun refresh() {
        val alerts = AlertStore(context).load()
        val settings = ProtectionSettingsStore(context).load()
        assessment = if (alerts.isEmpty()) null else RiskEngine.evaluate(alerts, settings)
    }

    LaunchedEffect(Unit) { refresh() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Nivel de riesgo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            val current = assessment
            if (current == null) {
                Text("⚪ SIN DATOS SUFICIENTES", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text("Todavía no hay señales suficientes para calcular un nivel de riesgo. Esto no significa que todo esté bien.")
            } else {
                val symbol = when (current.level) {
                    RiskLevel.NORMAL -> "🟢"
                    RiskLevel.ATTENTION -> "🟠"
                    RiskLevel.ELEVATED, RiskLevel.IMPORTANT -> "🔴"
                }
                val label = when (current.level) {
                    RiskLevel.NORMAL -> "NORMAL"
                    RiskLevel.ATTENTION -> "ATENCIÓN"
                    RiskLevel.ELEVATED -> "ELEVADO"
                    RiskLevel.IMPORTANT -> "IMPORTANTE"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("$symbol $label", style = MaterialTheme.typography.headlineSmall)
                    Text("${current.score}/100", style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(Modifier.height(8.dp))
                if (current.reasons.isEmpty()) {
                    Text("No se han identificado señales destacables en las alertas disponibles.")
                } else {
                    Text("Motivos principales", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    current.reasons.forEach { reason ->
                        Text("• $reason")
                    }
                }
            }
        }
    }
}
