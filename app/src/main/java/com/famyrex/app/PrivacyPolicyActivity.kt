package com.famyrex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text("Política de privacidad", style = MaterialTheme.typography.headlineSmall)
                    Text("Última actualización: 6 de septiembre de 2026", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Famyrex está diseñado para proteger, explicar y acompañar, no para espiar. " +
                            "El procesamiento funcional se realiza principalmente en el dispositivo.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    PrivacySection("Qué procesa", "Según las funciones activadas, Famyrex puede procesar localmente uso de aplicaciones, alertas, configuración parental, vinculación familiar, geozonas, informes y observaciones de notificaciones cuando el análisis de comunicación está expresamente activado.")
                    PrivacySection("Qué no hace", "No utiliza vigilancia oculta, publicidad personalizada, lectura secreta de chats ni un servidor propio para almacenar el historial familiar.")
                    PrivacySection("Ubicación", "Las geozonas requieren permiso de ubicación. Famyrex guarda localmente la configuración y los eventos funcionales; Android y Google Play Services pueden intervenir en la obtención de la ubicación y geofencing.")
                    PrivacySection("Seguridad web", "El navegador integrado puede cargar páginas de Internet y utilizar Safe Browsing para amenazas conocidas. Famyrex no intercepta el navegador externo ni los mensajes de otras aplicaciones.")
                    PrivacySection("Conservación", "Los datos funcionales permanecen en el dispositivo mientras sean necesarios o hasta que el usuario borre los datos de la aplicación o la desinstale. Esta versión no crea una cuenta remota de Famyrex.")
                    PrivacySection("Seguridad", "Los secretos de vinculación familiar se protegen mediante Android Keystore. Si faltan permisos o datos necesarios, Famyrex muestra el estado ⚪ Datos insuficientes en lugar de presentar una falsa sensación de seguridad.")
                    TextButton(onClick = { finish() }) { Text("Volver") }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun PrivacySection(title: String, body: String) {
    Column(modifier = Modifier.padding(top = 18.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, modifier = Modifier.padding(top = 5.dp))
    }
}
