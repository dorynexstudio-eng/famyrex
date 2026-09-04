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
fun FamilyCard(profile: FamilyProfile, devices: List<FamilyDevice>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Familia")
            Text(profile.displayName)
            Text("Rol: ${profile.role.name}")
            Text("Dispositivos: ${devices.size}")
            devices.forEach { device ->
                Text("${device.displayName}: ${device.linkState.name}")
            }
        }
    }
}
