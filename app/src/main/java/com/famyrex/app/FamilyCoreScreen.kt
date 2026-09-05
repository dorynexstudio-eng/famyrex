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
import androidx.compose.material3.HorizontalDivider
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
fun FamilyCoreScreen(context: Context, modifier: Modifier = Modifier) {
    val store = remember { FamilyStore(context) }
    var profiles by remember { mutableStateOf(store.profiles()) }
    var devices by remember { mutableStateOf(store.devices()) }
    var adultName by remember { mutableStateOf("") }
    var childName by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        store.ensureLocalOwner()
        profiles = store.profiles()
        devices = store.devices()
    }

    val adults = profiles.filter { it.role == FamilyRole.OWNER || it.role == FamilyRole.ADULT }
    val children = profiles.filter { it.role == FamilyRole.CHILD }

    LazyColumn(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Familia Famyrex", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.padding(2.dp))
            Text("Un grupo puede tener varios adultos autorizados y varios perfiles infantiles. Cada dispositivo supervisado pertenece a un perfil infantil.")
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Adultos autorizados", style = MaterialTheme.typography.titleMedium)
                    if (adults.isEmpty()) Text("⚪ Sin adultos configurados")
                    adults.forEach { adult ->
                        Text("👑 ${adult.displayName} · ${if (adult.role == FamilyRole.OWNER) "Administrador" else "Adulto autorizado"}")
                    }
                    OutlinedTextField(adultName, { adultName = it }, Modifier.fillMaxWidth(), label = { Text("Nombre del segundo padre/madre") })
                    Button(
                        enabled = adultName.isNotBlank(),
                        onClick = {
                            store.addAdult(adultName.trim())
                            adultName = ""
                            profiles = store.profiles()
                            message = "Adulto autorizado añadido."
                        },
                        Modifier.fillMaxWidth()
                    ) { Text("Añadir adulto") }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Perfiles infantiles", style = MaterialTheme.typography.titleMedium)
                    if (children.isEmpty()) Text("⚪ Sin perfiles infantiles")
                    children.forEach { child ->
                        val guardians = child.guardianProfileIds.mapNotNull { id -> profiles.firstOrNull { it.id == id }?.displayName }
                        Text("🧒 ${child.displayName} · Adultos: ${guardians.ifEmpty { listOf("sin asignar") }.joinToString()}")
                    }
                    OutlinedTextField(childName, { childName = it }, Modifier.fillMaxWidth(), label = { Text("Nombre del hijo/a") })
                    Button(
                        enabled = childName.isNotBlank() && adults.isNotEmpty(),
                        onClick = {
                            store.addChild(childName.trim(), adults.map { it.id })
                            childName = ""
                            profiles = store.profiles()
                            message = "Perfil infantil creado y vinculado a los adultos autorizados."
                        },
                        Modifier.fillMaxWidth()
                    ) { Text("Añadir hijo/a") }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dispositivos supervisados", style = MaterialTheme.typography.titleMedium)
                    if (devices.isEmpty()) Text("⚪ Ningún dispositivo vinculado")
                    devices.forEach { device ->
                        val child = profiles.firstOrNull { it.id == device.ownerProfileId }
                        val state = when (device.linkState) {
                            DeviceLinkState.LINKED -> "🟢 VINCULADO"
                            DeviceLinkState.PENDING -> "🟠 PENDIENTE"
                            DeviceLinkState.UNLINKED -> "⚪ SIN VINCULAR"
                        }
                        Text("📱 ${device.displayName} · ${child?.displayName ?: "perfil no encontrado"} · $state")
                    }
                    if (children.isNotEmpty()) {
                        OutlinedTextField(deviceName, { deviceName = it }, Modifier.fillMaxWidth(), label = { Text("Nombre del dispositivo") })
                        Button(
                            enabled = deviceName.isNotBlank(),
                            onClick = {
                                store.addDevice(deviceName.trim(), children.first().id)
                                deviceName = ""
                                devices = store.devices()
                                message = "Dispositivo preparado para vincular con ${children.first().displayName}."
                            },
                            Modifier.fillMaxWidth()
                        ) { Text("Preparar vinculación") }
                    }
                }
            }
        }

        item {
            HorizontalDivider()
            Text("Modo de esta instalación", style = MaterialTheme.typography.titleMedium)
            Text("El modo supervisado está pensado para el dispositivo del menor: interfaz mínima y funciones de protección, sin panel de administración.")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { store.setAppMode(FamyrexAppMode.PARENT); message = "Esta instalación queda marcada como dispositivo de padres." }, Modifier.weight(1f)) { Text("Padres") }
                OutlinedButton(onClick = { store.setAppMode(FamyrexAppMode.SUPERVISED); message = "Esta instalación queda marcada como dispositivo supervisado." }, Modifier.weight(1f)) { Text("Supervisado") }
            }
            Text("Modo actual: ${if (store.appMode() == FamyrexAppMode.PARENT) "PARENT" else "SUPERVISED"}")
        }

        if (message.isNotBlank()) item { Text(message) }

        item {
            Text("Protección y transparencia", style = MaterialTheme.typography.titleMedium)
            Text("La vinculación debe hacerse con autorización y permisos visibles. El modo supervisado no es una aplicación espía: no lee chats privados, no graba llamadas y no oculta la supervisión de forma clandestina.")
        }
    }
}
