package com.famyrex.app

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun SupervisedDeviceScreen(context: Context, modifier: Modifier = Modifier) {
    val store = remember { FamilyStore(context) }
    val agreementStore = remember { FamilyAgreementStore(context) }
    var status by remember { mutableStateOf<ParentalStatus?>(null) }
    var agreementStatus by remember { mutableStateOf<AgreementStatus?>(null) }
    var childName by remember { mutableStateOf("") }

    fun refresh() {
        val child = store.supervisedChild()
        childName = child?.displayName.orEmpty()
        val usage = ParentalUsageMonitor(context)
        val usageAccess = usage.hasUsageAccess()
        val accessibilityEnabled = isSupervisedAccessibilityEnabled(context)
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val totalMinutes = if (usageAccess) {
            usage.queryUsage(start, System.currentTimeMillis()).sumOf { it.totalTimeInForeground } / 60_000L
        } else null
        val limit = ParentalControlStore(context).load().screenTimeLimit
        status = ParentalStatusEvaluator.overall(usageAccess, accessibilityEnabled, totalMinutes, limit)
        val agreement = child?.id?.let(agreementStore::load)
        agreementStatus = FamilyAgreementEngine.evaluate(agreement, totalMinutes)
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Famyrex", style = MaterialTheme.typography.headlineMedium)
        Text("Tu espacio familiar", style = MaterialTheme.typography.headlineSmall)
        Text(if (childName.isBlank()) "Dispositivo supervisado" else "Perfil: $childName")

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tu acuerdo", style = MaterialTheme.typography.titleLarge)
                val child = store.supervisedChild()
                val agreement = child?.id?.let(agreementStore::load)
                val current = agreementStatus
                if (child == null) {
                    Text("⚪ Este dispositivo todavía no tiene un perfil infantil asignado.")
                } else if (agreement == null) {
                    Text("⚪ Todavía no hay un acuerdo familiar configurado para ${child.displayName}.")
                } else {
                    Text("Límite acordado: ${agreement.dailyMinutes} min al día")
                    Text("Objetivo: ${agreement.goal}")
                    when (current?.state) {
                        AgreementState.ON_TRACK -> Text("🟢 Vas dentro de lo acordado. Te quedan ${current.remainingMinutes} min aproximadamente.")
                        AgreementState.ATTENTION -> Text("🟠 Te acercas al límite acordado. Te quedan ${current.remainingMinutes} min aproximadamente.")
                        AgreementState.EXCEEDED -> Text("🟠 Hoy se ha superado el límite acordado. El acuerdo indica: ${agreement.consequence}")
                        AgreementState.INSUFFICIENT_DATA, null -> Text("⚪ Aún no hay datos suficientes para valorar el cumplimiento.")
                    }
                    Text("Revisión del acuerdo: ${agreement.reviewDate}")
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tu bienestar digital", style = MaterialTheme.typography.titleMedium)
                Text("Aquí puedes ver tu propio uso y cómo va tu acuerdo. Esta pantalla no muestra alertas privadas de comunicación ni sospechas sobre otras personas.")
                val minutes = agreementStatus?.usedMinutes ?: 0L
                Text("Uso registrado hoy: ${minutes} min")
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val current = status
                val symbol = when (current) {
                    ParentalStatus.GREEN -> "🟢"
                    ParentalStatus.ORANGE -> "🟠"
                    ParentalStatus.RED -> "🔴"
                    ParentalStatus.WHITE, null -> "⚪"
                }
                val label = when (current) {
                    ParentalStatus.GREEN -> "PROTECCIÓN ACTIVA"
                    ParentalStatus.ORANGE -> "ATENCIÓN"
                    ParentalStatus.RED -> "LÍMITE ALCANZADO"
                    ParentalStatus.WHITE, null -> "DATOS INSUFICIENTES"
                }
                Text("$symbol $label", style = MaterialTheme.typography.titleLarge)
                Text("Los límites y pausas configurados por la familia se aplican en este dispositivo cuando los permisos necesarios están activos.")
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Transparencia", style = MaterialTheme.typography.titleMedium)
                Text("Famyrex no es una aplicación espía. La supervisión visible depende de los permisos de Android y de las reglas familiares configuradas.")
                Text("Si falta un permiso, el estado se muestra como ⚪ en lugar de fingir que todo está protegido.")
            }
        }
    }
}

private fun isSupervisedAccessibilityEnabled(context: Context): Boolean {
    val expected = "${context.packageName}/${FamyrexParentalAccessibilityService::class.java.name}"
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}
