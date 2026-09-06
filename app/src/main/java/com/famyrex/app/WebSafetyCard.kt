package com.famyrex.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WebSafetyCard(settings: WebSafetySettings, context: Context? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            context?.startActivity(Intent(context, WebSafetyActivity::class.java))
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Seguridad web")
            Text("Protección: ${if (settings.enabled) "activa" else "desactivada"}")
            Text("Bloqueo de sitios peligrosos: ${if (settings.blockKnownThreats) "activo" else "desactivado"}")
            Text("Dominios bloqueados: ${settings.blockedDomains.size}")
            Text("Dominios permitidos: ${settings.allowedDomains.size}")
            Text("La clasificación de malware/phishing se delega a Safe Browsing cuando está disponible.")
            if (context != null) Text("Toca para abrir la protección web")
        }
    }
}
