package com.famyrex.app

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat

/** Estado real de la capacidad de vigilancia local. */
data class ProtectionHealth(
    val active: Boolean,
    val reasons: List<String>,
    val checkedAtMs: Long
)

object ProtectionHealthChecker {
    fun check(context: Context): ProtectionHealth {
        val reasons = mutableListOf<String>()
        val zones = FamilyZoneStore(context).load().any { it.enabled }

        val locationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (zones && !locationGranted) {
            reasons += "La ubicación necesaria para las geozonas no está disponible."
        }

        if (zones && Build.VERSION.SDK_INT >= 29 &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            reasons += "La ubicación en segundo plano no está disponible; las geozonas pueden dejar de vigilarse."
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            reasons += "Las notificaciones están bloqueadas; Famyrex no puede garantizar avisos locales."
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && notificationManager != null &&
            notificationManager.areNotificationsEnabled().not()) {
            if (reasons.none { it.contains("notificaciones") }) {
                reasons += "Las notificaciones del sistema están desactivadas."
            }
        }

        val parentalConfig = ParentalControlStore(context).load()
        if (context.isParentalEnforcementRequired(parentalConfig) &&
            !isParentalAccessibilityServiceEnabled(context)) {
            reasons += "El control parental de aplicaciones no está disponible; el servicio de accesibilidad está desactivado."
        }

        return ProtectionHealth(
            active = reasons.isEmpty(),
            reasons = reasons,
            checkedAtMs = System.currentTimeMillis()
        )
    }

    private fun Context.isParentalEnforcementRequired(config: ParentalControlConfig): Boolean {
        if (FamilyStore(this).appMode() != FamyrexAppMode.SUPERVISED) return false
        if (!AccessibilityConsentStore(this).isAccepted()) return false

        val screenTimeEnabled = config.screenTimeLimit?.enabled == true
        return screenTimeEnabled || config.appRestrictions.isNotEmpty() || config.pauseSchedules.any { it.enabled }
    }

    private fun isParentalAccessibilityServiceEnabled(context: Context): Boolean = runCatching {
        val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
        manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { service ->
                val info = service.resolveInfo?.serviceInfo ?: return@any false
                info.packageName == context.packageName &&
                    info.name == FamyrexParentalAccessibilityService::class.java.name
            }
    }.getOrDefault(false)
}
