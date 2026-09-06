package com.famyrex.app

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
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
fun AccessibilityConsentScreen(
    context: Context,
    onAccepted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var accepted by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Supervisión de aplicaciones", style = MaterialTheme.typography.headlineMedium)
        Text("Antes de activar esta función, debes aceptar esta explicación.")

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("¿Qué hace Famyrex?")
                Text("El servicio de accesibilidad observa qué aplicación está en primer plano para aplicar las reglas familiares configuradas en este dispositivo.")
                Text("Cuando una aplicación está restringida, Famyrex puede mostrar una pantalla visible indicando que existe un límite o una pausa.")
                Text("Famyrex no usa este servicio para leer, guardar o transmitir el contenido de chats, contraseñas o mensajes privados.")
                Text("La función es opcional y solo se activa después de que tú la habilites en los Ajustes de accesibilidad de Android.")
            }
        }

        Button(
            onClick = {
                AccessibilityConsentStore(context).accept()
                accepted = true
                onAccepted()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Acepto y continuar")
        }

        if (accepted) {
            Text("Consentimiento registrado. Ahora puedes activar la supervisión en Android.")
        }
    }
}

fun openAccessibilitySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
