package com.famyrex.app

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun DashboardCapabilitiesCard(context: Context) {
    var family by remember { mutableStateOf(FamilyStore(context).profiles().firstOrNull()) }
    var devices by remember { mutableStateOf(FamilyStore(context).devices()) }
    var zones by remember { mutableStateOf(FamilyZoneStore(context).load()) }
    var webSettings by remember { mutableStateOf(WebSafetySettingsStore(context).load()) }
    var aiSummary by remember { mutableStateOf(AiSummaryStore(context).load()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun refresh() {
        val familyStore = FamilyStore(context)
        family = familyStore.profiles().firstOrNull()
        devices = familyStore.devices()
        zones = FamilyZoneStore(context).load()
        webSettings = WebSafetySettingsStore(context).load()
        aiSummary = AiSummaryStore(context).load()
    }

    LaunchedEffect(Unit) { refresh() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        family?.let { profile ->
            FamilyCard(profile, devices)
        } ?: CapabilityStatusCard(
            title = "Familia",
            status = "⚪ SIN DATOS SUFICIENTES",
            detail = "Todavía no hay un perfil familiar persistido en el sistema de familia."
        )

        val locationState = when {
            !LocationPermissionHelper.hasForeground(context) -> LocationPermissionState.DENIED
            LocationPermissionHelper.hasBackground(context) && LocationPermissionHelper.hasPrecise(context) -> LocationPermissionState.BACKGROUND_READY
            LocationPermissionHelper.hasPrecise(context) -> LocationPermissionState.PRECISE
            else -> LocationPermissionState.APPROXIMATE
        }
        LocationCard(zones, locationState)

        WebSafetyCard(webSettings, context)

        aiSummary?.let { AiSummaryCard(it) } ?: CapabilityStatusCard(
            title = "Análisis inteligente",
            status = "⚪ SIN DATOS SUFICIENTES",
            detail = "Aún no existe un resumen generado. El análisis se ejecuta sobre señales disponibles y no diagnostica estados psicológicos."
        )
    }
}

@Composable
private fun CapabilityStatusCard(title: String, status: String, detail: String) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(status)
            Text(detail)
        }
    }
}
