package com.famyrex.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
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
fun DeviceSecurityCard(context: android.content.Context, modifier: Modifier = Modifier) {
    var snapshot by remember { mutableStateOf<DeviceSecuritySnapshot?>(DeviceSecurityStore(context).load()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun refresh() {
        snapshot = DeviceSecurityStore(context).load()
    }

    LaunchedEffect(Unit) { refresh() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Seguridad del dispositivo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val current = snapshot
            if (current == null) {
                Text("⚪ SIN DATOS SUFICIENTES", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text("Todavía no existe una comprobación de seguridad guardada. Esto no significa que el dispositivo esté seguro.")
            } else {
                val symbol = when (current.securityLevel) {
                    SecurityLevel.GOOD -> "🟢"
                    SecurityLevel.ATTENTION -> "🟠"
                    SecurityLevel.ELEVATED -> "🔴"
                }
                Text("$symbol ${current.securityLevel.name}", style = MaterialTheme.typography.titleLarge)
                Text("Android ${current.androidVersion} (API ${current.sdkInt})")
                Text("Bloqueo seguro: ${if (current.hasSecureLockScreen) "sí" else "no"}")
                Text("Acceso de uso: ${if (current.usageAccessGranted) "sí" else "no"}")
                Text("Apps visibles: ${current.installedAppCount}")
                if (current.isDeveloperOptionsEnabled == null) {
                    Text("Opciones de desarrollador: no determinable por una app normal")
                }
                current.reasons.take(4).forEach { Text("• $it") }
            }
        }
    }
}
