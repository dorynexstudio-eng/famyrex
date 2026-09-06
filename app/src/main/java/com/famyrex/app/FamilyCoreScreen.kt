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
import java.time.LocalDate

@Composable
fun FamilyCoreScreen(
    context: Context,
    modifier: Modifier = Modifier,
    onOpenParentalControl: () -> Unit = {},
    onFamilyChanged: () -> Unit = {}
) {
    val store = remember { FamilyStore(context) }
    val identityStore = remember { FamilyIdentityStore(context) }
    val agreementStore = remember { FamilyAgreementStore(context) }
    var profiles by remember { mutableStateOf(store.profiles()) }
    var devices by remember { mutableStateOf(store.devices()) }
    var agreement by remember { mutableStateOf(agreementStore.load()) }
    var adultName by remember { mutableStateOf("") }
    var childName by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }
    var agreementMinutes by remember { mutableStateOf(agreement?.dailyMinutes?.toString() ?: "120") }
    var agreementGoal by remember { mutableStateOf(agreement?.goal ?: "Mantener un uso equilibrado") }
    var agreementConsequence by remember { mutableStateOf(agreement?.consequence ?: "Hablarlo juntos y aplicar lo pactado") }
    var agreementReviewDate by remember { mutableStateOf(agreement?.reviewDate ?: LocalDate.now().plusDays(30).toString()) }
    var invitation by remember { mutableStateOf<OfflinePairingToken?>(null) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        store.ensureLocalOwner()
        profiles = store.profiles()
        devices = store.devices()
        agreement = agreementStore.load()
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
                    adults.forEach { adult -> Text("👑 ${adult.displayName} · ${if (adult.role == FamilyRole.OWNER) "Administrador" else "Adulto autorizado"}") }
                    OutlinedTextField(adultName, { adultName = it }, Modifier.fillMaxWidth(), label = { Text("Nombre del segundo padre/madre") })
                    Button(enabled = adultName.isNotBlank(), onClick = {
                        store.addAdult(adultName.trim())
                        adultName = ""
                        profiles = store.profiles()
                        message = "Adulto autorizado añadido."
                        onFamilyChanged()
                    }, modifier = Modifier.fillMaxWidth()) { Text("Añadir adulto") }
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
                    Button(enabled = childName.isNotBlank() && adults.isNotEmpty(), onClick = {
                        store.addChild(childName.trim(), adults.map { it.id })
                        childName = ""
                        profiles = store.profiles()
                        message = "Perfil infantil creado y vinculado a los adultos autorizados."
                        onFamilyChanged()
                    }, modifier = Modifier.fillMaxWidth()) { Text("Añadir hijo/a") }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Acuerdo familiar digital", style = MaterialTheme.typography.titleMedium)
                    Text("La familia decide las reglas juntas. Famyrex muestra el cumplimiento, pero no ejecuta automáticamente una consecuencia.")
                    if (children.isEmpty()) {
                        Text("⚪ Crea primero un perfil infantil para establecer un acuerdo.")
                    } else {
                        Text("Perfil: ${children.first().displayName}")
                        OutlinedTextField(agreementMinutes, { agreementMinutes = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), label = { Text("Minutos diarios acordados") })
                        OutlinedTextField(agreementGoal, { agreementGoal = it }, Modifier.fillMaxWidth(), label = { Text("Objetivo de la familia") })
                        OutlinedTextField(agreementConsequence, { agreementConsequence = it }, Modifier.fillMaxWidth(), label = { Text("Qué hacer si se incumple") })
                        OutlinedTextField(agreementReviewDate, { agreementReviewDate = it }, Modifier.fillMaxWidth(), label = { Text("Fecha de revisión (AAAA-MM-DD)") })
                        Button(
                            enabled = agreementMinutes.toIntOrNull()?.let { it in 1..1440 } == true && agreementGoal.isNotBlank() && agreementReviewDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")),
                            onClick = {
                                val saved = FamilyAgreement(
                                    childProfileId = children.first().id,
                                    dailyMinutes = agreementMinutes.toInt(),
                                    goal = agreementGoal.trim(),
                                    consequence = agreementConsequence.trim(),
                                    reviewDate = agreementReviewDate.trim()
                                )
                                agreementStore.save(saved)
                                agreement = saved
                                message = "Acuerdo familiar guardado. Famyrex observará y explicará el cumplimiento."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (agreement == null) "Crear acuerdo" else "Actualizar acuerdo") }
                        if (agreement != null) {
                            OutlinedButton(onClick = { agreementStore.clear(); agreement = null; message = "Acuerdo eliminado de este dispositivo." }, modifier = Modifier.fillMaxWidth()) { Text("Eliminar acuerdo") }
                            Text("Revisión prevista: ${agreement!!.reviewDate}")
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vinculación offline segura", style = MaterialTheme.typography.titleMedium)
                    Text("La invitación ya no acepta un número arbitrario: contiene una identidad de familia y un secreto aleatorio. El código de 6 dígitos se deriva con HMAC y caduca.")
                    invitation?.let { token ->
                        Text("Código de vinculación", style = MaterialTheme.typography.labelLarge)
                        Text(OfflinePairingTokenCodec.code(token), style = MaterialTheme.typography.headlineMedium)
                        Text("Familia: ${token.familyId.take(12)}…")
                        Text("Clave de invitación", style = MaterialTheme.typography.labelLarge)
                        Text(OfflinePairingTokenCodec.encode(token))
                        Text("Huella: ${OfflinePairingTokenCodec.fingerprint(token.secret)}")
                        Text("Caduca en ${((token.expiresAtMs - System.currentTimeMillis()).coerceAtLeast(0L) / 60_000L) + 1} min aproximadamente")
                    }
                    Button(onClick = {
                        val token = OfflinePairingTokenCodec.create(identityStore.identity().familyId, System.currentTimeMillis())
                        invitation = token
                        message = "Invitación generada. Transfiere la clave y el código al dispositivo supervisado."
                    }, modifier = Modifier.fillMaxWidth()) { Text("Generar invitación") }
                    Text("Sin servidor: el dispositivo supervisado verifica localmente que el código corresponde a la clave, familia y caducidad mostradas aquí.")
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
                        Button(enabled = deviceName.isNotBlank(), onClick = {
                            store.addDevice(deviceName.trim(), children.first().id)
                            deviceName = ""
                            devices = store.devices()
                            message = "Dispositivo preparado para vincular con ${children.first().displayName}."
                            onFamilyChanged()
                        }, modifier = Modifier.fillMaxWidth()) { Text("Preparar dispositivo") }
                    }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Control parental", style = MaterialTheme.typography.titleMedium)
                    Text("Configura límites de pantalla, pausas y restricciones de aplicaciones en este dispositivo.")
                    Button(onClick = onOpenParentalControl, modifier = Modifier.fillMaxWidth()) { Text("Abrir Control parental") }
                }
            }
        }

        item {
            HorizontalDivider()
            Text("Modo de esta instalación", style = MaterialTheme.typography.titleMedium)
            Text("El modo supervisado está pensado para el dispositivo del menor: interfaz mínima y funciones de protección, sin panel de administración.")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { store.setAppMode(FamyrexAppMode.PARENT); message = "Esta instalación queda marcada como dispositivo de padres." }, modifier = Modifier.weight(1f)) { Text("Padres") }
                OutlinedButton(onClick = { store.setAppMode(FamyrexAppMode.SUPERVISED); message = "Esta instalación queda marcada como dispositivo supervisado." }, modifier = Modifier.weight(1f)) { Text("Supervisado") }
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
