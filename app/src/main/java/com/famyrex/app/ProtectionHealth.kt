package com.famyrex.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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

        return ProtectionHealth(
            active = reasons.isEmpty(),
            reasons = reasons,
            checkedAtMs = System.currentTimeMillis()
        )
    }
}
