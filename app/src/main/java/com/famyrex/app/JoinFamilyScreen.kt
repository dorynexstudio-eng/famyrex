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
    val pairing = remember { PairingCoordinator(PairingCodeStore(context)) }
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    fun join() {
        if (!PairingCodeProtocol.isValidFormat(code)) {
            message = "Introduce un código de 6 dígitos."
            return
        }
        if (!pairing.validateCode(code)) {
            message = "El código no es válido o ha caducado. Pide al adulto autorizado que genere uno nuevo."
            return
        }
        val child = store.profiles().firstOrNull { it.role == FamilyRole.CHILD }
            ?: store.addChild("Dispositivo supervisado", store.profiles().filter { it.role == FamilyRole.OWNER || it.role == FamilyRole.ADULT }.map { it.id })
        val pending = store.devices().firstOrNull { it.linkState == DeviceLinkState.PENDING && it.ownerProfileId == child.id }
            ?: store.addDevice("Este dispositivo", child.id)
        if (!pairing.consumeCode(code)) {
            message = "El código ya no está disponible. Pide uno nuevo."
            return
        }
        store.setDeviceState(pending.id, DeviceLinkState.LINKED)
        store.setAppMode(FamyrexAppMode.SUPERVISED)
        message = "Dispositivo vinculado correctamente. Famyrex está en modo supervisado."
        onJoined()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Unirse a una familia", style = MaterialTheme.typography.headlineMedium)
        Text("Introduce el código de 6 dígitos que te ha dado el adulto autorizado. El código se comprueba localmente y solo puede utilizarse una vez.")
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(PairingCodeProtocol.CODE_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Código de vinculación") },
                    singleLine = true
                )
                Button(
                    enabled = code.length == PairingCodeProtocol.CODE_LENGTH,
                    onClick = ::join,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Vincular este dispositivo") }
                if (message.isNotBlank()) Text(message)
            }
        }
        Text("Privacidad: este proceso no envía el código a ningún servidor ni activa funciones ocultas. La supervisión se muestra de forma explícita.")
    }
}
