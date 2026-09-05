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
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    fun join() {
        if (!PairingCodeProtocol.isValidFormat(code)) {
            message = "Introduce un código de 6 dígitos."
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

        // With the 0€ / no-server architecture, the six-digit invitation is a
        // bearer confirmation exchanged manually. There is no remote authority
        // on this device that could verify the parent's local PairingCodeStore.
        store.setDeviceState(pending.id, DeviceLinkState.LINKED)
        store.setAppMode(FamyrexAppMode.SUPERVISED)
        message = "Código aceptado. Dispositivo vinculado y modo supervisado activado."
        onJoined()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Unirse a una familia", style = MaterialTheme.typography.headlineMedium)
        Text("Introduce el código de 6 dígitos que te ha dado el adulto autorizado.")
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
        Text("Privacidad: el código se intercambia manualmente y no se envía a ningún servidor. La supervisión es explícita y visible.")
    }
}
