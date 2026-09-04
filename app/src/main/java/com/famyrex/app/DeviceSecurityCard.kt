package com.famyrex.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable

@Composable
fun DeviceSecurityCard(snapshot: DeviceSecuritySnapshot) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Seguridad del dispositivo")
            Text("Nivel: ${snapshot.securityLevel.name}")
            Text("Android ${snapshot.androidVersion} (API ${snapshot.sdkInt})")
            Text("Bloqueo seguro: ${if (snapshot.hasSecureLockScreen) "sí" else "no"}")
            Text("Acceso de uso: ${if (snapshot.usageAccessGranted) "sí" else "no"}")
            Text("Apps visibles: ${snapshot.installedAppCount}")
            if (snapshot.isDeveloperOptionsEnabled == null) {
                Text("Opciones de desarrollador: no determinable por una app normal")
            }
            snapshot.reasons.take(4).forEach { Text("• $it") }
        }
    }
}
