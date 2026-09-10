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
                    Text("Última actualización: 10 de septiembre de 2026", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Famyrex está diseñado para proteger, explicar y acompañar, no para espiar. " +
                            "El procesamiento funcional es principalmente local, pero algunas funciones de familia conectada utilizan Firebase/Firestore para sincronizar el estado estrictamente necesario entre dispositivos autorizados.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    PrivacySection("Qué puede procesar", "Según las funciones activadas, Famyrex puede procesar localmente uso de aplicaciones, alertas, configuración parental, vinculación familiar, geozonas, informes y señales derivadas de notificaciones cuando el análisis de comunicación está expresamente activado.")
                    PrivacySection("Familia conectada", "Las funciones de vinculación y supervisión entre dispositivos utilizan Firebase/Firestore. Cuando la ubicación familiar conectada está activa, puede sincronizarse la última ubicación del dispositivo infantil: latitud, longitud, precisión aproximada y momento de captura. Famyrex conserva en remoto únicamente el último punto necesario para esta función, no un historial remoto de recorridos.")
                    PrivacySection("Qué no hace", "No utiliza vigilancia oculta, no lee contraseñas, no realiza lectura secreta de chats o mensajes, no utiliza micrófono ni cámara para vigilancia y no vende datos personales ni usa publicidad personalizada.")
                    PrivacySection("Ubicación", "Las geozonas requieren permiso de ubicación. La ubicación sincronizada entre dispositivos requiere además la función familiar correspondiente y permisos de Android. Android y Google Play Services pueden intervenir en la obtención de la ubicación y el geofencing.")
                    PrivacySection("Seguridad web", "El navegador integrado puede cargar páginas de Internet y utilizar Safe Browsing para amenazas conocidas. Famyrex no intercepta el navegador externo ni los mensajes de otras aplicaciones.")
                    PrivacySection("Conservación y eliminación", "Los datos funcionales locales permanecen mientras sean necesarios o hasta que el usuario borre los datos de Famyrex o desinstale la aplicación. La ubicación conectada se limita al último punto sincronizado y se sustituye al publicar uno nuevo. Los datos de infraestructura de Firebase pueden estar sujetos a sus propias políticas y retenciones técnicas.")
                    PrivacySection("Permisos y seguridad", "Famyrex solicita permisos solo para funciones que los necesitan y el usuario puede revocarlos desde Android. Los secretos de vinculación se protegen mediante Android Keystore. Las alertas son señales para revisar el contexto y no diagnósticos ni acusaciones.")
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
