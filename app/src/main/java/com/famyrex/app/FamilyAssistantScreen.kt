package com.famyrex.app

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FamilyAssistantScreen(context: Context, modifier: Modifier = Modifier) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("Soy el asistente local de Famyrex. Pregúntame por el uso, alertas, tendencias o bienestar.") }

    LazyColumn(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Asistente familiar", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall) }
        item { Text("Consulta únicamente la información que Famyrex ha registrado de forma autorizada.") }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Famyrex")
                    Text(answer)
                }
            }
        }
        item {
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Pregunta") },
                placeholder = { Text("¿Cuánto se ha usado hoy?") }
            )
        }
        item {
            Button(
                onClick = { answer = FamilyAssistantEngine.answer(context, question) },
                enabled = question.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Preguntar") }
        }
        item { Text("Privacidad: el asistente no lee chats privados ni intenta deducir emociones o diagnósticos.") }
    }
}
