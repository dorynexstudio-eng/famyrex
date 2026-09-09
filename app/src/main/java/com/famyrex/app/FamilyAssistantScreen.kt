package com.famyrex.app

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
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
    var answer by remember { mutableStateOf("Soy el asistente local de Famyrex. Pregúntame por las funciones de la app, los datos disponibles o un problema familiar.") }

    fun ask(value: String) {
        question = value
        answer = FamilyAssistantEngine.answer(context, value)
        question = ""
    }

    LazyColumn(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Asistente familiar", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall) }
        item { Text("Puedo explicar qué hace Famyrex, consultar únicamente la información registrada de forma autorizada y ayudar a ordenar un conflicto sin decidir quién tiene razón.") }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Famyrex")
                    Text(answer)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Acciones rápidas")
                AssistChip(onClick = { ask("Tenemos un conflicto por el móvil. ¿Qué hacemos?") }, label = { Text("Tenemos un conflicto") })
                AssistChip(onClick = { ask("Creo que hay bullying. ¿Cómo puede detectarlo Famyrex?") }, label = { Text("Creo que hay acoso") })
                AssistChip(onClick = { ask("¿Qué funciones tiene Famyrex?") }, label = { Text("Ver funciones") })
                AssistChip(onClick = { ask("Quiero crear un acuerdo familiar") }, label = { Text("Crear un acuerdo") })
            }
        }
        item {
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Pregunta") },
                placeholder = { Text("¿Qué funciones tiene Famyrex?") }
            )
        }
        item {
            Button(
                onClick = { ask(question) },
                enabled = question.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Preguntar") }
        }
        item {
            Text("Privacidad: el asistente no lee automáticamente chats privados completos. El listener de notificaciones solo puede analizar el contenido que Android expone con el permiso correspondiente. Las señales de riesgo no se presentan como hechos confirmados.")
        }
    }
}
