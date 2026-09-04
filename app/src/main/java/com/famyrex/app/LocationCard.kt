package com.famyrex.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable

@Composable
fun LocationCard(
    zones: List<FamilyZone>,
    permissionState: LocationPermissionState
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Localización familiar")
            Text("Permiso: ${permissionState.name}")
            Text("Zonas configuradas: ${zones.size}")
            zones.forEach { zone ->
                Text("${zone.name} · radio ${zone.radiusMeters.toInt()} m")
            }
            Text("Las zonas requieren permiso de ubicación y pueden tener latencia.")
        }
    }
}
