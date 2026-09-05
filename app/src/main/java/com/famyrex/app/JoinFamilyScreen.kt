package com.famyrex.app

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun JoinFamilyScreen(
    context: Context,
    modifier: Modifier = Modifier,
    onJoined: () -> Unit = {}
) {
    val store = remember { FamilyStore(context) }
    var invitation by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    fun join() {
        val token = OfflinePairingTokenCodec.verify(invitation, code, System.currentTimeMillis())
        if (token == null) {
            message = "Invitación incorrecta, manipulada o caducada. Pide una nueva al adulto autorizado."
            return
        }

        val child = store.profiles().firstOrNull { it.role == FamilyRole.CHILD }
            ?: store.addChild(
                "Dispositivo supervisado",
                store.profiles()
                    .filter { it.role == FamilyRole.OWNER || it.role == FamilyRole.ADULT }
                    .map { it.id }
            )
        val pending = store.devices().firstOrNull {
            it.linkState == DeviceLinkState.PENDING && it.ownerProfileId == child.id
        } ?: store.addDevice("Este dispositivo", child.id)

        store.setDeviceState(pending.id, DeviceLinkState.LINKED)
        store.setAppMode(FamyrexAppMode.SUPERVISED)
        message = "Invitación verificada para la familia ${token.familyId.take(12)}… Dispositivo vinculado y modo supervisado activado."
        onJoined()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Unirse a una familia", style = MaterialTheme.typography.headlineMedium)
        Text("El adulto autorizado debe darte la clave de invitación y el código de 6 dígitos. Ambos se verifican localmente, sin enviar datos a ningún servidor.")
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = invitation,
                    onValueChange = { invitation = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Clave de invitación") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(6) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Código de 6 dígitos") },
                    singleLine = true
                )
                Button(
                    enabled = invitation.isNotBlank() && code.length == 6,
                    onClick = ::join,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Verificar y vincular") }
                if (message.isNotBlank()) Text(message)
            }
        }
        Text("Privacidad: la invitación se intercambia manualmente y se verifica completamente en este dispositivo. La supervisión es explícita y visible.")
    }
}
