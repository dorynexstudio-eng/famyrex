package com.famyrex.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private const val PREMIUM_PREFS = "famyrex_prefs"
private const val PREMIUM_PARENT = "parent_name"
private const val PREMIUM_CHILD = "child_name"

class PremiumMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val familyStore = FamilyStore(applicationContext)
        SupervisedStateRestorer.restore(familyStore)
        FamyrexNotificationManager.ensureChannels(this)
        FamyrexWorkScheduler.scheduleProtectionHealth(applicationContext)
        setContent { FamyrexPremiumApp(applicationContext) }
    }
}

private val FamyrexNavy = Color(0xFF071B3A)
private val FamyrexBlue = Color(0xFF0B5CAB)
private val FamyrexCyan = Color(0xFF12D9E8)
private val FamyrexGreen = Color(0xFF2BE39A)
private val FamyrexSurface = Color(0xFFF5F8FC)
private val FamyrexText = Color(0xFF10233F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamyrexPremiumApp(context: Context) {
    val prefs = remember { context.getSharedPreferences(PREMIUM_PREFS, Context.MODE_PRIVATE) }
    val familyStore = remember { FamilyStore(context) }
    var tab by remember { mutableIntStateOf(0) }
    var family by remember { mutableStateOf(loadPremiumFamily(prefs)) }
    var zones by remember { mutableStateOf(loadPremiumZones(prefs)) }
    var parentalControlOpen by remember { mutableStateOf(false) }
    var remoteControlOpen by remember { mutableStateOf(false) }

    fun refreshFamily() {
        val profiles = familyStore.profiles()
        val owner = profiles.firstOrNull { it.role == FamilyRole.OWNER }
        val child = profiles.firstOrNull { it.role == FamilyRole.CHILD }
        family = if (owner != null || child != null) PremiumFamilyState(owner?.displayName.orEmpty(), child?.displayName.orEmpty()) else loadPremiumFamily(prefs)
    }

    LaunchedEffect(Unit) { refreshFamily() }

    Scaffold(
        containerColor = FamyrexSurface,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(12.dp), color = FamyrexNavy, modifier = Modifier.size(42.dp)) {
                            Image(painter = painterResource(id = R.drawable.ic_famyrex_logo), contentDescription = "Famyrex", contentScale = ContentScale.Crop, modifier = Modifier.padding(4.dp).fillMaxSize())
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Famyrex", fontWeight = FontWeight.ExtraBold, color = FamyrexText, fontSize = 23.sp)
                            Text("Seguridad familiar inteligente", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                    }
                },
                actions = { IconButton(onClick = { context.startActivity(Intent(context, PrivacyPolicyActivity::class.java)) }) { Icon(Icons.Default.Settings, "Configuración", tint = FamyrexText) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FamyrexSurface)
            )
        },
        bottomBar = {
            FamyrexNavigationBar(tab) { selected ->
                tab = selected
                parentalControlOpen = false
                remoteControlOpen = false
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> PremiumDashboard(context, family)
                1 -> RealAlertsScreen(context, Modifier.fillMaxSize())
                2 -> when {
                    remoteControlOpen -> FamilyRemoteControlScreen(
                        context = context,
                        familyId = FamyrexCloudFamilyRepository(context).cachedFamilyId(),
                        modifier = Modifier.fillMaxSize()
                    )
                    parentalControlOpen -> ParentalControlScreen(Modifier.fillMaxSize())
                    else -> FamilyCoreScreen(
                        context = context,
                        onOpenParentalControl = { parentalControlOpen = true },
                        onOpenRemoteControl = { remoteControlOpen = true },
                        onFamilyChanged = { refreshFamily() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                3 -> LocationScreen(context, zones, { updated -> zones = updated; savePremiumZones(prefs, updated) }, Modifier.fillMaxSize())
                4 -> FamilyAssistantScreen(context, Modifier.fillMaxSize())
                5 -> DailyReportScreen(context, onBack = { tab = 0 }, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun FamyrexNavigationBar(selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp, modifier = Modifier.navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            PremiumNavItem(0, selected, "Inicio", Icons.Default.Home, onSelect)
            PremiumNavItem(1, selected, "Alertas", Icons.Default.Notifications, onSelect)
            PremiumNavItem(2, selected, "Familia", Icons.Default.Person, onSelect)
            PremiumNavItem(3, selected, "Zonas", Icons.Default.LocationOn, onSelect)
            PremiumNavItem(4, selected, "IA", Icons.Default.Info, onSelect)
            PremiumNavItem(5, selected, "Informe", Icons.Default.CheckCircle, onSelect)
        }
    }
}

@Composable
private fun PremiumNavItem(index: Int, selected: Int, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onSelect: (Int) -> Unit) {
    val active = index == selected
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 3.dp)) {
        IconButton(onClick = { onSelect(index) }, modifier = Modifier.size(42.dp)) {
            Surface(shape = CircleShape, color = if (active) FamyrexBlue else Color.Transparent) { Icon(icon, label, tint = if (active) Color.White else Color(0xFF64748B), modifier = Modifier.padding(9.dp)) }
        }
        Text(label, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = if (active) FamyrexBlue else Color(0xFF64748B))
    }
}

@Composable
private fun PremiumDashboard(context: Context, family: PremiumFamilyState) {
    var components by remember { mutableStateOf(ProtectionComponentChecker.check(context)) }
    var status by remember { mutableStateOf("Sin datos suficientes") }
    fun refresh() {
        components = ProtectionComponentChecker.check(context)
        val usage = ParentalUsageMonitor(context); val access = usage.hasUsageAccess(); val accessibility = isPremiumAccessibilityEnabled(context)
        val start = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val minutes = if (access) usage.queryUsage(start, System.currentTimeMillis()).sumOf { it.totalTimeInForeground } / 60_000L else null
        val evaluated = ParentalStatusEvaluator.overall(access, accessibility, minutes, ParentalControlStore(context).load().screenTimeLimit)
        status = when (evaluated) { ParentalStatus.HEALTHY -> "Todo en orden"; ParentalStatus.ATTENTION -> "Hay algo que revisar"; ParentalStatus.RISK -> "Atención recomendada"; else -> "Sin datos suficientes" }
    }
    LaunchedEffect(Unit) { refresh() }
    val active = components.count { it.status == ProtectionComponentStatus.ACTIVE }; val degraded = components.count { it.status == ProtectionComponentStatus.DEGRADED }; val configured = family.child.isNotBlank()
    LazyColumn(modifier = Modifier.fillMaxSize().background(FamyrexSurface).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(4.dp)); Text("Centro de seguridad familiar", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = FamyrexText); Text(if (configured) "Protección de ${family.child}" else "Configura tu familia para comenzar", color = Color(0xFF64748B), fontSize = 14.sp) }
        item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = FamyrexNavy)) { Column(Modifier.padding(22.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Surface(shape = CircleShape, color = FamyrexGreen.copy(alpha = .18f)) { Image(painterResource(R.drawable.ic_famyrex_logo), null, modifier = Modifier.padding(7.dp).size(40.dp)) }; Spacer(Modifier.width(14.dp)); Column { Text("Estado de protección", color = Color.White.copy(alpha = .72f), fontSize = 13.sp); Text(status, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold) } }; Spacer(Modifier.height(16.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatusPill("$active", "activas", FamyrexGreen); StatusPill("$degraded", "a revisar", Color(0xFFFFB74D)) }; Spacer(Modifier.height(16.dp)); Button(onClick = { refresh() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Actualizar protección", fontWeight = FontWeight.Bold) } } } }
        item { Text("Tu familia", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FamyrexText) }
        item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Surface(shape = CircleShape, color = Color(0xFFE7F4FF)) { Icon(Icons.Default.Person, null, tint = FamyrexBlue, modifier = Modifier.padding(11.dp).size(28.dp)) }; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(if (family.parent.isBlank()) "Adulto responsable" else family.parent, fontWeight = FontWeight.Bold, color = FamyrexText); Text(if (family.child.isBlank()) "Ningún dispositivo vinculado todavía" else "Supervisando a ${family.child}", color = Color(0xFF64748B), fontSize = 13.sp) }; Text(if (configured) "●" else "○", color = if (configured) FamyrexGreen else Color(0xFF94A3B8), fontSize = 22.sp) } } }
        item { Text("Protecciones", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FamyrexText) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { PremiumFeatureCard("Uso digital", "Apps y tiempo", Icons.Default.Info, FamyrexBlue, Modifier.weight(1f)); PremiumFeatureCard("Alertas", "Señales de riesgo", Icons.Default.Warning, Color(0xFF0D8A83), Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { PremiumFeatureCard("Zonas seguras", "Casa y colegio", Icons.Default.LocationOn, Color(0xFF118AB2), Modifier.weight(1f)); PremiumFeatureCard("Inteligencia", "Recomendaciones", Icons.Default.CheckCircle, Color(0xFF5B46C5), Modifier.weight(1f)) } }
        item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CheckCircle, null, tint = FamyrexGreen, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(12.dp)); Column { Text("Privacidad por diseño", fontWeight = FontWeight.Bold, color = FamyrexText); Text("El procesamiento funcional se realiza en el dispositivo.", color = Color(0xFF64748B), fontSize = 13.sp) } } } }
        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun StatusPill(value: String, label: String, tint: Color) { Surface(color = Color.White.copy(alpha = .10f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().weight(1f)) { Column(Modifier.padding(12.dp)) { Text(value, color = tint, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text(label, color = Color.White.copy(alpha = .72f), fontSize = 12.sp) } } }

@Composable
private fun PremiumFeatureCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier) { Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(16.dp)) { Surface(shape = RoundedCornerShape(12.dp), color = tint.copy(alpha = .12f)) { Icon(icon, null, tint = tint, modifier = Modifier.padding(9.dp).size(24.dp)) }; Spacer(Modifier.height(12.dp)); Text(title, fontWeight = FontWeight.Bold, color = FamyrexText); Text(subtitle, color = Color(0xFF64748B), fontSize = 12.sp) } } }

