package com.famyrex.app

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

private data class OverviewUsage(
    val totalMinutes: Long,
    val topApps: List<Pair<String, Long>>
)

@Composable
fun FamilyChildOverviewScreen(
    context: Context,
    modifier: Modifier = Modifier,
    onOpenAlerts: () -> Unit = {},
    onOpenLocation: () -> Unit = {},
    onOpenUsage: () -> Unit = {},
    onOpenFamily: () -> Unit = {},
    onOpenParentalControl: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val appContext = context.applicationContext
    val store = remember { FamilyStore(appContext) }
    var profiles by remember { mutableStateOf(store.profiles()) }
    var selectedChildId by remember { mutableStateOf<String?>(null) }
    var usage by remember { mutableStateOf<OverviewUsage?>(null) }
    var alertCount by remember { mutableStateOf(0) }
    var childLocation by remember { mutableStateOf<FamilyChildLocation?>(null) }
    var protection by remember { mutableStateOf(ProtectionComponentChecker.check(appContext)) }
    var communicationMonitoringEnabled by remember { mutableStateOf(CommunicationMonitoringSettings.isNotificationListenerEnabled(appContext)) }
    var communicationIncidentCount by remember { mutableStateOf(0) }

    fun refresh() {
        profiles = store.profiles()
        val children = profiles.filter { it.role == FamilyRole.CHILD }
        if (selectedChildId !in children.map { it.id }) selectedChildId = children.firstOrNull()?.id
        val monitor = ParentalUsageMonitor(appContext)
        val start = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val stats = if (monitor.hasUsageAccess()) monitor.queryUsage(start, System.currentTimeMillis()) else emptyList()
        val topApps = stats.take(3).mapNotNull { stat ->
            val label = runCatching { appContext.packageManager.getApplicationLabel(appContext.packageManager.getApplicationInfo(stat.packageName, 0)).toString() }.getOrNull()
            label?.let { it to (stat.totalTimeInForeground / 60_000L) }
        }
        usage = if (stats.isNotEmpty()) OverviewUsage(stats.sumOf { it.totalTimeInForeground } / 60_000L, topApps) else null
        val alerts = AlertStore(appContext).load()
        alertCount = alerts.count { it.lifecycleStatus != AlertLifecycleStatus.RESOLVED && it.lifecycleStatus != AlertLifecycleStatus.DISMISSED }
        communicationIncidentCount = CommunicationRiskIncidentStore(appContext).load().count { incident ->
            incident.status != RiskIncidentStatus.RESOLVED && incident.status != RiskIncidentStatus.DISMISSED && incident.status != RiskIncidentStatus.AUTO_DISMISSED
        }
        communicationMonitoringEnabled = CommunicationMonitoringSettings.isNotificationListenerEnabled(appContext)
        protection = ProtectionComponentChecker.check(appContext)
    }

    LaunchedEffect(Unit) { refresh() }
    val children = profiles.filter { it.role == FamilyRole.CHILD }
    val selectedChild = children.firstOrNull { it.id == selectedChildId } ?: children.firstOrNull()

    LaunchedEffect(selectedChild?.id) {
        childLocation = null
        selectedChild?.id?.let { childId ->
            FamilyLocationRepository(appContext).loadChildLocation(
                childMemberId = childId,
                onSuccess = { childLocation = it }
            )
        }
    }

    val activeProtection = protection.count { it.status == ProtectionComponentStatus.ACTIVE }
    val attentionProtection = protection.count { it.status == ProtectionComponentStatus.DEGRADED || it.status == ProtectionComponentStatus.NOT_CONFIGURED }
    val screenLimit = ParentalControlStore(appContext).load().screenTimeLimit
    val usageLabel = usage?.let { formatOverviewMinutes(it.totalMinutes) } ?: "No disponible"
    val limitLabel = screenLimit?.let { formatOverviewMinutes(it.dailyMinutes.toLong()) } ?: "Sin límite configurado"

    LazyColumn(modifier = modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Familia", style = MaterialTheme.typography.headlineMedium)
                    Text("Un vistazo a lo importante", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, null); Spacer(Modifier.size(4.dp)); Text("Ajustes") }
            }
        }
        if (children.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Añade un perfil infantil", style = MaterialTheme.typography.titleLarge)
                        Text("Cuando vincules un hijo/a, aquí tendrás su tiempo de uso, ubicación, alertas y estado de protección en un solo lugar.")
                        OutlinedButton(onClick = onOpenFamily, modifier = Modifier.fillMaxWidth()) { Text("Configurar familia") }
                    }
                }
            }
        } else {
            item {
                Text("¿A quién quieres ver?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    children.forEach { child ->
                        val selected = child.id == selectedChild?.id
                        Surface(onClick = { selectedChildId = child.id }, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(shape = androidx.compose.foundation.shape.CircleShape, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest) { Icon(Icons.Default.Person, null, tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(9.dp).size(24.dp)) }
                                Spacer(Modifier.size(6.dp)); Text(child.displayName, maxLines = 1)
                            }
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(12.dp).size(32.dp)) }
                        Spacer(Modifier.size(14.dp))
                        Column {
                            Text(selectedChild?.displayName ?: "Perfil", style = MaterialTheme.typography.titleLarge)
                            Text(if (attentionProtection == 0 && alertCount == 0) "Todo tranquilo por ahora" else "Hay información que merece una revisión", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                OverviewMetricCard(Icons.Default.Timelapse, "Tiempo de uso", usageLabel, if (usage != null) "$usageLabel hoy · límite $limitLabel" else "Activa el acceso a datos de uso para ver la actividad", onOpenUsage)
            }
            if (!usage?.topApps.isNullOrEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Más utilizadas hoy", style = MaterialTheme.typography.titleMedium)
                            usage!!.topApps.forEachIndexed { index, app ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${index + 1}. ${app.first}")
                                    Text(formatOverviewMinutes(app.second), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            item {
                FamilyLocationOverviewCard(
                    context = appContext,
                    childName = selectedChild?.displayName ?: "Perfil infantil",
                    location = childLocation,
                    onOpenLocation = onOpenLocation
                )
            }
            item {
                CommunicationMonitoringOverviewCard(
                    enabled = communicationMonitoringEnabled,
                    incidentCount = communicationIncidentCount,
                    onOpenSettings = { CommunicationMonitoringSettings.openSystemSettings(appContext) }
                )
            }
            item {
                OverviewMetricCard(Icons.Default.Notifications, "Alertas", if (alertCount == 0) "Todo tranquilo" else "$alertCount para revisar", "Las alertas son señales contextualizadas, no diagnósticos", onOpenAlerts)
            }
            item {
                OverviewMetricCard(Icons.Default.CheckCircle, "Protección", "$activeProtection protecciones activas", if (attentionProtection == 0) "Todo está funcionando correctamente" else "$attentionProtection elementos necesitan atención", onOpenParentalControl)
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Inteligencia familiar", style = MaterialTheme.typography.titleMedium)
                        Text("Famyrex combina actividad, cambios y señales para ayudarte a entender el contexto antes de tomar una decisión.")
                        TextButton(onClick = onOpenFamily) { Text("Gestionar familia") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunicationMonitoringOverviewCard(
    enabled: Boolean,
    incidentCount: Int,
    onOpenSettings: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(11.dp).size(26.dp))
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Protección de comunicaciones", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (enabled) "Monitorización activa" else "Necesita activación", style = MaterialTheme.typography.titleLarge)
                }
            }
            Text(
                if (enabled) {
                    if (incidentCount == 0) "No hay señales de riesgo pendientes. Famyrex analiza las notificaciones expuestas por Android sin guardar la conversación completa."
                    else "$incidentCount episodio(s) de riesgo para revisar. Famyrex muestra señales y contexto, no acusaciones."
                } else {
                    "Activa el acceso especial de Android para que Famyrex pueda analizar las notificaciones disponibles y detectar señales de riesgo. La función es opcional."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(if (enabled) "Revisar acceso" else "Activar en Android")
            }
        }
    }
}

@Composable
private fun FamilyLocationOverviewCard(
    context: Context,
    childName: String,
    location: FamilyChildLocation?,
    onOpenLocation: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(11.dp).size(26.dp))
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ubicación", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (location != null) "${childName} está localizado" else "Sin ubicación reciente", style = MaterialTheme.typography.titleLarge)
                }
            }
            if (location != null) {
                val age = (System.currentTimeMillis() - location.capturedAtMs).coerceAtLeast(0L)
                val freshness = if (age < 15 * 60_000L) "Actualizada recientemente" else "Última ubicación disponible"
                Text("${location.latitude.formatOverviewCoordinate()}, ${location.longitude.formatOverviewCoordinate()} · ${freshness}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Precisión aproximada: ${location.accuracyMeters.toInt().coerceAtLeast(1)} m")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { openFamilyLocationInMaps(context, location.latitude, location.longitude, childName) }, modifier = Modifier.weight(1f)) {
                        Text("Ver en Maps")
                    }
                    Button(onClick = { navigateFamilyLocationInMaps(context, location.latitude, location.longitude) }, modifier = Modifier.weight(1f)) {
                        Text("Cómo llegar")
                    }
                }
            } else {
                Text("El dispositivo infantil todavía no ha enviado una ubicación. Comprueba los permisos de ubicación en el móvil del menor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onOpenLocation, modifier = Modifier.fillMaxWidth()) { Text("Abrir ubicación") }
            }
        }
    }
}

@Composable
private fun OverviewMetricCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, detail: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(11.dp).size(26.dp)) }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleLarge)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatOverviewMinutes(minutes: Long): String {
    val h = minutes / 60; val m = minutes % 60
    return if (h > 0) "${h}h ${m}min" else "${m} min"
}

private fun Double.formatOverviewCoordinate(): String = String.format(Locale.US, "%.5f", this)
