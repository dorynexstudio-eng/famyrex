package com.famyrex.app

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

enum class WellbeingStatus {
    UNKNOWN,
    OK,
    ATTENTION,
    CONCERN
}

data class WellbeingCardState(
    val assessment: WellbeingAssessment?,
    val trend: WellbeingTrendEngine.Assessment?,
    val status: WellbeingStatus,
    val dataDays: Int
)

@Composable
fun WellbeingCard(context: Context) {
    var state by remember { mutableStateOf(WellbeingCardState(null, null, WellbeingStatus.UNKNOWN, 0)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    suspend fun refresh() {
        state = withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone).toString()
            val settings = WellbeingSettingsStore(context).load()
            val history = UsageSnapshotStore(context).loadHistory()
            val todayFromHistory = history.firstOrNull { it.date == today }
            val todayUsage = if (todayFromHistory != null) {
                todayFromHistory.totalTimeMs
            } else {
                UsageRepository.loadToday(context).sumOf { it.totalTimeMs }
            }

            val intervals = UsageIntervalStore(context).load(today)
            val assessment = if (todayUsage > 0L || intervals.isNotEmpty()) {
                WellbeingEngine.evaluate(todayUsage / 60_000L, intervals, settings)
            } else {
                null
            }
            val trend = WellbeingTrendEngine.evaluate(history)

            val status = when {
                assessment == null -> WellbeingStatus.UNKNOWN
                assessment.todayMinutes >= assessment.goalMinutes * 1.25 -> WellbeingStatus.CONCERN
                trend?.score ?: 0 >= 70 -> WellbeingStatus.CONCERN
                assessment.nightMinutes >= 30L -> WellbeingStatus.ATTENTION
                assessment.todayMinutes >= assessment.goalMinutes -> WellbeingStatus.ATTENTION
                assessment.breakCount == 0 && intervals.size >= 4 -> WellbeingStatus.ATTENTION
                trend?.score ?: 0 >= 35 -> WellbeingStatus.ATTENTION
                else -> WellbeingStatus.OK
            }

            WellbeingCardState(assessment, trend, status, history.size)
        }
    }

    LaunchedEffect(Unit) { refresh() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                kotlinx.coroutines.MainScope().launch { refresh() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Bienestar digital", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Text(statusLabel(state.status), style = MaterialTheme.typography.labelLarge)
            }

            val assessment = state.assessment
            if (assessment == null) {
                Text("⚪ SIN DATOS SUFICIENTES", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Todavía no hay datos de uso suficientes para valorar el bienestar digital. Esto no significa que todo esté bien."
                )
            } else {
                Text("${assessment.todayMinutes} min de ${assessment.goalMinutes} min")
                LinearProgressIndicator(
                    progress = { assessment.goalProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Objetivo diario: ${assessment.goalProgress}%")
                Text("Pausas largas detectadas: ${assessment.breakCount}")
                Text("Uso nocturno: ${assessment.nightMinutes} min")
                Text(assessment.recommendation)

                val trend = state.trend
                if (trend != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("Tendencia: ${trend.title}", style = MaterialTheme.typography.titleSmall)
                    Text("Media reciente: ${trend.averageMinutes} min/día · ${trend.sustainedDays} días elevados")
                } else {
                    Text("Tendencia semanal: ⚪ aún no hay 7 días de datos")
                }
            }
        }
    }
}

private fun statusLabel(status: WellbeingStatus): String = when (status) {
    WellbeingStatus.UNKNOWN -> "⚪ SIN DATOS"
    WellbeingStatus.OK -> "🟢 EQUILIBRADO"
    WellbeingStatus.ATTENTION -> "🟠 ATENCIÓN"
    WellbeingStatus.CONCERN -> "🔴 SEÑALES DE ATENCIÓN"
}
