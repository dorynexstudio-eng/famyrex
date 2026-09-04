package com.famyrex.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WellbeingCard(assessment: WellbeingAssessment) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Bienestar digital", style = MaterialTheme.typography.titleLarge)
            Text("${assessment.todayMinutes} min de ${assessment.goalMinutes} min")
            LinearProgressIndicator(
                progress = { assessment.goalProgress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Progreso del objetivo: ${assessment.goalProgress}%")
            Text("Pausas largas detectadas: ${assessment.breakCount}")
            Text("Uso nocturno: ${assessment.nightMinutes} min")
            Text(assessment.recommendation)
        }
    }
}