private data class PremiumFamilyState(val parent: String, val child: String)
private fun loadPremiumFamily(prefs: android.content.SharedPreferences): PremiumFamilyState = PremiumFamilyState(prefs.getString(PREMIUM_PARENT, "") ?: "", prefs.getString(PREMIUM_CHILD, "") ?: "")
private fun loadPremiumZones(prefs: android.content.SharedPreferences): List<GeoZone> { val raw = prefs.getString("geo_zones", "") ?: return emptyList(); if (raw.isBlank()) return emptyList(); return raw.split(";").mapNotNull { row -> val parts = row.split("|"); if (parts.size == 4) runCatching { GeoZone(parts[0], parts[1].toDouble(), parts[2].toDouble(), parts[3].toFloat()) }.getOrNull() else null } }
private fun savePremiumZones(prefs: android.content.SharedPreferences, zones: List<GeoZone>) { prefs.edit().putString("geo_zones", zones.joinToString(";") { "${it.name}|${it.latitude}|${it.longitude}|${it.radiusMeters}" }).apply() }
private fun isPremiumAccessibilityEnabled(context: Context): Boolean { val expected = "${context.packageName}/${FamyrexParentalAccessibilityService::class.java.name}"; val enabled = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false; return enabled.split(':').any { it.equals(expected, ignoreCase = true) } }
