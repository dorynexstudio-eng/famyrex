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
import java.util.UUID

@Composable
fun JoinFamilyScreen(
    context: Context,
    modifier: Modifier = Modifier,
    onJoined: () -> Unit = {}
) {
    val appContext = context.applicationContext
    val store = remember { FamilyStore(appContext) }
    val existingIdentity = remember { store.verifiedFamilyIdentity() }
    var code by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var childLabel by remember { mutableStateOf("Perfil infantil") }
    var message by remember { mutableStateOf("") }
    var joining by remember { mutableStateOf(false) }

    if (existingIdentity != null) {
        val child = store.supervisedChild()
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Dispositivo vinculado", style = MaterialTheme.typography.headlineMedium)
            Text("Este dispositivo ya pertenece a una familia Famyrex.")
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Familia: ${existingIdentity.familyId.take(12)}…")
                    Text("Perfil infantil: ${child?.displayName ?: "no disponible"}")
                    Text("Huella local: ${existingIdentity.fingerprint}")
                    Text("La vinculación se conserva tras reiniciar la aplicación.")
                }
            }
            Button(onClick = onJoined, modifier = Modifier.fillMaxWidth()) { Text("Continuar") }
        }
        return
    }

    fun join() {
        if (joining) return
        joining = true
        message = "Vinculando dispositivo…"
        val normalizedLabel = childLabel.trim().ifBlank { "Perfil infantil" }.take(40)
        val normalizedToken = token.trim()
        val deviceId = "device-${UUID.randomUUID().toString().replace("-", "").take(16)}"

        FamyrexPairingService(appContext).redeemCode(
            code = code,
            token = normalizedToken,
            childLabel = normalizedLabel,
            famyrexMemberId = null,
            famyrexDeviceId = deviceId,
            onSuccess = { familyId, _, resolvedMemberId, resolvedDeviceId ->
                runCatching {
                    val child = store.ensureSupervisedChild(resolvedMemberId, normalizedLabel)
                    val device = store.addDeviceWithId(resolvedDeviceId, "Este dispositivo", child.id)
                    require(device.id == resolvedDeviceId)
                    val secret = UUID.randomUUID().toString().replace("-", "").take(32)
                    store.saveVerifiedFamilyIdentity(familyId, secret, OfflinePairingTokenCodec.fingerprint(secret))
                    store.setDeviceState(resolvedDeviceId, DeviceLinkState.LINKED)
                    store.setAppMode(FamyrexAppMode.SUPERVISED)
                }.onFailure {
                    message = "La vinculación remota se completó, pero no se pudo guardar el estado local: ${it.message}"
                    joining = false
                    return@redeemCode
                }
                message = "Familia vinculada correctamente."
                joining = false
                onJoined()
            },
            onError = {
                message = it
                joining = false
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Unirse a una familia", style = MaterialTheme.typography.headlineMedium)
        Text("Introduce el código y la clave de vinculación que te proporciona el adulto autorizado. Ambos son necesarios para completar la vinculación segura.")
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(childLabel, { childLabel = it.take(40) }, Modifier.fillMaxWidth(), label = { Text("Nombre del perfil") }, singleLine = true)
                OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), label = { Text("Código de 6 dígitos") }, singleLine = true)
                OutlinedTextField(token, { token = it.filterNot(Char::isWhitespace) }, Modifier.fillMaxWidth(), label = { Text("Clave de vinculación") }, singleLine = true)
                Button(enabled = code.length == 6 && token.isNotBlank() && !joining, onClick = ::join, modifier = Modifier.fillMaxWidth()) { Text(if (joining) "Vinculando…" else "Verificar y vincular") }
                if (message.isNotBlank()) Text(message)
            }
        }
        Text("Privacidad: el dispositivo usa una identidad técnica anónima de Firebase para el transporte. La identidad lógica de Famyrex se mantiene separada y se valida antes de ejecutar comandos.")
    }
}
