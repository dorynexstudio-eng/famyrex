package com.famyrex.app

import android.content.Context
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
import kotlinx.coroutines.delay

private const val INVENTORY_STALE_MS = 72L * 60L * 60L * 1000L

@Composable
fun FamilyRemoteControlScreen(
    context: Context,
    familyId: String?,
    modifier: Modifier = Modifier
) {
    val repository = remember { FamilyCloudControlRepository(context) }
    val policySyncService = remember { FamilyPolicySyncService(context) }
    val receiptStore = remember { RemoteCommandReceiptStore(context) }
    var children by remember { mutableStateOf(emptyList<CloudChildDevice>()) }
    var selectedUid by remember { mutableStateOf<String?>(null) }
    var inventory by remember { mutableStateOf<ChildAppInventoryState?>(null) }
    var deviceStatus by remember { mutableStateOf<ChildDeviceStatus?>(null) }
    var loading by remember { mutableStateOf(false) }
    var inventoryLoading by remember { mutableStateOf(false) }
    var statusLoading by remember { mutableStateOf(false) }
    var policySyncLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var extraMinutes by remember { mutableStateOf("30") }
    var dailyLimit by remember { mutableStateOf("120") }
    var appLimit by remember { mutableStateOf("60") }
    var scheduleStart by remember { mutableStateOf("22:00") }
    var scheduleEnd by remember { mutableStateOf("07:00") }
    var selectedPackage by remember { mutableStateOf<String?>(null) }
    var lastCommandId by remember { mutableStateOf<String?>(null) }
    var cloudReceipt by remember { mutableStateOf<CloudCommandReceipt?>(null) }
    var lastReceipt by remember { mutableStateOf(receiptStore.load()) }

    fun refreshChildren() {
        val id = familyId?.takeIf { it.isNotBlank() } ?: run {
            children = emptyList()
            inventory = null
            deviceStatus = null
            message = "La familia cloud todavía no está disponible en esta instalación."
            return
        }
        loading = true
        repository.loadChildren(id, { result ->
            children = result
            val next = if (selectedUid != null && result.any { it.uid == selectedUid }) selectedUid else result.firstOrNull()?.uid
            selectedUid = next
            loading = false
        }, { error ->
            loading = false
            message = error
        })
    }

    fun refreshInventory(child: CloudChildDevice?) {
        val id = familyId?.takeIf { it.isNotBlank() } ?: return
        if (child == null) {
            inventory = null
            selectedPackage = null
            return
        }
        inventoryLoading = true
        selectedPackage = null
        repository.loadChildAppInventory(id, child, { state ->
            inventory = state
            inventoryLoading = false
        }, { error ->
            inventory = null
            inventoryLoading = false
            message = error
        })
    }

    fun refreshStatus(child: CloudChildDevice?) {
        val id = familyId?.takeIf { it.isNotBlank() } ?: return
        if (child == null) {
            deviceStatus = null
            return
        }
        statusLoading = true
        repository.loadChildDeviceStatus(id, child, { status ->
            deviceStatus = status
            statusLoading = false
        }, { error ->
            statusLoading = false
            message = error
        })
    }

    fun syncPolicy(child: CloudChildDevice?) {
        val id = familyId?.takeIf { it.isNotBlank() } ?: return
        if (child == null) {
            message = "Selecciona primero un dispositivo infantil."
            return
        }
        policySyncLoading = true
        policySyncService.syncCurrentPolicy(id, child, { commandId ->
            policySyncLoading = false
            lastCommandId = commandId
            cloudReceipt = null
            message = "Política completa enviada. Esperando confirmación del dispositivo…"
        }, { error ->
            policySyncLoading = false
            message = error
        })
    }

    LaunchedEffect(familyId) { refreshChildren() }
    LaunchedEffect(selectedUid, children) {
        val child = children.firstOrNull { it.uid == selectedUid }
        refreshInventory(child)
        refreshStatus(child)
    }
    LaunchedEffect(selectedUid, familyId) {
        if (selectedUid == null || familyId.isNullOrBlank()) return@LaunchedEffect
        while (true) {
            refreshStatus(children.firstOrNull { it.uid == selectedUid })
            delay(60_000L)
        }
    }
    LaunchedEffect(lastCommandId, selectedUid, familyId) {
        val commandId = lastCommandId ?: return@LaunchedEffect
        val id = familyId ?: return@LaunchedEffect
        val child = children.firstOrNull { it.uid == selectedUid } ?: return@LaunchedEffect
        delay(1500)
        repository.loadCommandReceipt(id, commandId, child.deviceId, { receipt ->
            cloudReceipt = receipt
            message = when {
                receipt == null -> "Orden enviada. Esperando confirmación del dispositivo…"
                receipt.success -> "Orden ejecutada correctamente en ${child.displayName}."
                else -> "Orden rechazada en ${child.displayName}: ${receipt.reason ?: "sin motivo indicado"}"
            }
        }, { error -> message = error })
    }

    val selected = children.firstOrNull { it.uid == selectedUid }
    val selectedApp = inventory?.apps?.firstOrNull { it.packageName == selectedPackage }
    val inventoryAge = inventory?.updatedAtMs?.let { (System.currentTimeMillis() - it).coerceAtLeast(0L) }
    val inventoryIsStale = inventory?.updatedAtMs?.let { it <= 0L || System.currentTimeMillis() - it > INVENTORY_STALE_MS } == true

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
        cloudReceipt = null
        repository.issueCommand(id, child, action, value, { commandId ->
            loading = false
            lastCommandId = commandId
            message = "Orden ${action.name} enviada. Esperando confirmación…"
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
                            OutlinedButton(onClick = { selectedUid = child.uid }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (child.uid == selectedUid) "✓ ${child.displayName}" else child.displayName)
                            }
                        }
                        selected?.let {
                            Text("Perfil: ${it.displayName}")
                            Text("Device ID: ${it.deviceId}")
                        }
                        OutlinedButton(onClick = { refreshChildren() }, modifier = Modifier.fillMaxWidth()) { Text("Actualizar dispositivos") }
                    }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Estado del dispositivo", style = MaterialTheme.typography.titleMedium)
                    when {
                        selected == null -> Text("⚪ Selecciona un dispositivo infantil.")
                        statusLoading && deviceStatus == null -> Text("🟠 Consultando estado…")
                        deviceStatus == null -> Text("⚪ El dispositivo todavía no ha comunicado su estado.")
                        else -> {
                            val status = deviceStatus!!
                            when (status.liveness()) {
                                ChildDeviceLiveness.ONLINE -> Text("🟢 Conectado recientemente")
                                ChildDeviceLiveness.OFFLINE -> Text("🟠 Sin comunicación reciente")
                                ChildDeviceLiveness.UNKNOWN -> Text("⚪ Estado de conexión desconocido")
                            }
                            Text("Última comunicación: ${formatDeviceAge(status.updatedAtMs)}")
                            when (status.protectionActive) {
                                true -> Text("🟢 Protección local activa")
                                false -> Text("🔴 Protección local degradada")
                                null -> Text("⚪ Salud de protección no disponible")
                            }
                            if (status.protectionReasons.isNotEmpty()) {
                                status.protectionReasons.forEach { reason -> Text("• $reason") }
                            }
                            if (status.protectionCheckedAtMs > 0L) {
                                Text("Última comprobación de protección: ${formatDeviceAge(status.protectionCheckedAtMs)}")
                            }
                            OutlinedButton(enabled = !statusLoading, onClick = { refreshStatus(selected) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Actualizar estado")
                            }
                            Text("El estado de conexión es una señal de comunicación, no una garantía de disponibilidad continua.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sincronización de política", style = MaterialTheme.typography.titleMedium)
                    Text("Envía al dispositivo infantil la política parental completa actualmente almacenada en este dispositivo.")
                    Button(enabled = !policySyncLoading && selected != null, onClick = { syncPolicy(selected) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (policySyncLoading) "Sincronizando…" else "Sincronizar política completa")
                    }
                    Text("La sincronización se valida por familia, perfil y dispositivo y conserva protección contra revisiones repetidas.", style = MaterialTheme.typography.bodySmall)
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
                    Text("Aplicaciones del dispositivo infantil", style = MaterialTheme.typography.titleMedium)
                    when {
                        selected == null -> Text("⚪ Selecciona primero un dispositivo infantil.")
                        inventoryLoading -> Text("🟠 Consultando inventario del dispositivo…")
                        inventory == null -> Text("⚪ El inventario todavía no está disponible.")
                        inventory!!.apps.isEmpty() -> Text("⚪ El dispositivo no ha comunicado aplicaciones iniciables.")
                        inventoryIsStale -> Text("🟠 Inventario desactualizado. Última actualización: ${formatInventoryAge(inventoryAge)}. Actualiza el dispositivo infantil antes de tomar decisiones sobre una app.")
                        else -> {
                            Text("🟢 Inventario recibido del dispositivo infantil · ${inventory!!.apps.size} apps")
                            inventory!!.apps.take(50).forEach { app ->
                                OutlinedButton(onClick = { selectedPackage = app.packageName }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (app.packageName == selectedPackage) "✓ ${app.label}" else app.label)
                                }
                            }
                        }
                    }
                    OutlinedButton(enabled = selected != null && !inventoryLoading, onClick = { refreshInventory(selected) }, modifier = Modifier.fillMaxWidth()) { Text("Actualizar inventario") }
                    if (!inventoryIsStale) {
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
                    Text("Estado de la última orden", style = MaterialTheme.typography.titleMedium)
                    cloudReceipt?.let { receipt ->
                        Text(if (receipt.success) "🟢 Ejecutada" else "🔴 Rechazada")
                        Text("${receipt.action.name} · ${receipt.commandId.take(8)}…")
                        receipt.reason?.let { Text("Motivo: $it") }
                    } ?: lastCommandId?.let {
                        Text("🟠 Enviada · ${it.take(8)}…")
                        Text("El dispositivo todavía no ha confirmado la ejecución.")
                    } ?: Text("⚪ No hay una orden remota reciente en esta sesión.")
                }
            }
        }

        item { if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodyMedium) }
    }
}

private fun formatDeviceAge(timestampMs: Long): String {
    if (timestampMs <= 0L) return "sin datos"
    val age = (System.currentTimeMillis() - timestampMs).coerceAtLeast(0L)
    val minutes = age / 60_000L
    return when {
        minutes < 1L -> "hace menos de 1 minuto"
        minutes < 60L -> "hace $minutes min"
        minutes < 1440L -> "hace ${minutes / 60L} h"
        else -> "hace ${minutes / 1440L} d"
    }
}

private fun formatInventoryAge(ageMs: Long?): String {
    if (ageMs == null || ageMs < 0L) return "sin datos"
    val minutes = ageMs / 60_000L
    return when {
        minutes < 60L -> "$minutes min"
        minutes < 1440L -> "${minutes / 60L} h"
        else -> "${minutes / 1440L} d"
    }
}
