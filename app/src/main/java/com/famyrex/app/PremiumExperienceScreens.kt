package com.famyrex.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun PremiumAlertsScreen(context: Context, modifier: Modifier = Modifier) {
    val alerts = remember { AlertStore(context).load() }
    val pending = alerts.count { it.lifecycleStatus != AlertLifecycleStatus.RESOLVED && it.lifecycleStatus != AlertLifecycleStatus.DISMISSED }
    var details by remember { mutableStateOf(false) }
    PremiumScreenScaffold(
        modifier = modifier,
        icon = Icons.Default.Notifications,
        title = "Alertas",
        subtitle = if (pending == 0) "No hay nada que requiera tu atención ahora" else "$pending señal${if (pending == 1) "" else "es"} para revisar"
    ) {
        PremiumInfoCard(
            icon = Icons.Default.CheckCircle,
            title = if (pending == 0) "Todo tranquilo" else "Hay algo que revisar",
            text = if (pending == 0) "Famyrex no detecta alertas pendientes. Una ausencia de alertas no demuestra que no exista ningún riesgo." else "Famyrex ha detectado señales que conviene revisar con contexto antes de tomar decisiones.",
            action = { details = !details },
            actionLabel = if (details) "Ocultar detalle" else "Ver alertas"
        )
        PremiumInfoCard(Icons.Default.Security, "Cómo interpreta Famyrex", "Las alertas son señales, no diagnósticos ni acusaciones. La aplicación separa datos observados, cambios, indicadores y hechos confirmados.")
        if (details) {
            Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                RealAlertsScreen(context, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun PremiumFamilyScreen(
    context: Context,
    onOpenParentalControl: () -> Unit,
    onOpenRemoteControl: () -> Unit,
    onFamilyChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val store = remember { FamilyStore(context) }
    val profiles = remember { store.profiles() }
    val owner = profiles.firstOrNull { it.role == FamilyRole.OWNER }
    val children = profiles.filter { it.role == FamilyRole.CHILD }
    var management by remember { mutableStateOf(false) }
    PremiumScreenScaffold(modifier, Icons.Default.Group, "Familia", "Personas, acuerdos y protección en un mismo lugar") {
        if (owner != null) PremiumPersonCard(owner.displayName, "Adulto responsable", Icons.Default.Person)
        if (children.isEmpty()) {
            PremiumInfoCard(Icons.Default.Person, "Añade un perfil infantil", "Vincula el dispositivo de un hijo/a para poder consultar actividad, ubicación y señales de protección.", onClick = { management = true }, actionLabel = "Configurar familia")
        } else {
            children.forEach { child -> PremiumPersonCard(child.displayName, "Perfil infantil protegido", Icons.Default.Person) }
        }
        PremiumInfoCard(Icons.Default.Security, "Protección y límites", "Gestiona tiempo de pantalla, aplicaciones y las medidas que requieren permisos especiales de Android.", onClick = onOpenParentalControl, actionLabel = "Gestionar protección")
        PremiumInfoCard(Icons.Default.Group, "Gestión de la familia", "Códigos de vinculación, perfiles y conexión entre dispositivos.", onClick = { management = true }, actionLabel = "Gestionar familia")
        if (children.isNotEmpty()) PremiumInfoCard(Icons.Default.Security, "Control remoto", "Accede a las acciones disponibles para el dispositivo vinculado, siempre dentro de las reglas y permisos de Famyrex.", onClick = onOpenRemoteControl, actionLabel = "Abrir control")
        if (management) {
            Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                FamilyCoreScreen(context, onOpenParentalControl, onOpenRemoteControl, onFamilyChanged, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun PremiumLocationScreen(
    context: Context,
    zones: List<GeoZone>,
    onZonesChange: (List<GeoZone>) -> Unit,
    modifier: Modifier = Modifier
) {
    var foreground by remember { mutableStateOf(hasForegroundLocation(context)) }
    var background by remember { mutableStateOf(hasBackgroundLocation(context)) }
    var details by remember { mutableStateOf(false) }
    var disclosure by remember { mutableStateOf(false) }
    val foregroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        foreground = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        background = hasBackgroundLocation(context)
    }
    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> background = granted }

    PremiumScreenScaffold(modifier, Icons.Default.LocationOn, "Mapa", "Ubicación familiar y lugares seguros") {
        PremiumInfoCard(
            Icons.Default.LocationOn,
            if (foreground) "Ubicación disponible" else "Ubicación desactivada",
            if (foreground) {
                if (background) "Famyrex tiene el permiso necesario para actualizar ubicación en segundo plano." else "La ubicación funciona mientras la app está en uso. Para geozonas y actualizaciones en segundo plano hace falta un permiso adicional de Android."
            } else "Activa la ubicación solo si quieres utilizar esta función. Famyrex la utiliza para geozonas y protección familiar.",
            onClick = { if (!foreground) foregroundLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) else details = !details },
            actionLabel = if (!foreground) "Activar ubicación" else if (details) "Ocultar gestión" else "Gestionar ubicación"
        )
        if (foreground && Build.VERSION.SDK_INT >= 29 && !background) {
            PremiumInfoCard(Icons.Default.Security, "Ubicación en segundo plano", "Esta función es opcional. Permite mantener geozonas y recibir cambios de ubicación cuando Famyrex no está abierta. Android mostrará su propia pantalla de permisos.", onClick = { disclosure = true }, actionLabel = "Activar segundo plano")
        }
        PremiumInfoCard(Icons.Default.CheckCircle, "Lugares seguros", if (zones.isEmpty()) "Todavía no tienes geozonas configuradas." else "Tienes ${zones.size} lugar${if (zones.size == 1) "" else "es"} seguro${if (zones.size == 1) "" else "s"} configurado${if (zones.size == 1) "" else "s"}.")
        if (disclosure) {
            Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Antes de activarlo", style = MaterialTheme.typography.titleLarge)
                    Text("Famyrex necesita ubicación en segundo plano para mantener actualizadas las geozonas cuando la aplicación no está abierta. La función es opcional y puedes decidir no concederla.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { disclosure = false }, modifier = Modifier.weight(1f)) { Text("Ahora no") }
                        Button(onClick = {
                            disclosure = false
                            if (Build.VERSION.SDK_INT >= 29) backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }, modifier = Modifier.weight(1f)) { Text("Continuar") }
                    }
                }
            }
        }
        if (details) {
            Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                LocationScreen(context, zones, onZonesChange, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun PremiumAssistantScreen(context: Context, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    PremiumScreenScaffold(modifier, Icons.Default.Security, "Asistente", "Entiende lo que ocurre y decide qué hacer después") {
        PremiumInfoCard(Icons.Default.Security, "Un asistente para la familia", "Famyrex combina señales de protección, contexto familiar y recomendaciones para ayudarte a interpretar una situación sin convertir una señal en una acusación.")
        PremiumInfoCard(Icons.Default.CheckCircle, "Mediación y contexto", "Cuando existe un posible conflicto, el asistente distingue lo observado de lo que todavía no se sabe y propone próximos pasos revisables.", onClick = { open = !open }, actionLabel = if (open) "Ocultar asistente" else "Abrir asistente")
        if (open) {
            Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                FamilyAssistantScreen(context, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun PremiumActivityScreen(context: Context, onOpenReport: () -> Unit, modifier: Modifier = Modifier) {
    var details by remember { mutableStateOf(false) }
    PremiumScreenScaffold(modifier, Icons.Default.Timelapse, "Actividad", "Tiempo de uso, hábitos y tendencias de hoy") {
        PremiumInfoCard(Icons.Default.Timelapse, "Tu resumen digital", "Consulta cuánto tiempo se ha utilizado el dispositivo y qué aplicaciones concentran la actividad. Los datos dependen del acceso de uso concedido por Android.", onClick = { details = !details }, actionLabel = if (details) "Ocultar actividad" else "Ver actividad")
        PremiumInfoCard(Icons.Default.Assessment, "Informe familiar", "Convierte la actividad y las señales disponibles en una lectura sencilla del día.", onClick = onOpenReport, actionLabel = "Abrir informe")
        PremiumInfoCard(Icons.Default.CheckCircle, "Pensado para entender, no vigilar", "Famyrex muestra tendencias y contexto para facilitar conversaciones familiares, evitando presentar una métrica aislada como una conclusión.")
        if (details) {
            Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                UsageInsightsScreen(context, Modifier.fillMaxWidth(), onOpenSettings = {}, onOpenReport = onOpenReport)
            }
        }
    }
}

@Composable
private fun PremiumScreenScaffold(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    LazyColumn(modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp).size(28.dp))
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineMedium)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
        item {
            Text("Famyrex prioriza señales y contexto. Las funciones de protección dependen de los permisos y accesos que la familia decida activar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}

@Composable
private fun PremiumInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String,
    onClick: (() -> Unit)? = null,
    actionLabel: String? = null
) {
    Card(onClick = onClick ?: {}, enabled = onClick != null, modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(24.dp))
                }
                Spacer(Modifier.size(12.dp))
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onClick != null && actionLabel != null) TextButton(onClick = onClick) { Text(actionLabel) }
        }
    }
}

@Composable
private fun PremiumPersonCard(name: String, role: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primaryContainer) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp).size(28.dp)) }
            Spacer(Modifier.size(14.dp))
            Column { Text(name, style = MaterialTheme.typography.titleLarge); Text(role, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

private fun hasForegroundLocation(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun hasBackgroundLocation(context: Context): Boolean =
    Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
