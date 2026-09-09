package com.famyrex.app

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FamyrexSettingsScreen(context: Context, onBack: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
                Column(Modifier.weight(1f)) { Text("Ajustes", style = MaterialTheme.typography.headlineMedium); Text("Configura Famyrex sin salir de la aplicación", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        item { SettingsSection(Icons.Default.Security, "Protección", "Permisos y servicios que Famyrex necesita para funcionar correctamente.") {
            SettingsAction("Acceso a datos de uso", "Verifica el permiso de UsageStats", { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) })
            SettingsAction("Accesibilidad", "Control parental y señales de protección", { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
            SettingsAction("Notificaciones", "Permite recibir alertas de Famyrex", { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) })
        } }
        item { SettingsSection(Icons.Default.LocationOn, "Ubicación", "Gestiona el permiso que permite usar ubicación y geozonas.") {
            SettingsAction("Permisos de ubicación", "Revisar permisos del dispositivo", { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))) })
        } }
        item { SettingsSection(Icons.Default.Notifications, "Alertas", "Las alertas son señales para revisar el contexto y no sustituyen una valoración humana.") {
            SettingsAction("Preferencias del sistema", "Puedes ajustar notificaciones desde Android", { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) })
        } }
        item { SettingsSection(Icons.Default.Settings, "Familia", "Perfiles, dispositivos, acuerdos y vinculación familiar se gestionan desde la sección Familia.") }
        item { SettingsSection(Icons.Default.Info, "Privacidad y ayuda", "Consulta qué observa Famyrex y cómo se utilizan los datos.") {
            SettingsAction("Política de privacidad", "Abrir política de privacidad", { context.startActivity(Intent(context, PrivacyPolicyActivity::class.java)) })
        } }
        item {
            Text("Famyrex 2.0.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
        }
    }
}

@Composable
private fun SettingsSection(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, actions: @Composable ColumnScope.() -> Unit = {}) {
    Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.padding(4.dp)); Text(title, style = MaterialTheme.typography.titleLarge) }
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            actions()
        }
    }
}

@Composable
private fun ColumnScope.SettingsAction(title: String, description: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) { Text(title); Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
