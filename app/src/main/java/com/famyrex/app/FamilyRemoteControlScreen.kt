package com.famyrex.app

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FamilyRemoteControlScreen(
    context: Context,
    familyId: String?,
    modifier: Modifier = Modifier
) {
    val repository = remember { FamilyCloudControlRepository(context) }
    val receiptStore = remember { RemoteCommandReceiptStore(context) }
    val installedApps = remember { loadLaunchableApps(context) }
    var children by remember { mutableStateOf(emptyList<CloudChildDevice>()) }
    var selectedUid by remember { mutableStateOf<String?>(null) }
    var selectedPackage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var extraMinutes by remember { mutableStateOf("30") }
    var dailyLimit by remember { mutableStateOf("120") }
    var appLimit by remember { mutableStateOf("60") }
    var scheduleStart by remember { mutableStateOf("22:00") }
    var scheduleEnd by remember { mutableStateOf("07:00") }
    var lastReceipt by remember { mutableStateOf(receiptStore.load()) }

    fun refresh() {
        val id = familyId?.takeIf { it.isNotBlank() } ?: run {
            children = emptyList()
            message = "La familia cloud todavía no está disponible en esta instalación."
            return
        }
        loading = true
        repository.loadChildren(id, { result ->
            children = result
            if (selectedUid == null || result.none { it.uid == selectedUid }) selectedUid = result.firstOrNull()?.uid
            loading = false
        }, { error ->
            loading = false
            message = error
        })
    }

    LaunchedEffect(familyId) { refresh() }
    val selected = children.firstOrNull { it.uid == selectedUid }
    val selectedApp = installedApps.firstOrNull { it.packageName == selectedPackage }

    fun issue(action: FamilyControlAction, value: String? = null) {
        val id = familyId?.takeIf { it.isNotBlank() } ?: return
        val child = selected ?: run {
            message = "Selecciona primero un dispositivo infantil."
            return
        }
        if (!RemoteControlActionPolicy.isSupported(action)) {
            message = "Esta acción todavía no está disponible en el dispositivo supervisado."
            return
        }
        loading = true
        repository.issueCommand(id, child, action, value, { commandId ->
            loading = false
            message = "Orden ${action.name} enviada. ID: ${commandId.take(8)}…"
            lastReceipt = receiptStore.load()
        }, { error ->
            loading = false
            message = error
        })
    }

    fun clockToMinutes(value: String): Int? {
        val parts = value.split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
    }

    LazyColumn(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Control remoto familiar", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.padding(2.dp))
            Text("Control autorizado sobre dispositivos vinculados. Cada orden se valida por familia, perfil y dispositivo antes de ejecutarse.")
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dispositivo infantil", style = MaterialTheme.typography.titleMedium)
                    if (children.isEmpty()) {
                        Text(if (loading) "Cargando dispositivos…" else "⚪ No hay dispositivos cloud disponibles.")
                    } else {
                        children.forEach { child ->
                            OutlinedButton(
                                onClick = { selectedUid = child.uid },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (child.uid == selectedUid) "✓ ${child.displayName}" else child.displayName)
                            }
                        }
                        selected?.let {
                            Text("Perfil: ${it.displayName}")
                            Text("Device ID: ${it.deviceId}")
                        }
                        OutlinedButton(onClick = { refresh() }, modifier = Modifier.fillMaxWidth()) { Text("Actualizar dispositivos") }
                    }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Acciones inmediatas", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !loading && selected != null, onClick = { issue(FamilyControlAction.LOCK_DEVICE) }, modifier = Modifier.weight(1f)) { Text("Bloquear") }
                        OutlinedButton(enabled = !loading && selected != null, onClick = { issue(FamilyControlAction.UNLOCK_DEVICE) }, modifier = Modifier.weight(1f)) { Text("Desbloquear") }
                    }
                    OutlinedTextField(extraMinutes, { extraMinutes = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), label = { Text("Minutos extra") })
                    Button(enabled = !loading && selected != null && extraMinutes.toIntOrNull()?.let { it in 1..1440 } == true, onClick = { issue(FamilyControlAction.GRANT_EXTRA_TIME, extraMinutes) }, modifier = Modifier.fillMaxWidth()) { Text("Conceder tiempo extra") }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Límites y horario", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(dailyLimit, { dailyLimit = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), label = { Text("Límite diario (minutos)") })
                    Button(enabled = !loading && selected != null && dailyLimit.toIntOrNull()?.let { it in 1..1440 } == true, onClick = { issue(FamilyControlAction.SET_DAILY_LIMIT, dailyLimit) }, modifier = Modifier.fillMaxWidth()) { Text("Aplicar límite diario") }
                    OutlinedTextField(scheduleStart, { scheduleStart = it.take(5) }, Modifier.fillMaxWidth(), label = { Text("Inicio de pausa (HH:MM)") })
                    OutlinedTextField(scheduleEnd, { scheduleEnd = it.take(5) }, Modifier.fillMaxWidth(), label = { Text("Fin de pausa (HH:MM)") })
                    val start = clockToMinutes(scheduleStart)
                    val end = clockToMinutes(scheduleEnd)
                    Button(enabled = !loading && selected != null && start != null && end != null, onClick = { issue(FamilyControlAction.SET_SCHEDULE, "$start-$end") }, modifier = Modifier.fillMaxWidth()) { Text("Aplicar horario de pausa") }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aplicaciones supervisadas", style = MaterialTheme.typography.titleMedium)
                    Text("Selecciona una aplicación instalada en este panel para enviar su paquete Android al dispositivo vinculado.", style = MaterialTheme.typography.bodySmall)
                    if (installedApps.isEmpty()) {
                        Text("⚪ No se han encontrado aplicaciones iniciables.")
                    } else {
                        installedApps.take(30).forEach { app ->
                            OutlinedButton(onClick = { selectedPackage = app.packageName }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (app.packageName == selectedPackage) "✓ ${app.label}" else app.label)
                            }
                        }
                        selectedApp?.let {
                            Text("Aplicación: ${it.label}")
                            Text(it.packageName, style = MaterialTheme.typography.bodySmall)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(enabled = !loading && selected != null, onClick = { issue(FamilyControlAction.BLOCK_APP, it.packageName) }, modifier = Modifier.weight(1f)) { Text("Bloquear") }
                                OutlinedButton(enabled = !loading && selected != null, onClick = { issue(FamilyControlAction.ALLOW_APP, it.packageName) }, modifier = Modifier.weight(1f)) { Text("Permitir") }
                            }
                            OutlinedTextField(appLimit, { appLimit = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), label = { Text("Límite de esta app (minutos)") })
                            Button(enabled = !loading && selected != null && appLimit.toIntOrNull()?.let { it in 1..1440 } == true, onClick = { issue(FamilyControlAction.SET_APP_LIMIT, "${it.packageName}:$appLimit") }, modifier = Modifier.fillMaxWidth()) { Text("Aplicar límite a la app") }
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sincronización de política", style = MaterialTheme.typography.titleMedium)
                    Text("La política completa se sincronizará mediante SYNC_POLICY cuando exista un snapshot válido del dispositivo seleccionado.")
                    OutlinedButton(enabled = false, onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Sincronizar política completa") }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Último recibo local", style = MaterialTheme.typography.titleMedium)
                    val receipt = lastReceipt
                    if (receipt == null) {
                        Text("⚪ Todavía no hay una ejecución remota registrada en este dispositivo.")
                    } else {
                        Text(if (receipt.success) "🟢 ${receipt.action.name}" else "🔴 ${receipt.action.name}")
                        Text("Comando: ${receipt.commandId}")
                        receipt.reason?.let { Text("Motivo: $it") }
                    }
                }
            }
        }

        if (message.isNotBlank()) item { Text(message) }
    }
}

private data class InstalledApp(
    val packageName: String,
    val label: String
)

private fun loadLaunchableApps(context: Context): List<InstalledApp> = runCatching {
    val packageManager = context.packageManager
    packageManager.getInstalledApplications(0)
        .asSequence()
        .filter { it.packageName != context.packageName }
        .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
        .map { info: ApplicationInfo -> InstalledApp(info.packageName, info.loadLabel(packageManager).toString().ifBlank { info.packageName }) }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
        .toList()
}.getOrDefault(emptyList())
