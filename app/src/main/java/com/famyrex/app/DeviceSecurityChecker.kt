package com.famyrex.app

import android.Manifest
import android.app.AppOpsManager
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.UserManager
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class DeviceSecurityChecker(private val context: Context) {

    fun check(): DeviceSecuritySnapshot {
        val pm = context.packageManager
        val installedCount = runCatching {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(PackageManager.GET_META_DATA).size
        }.getOrDefault(0)

        val usage = hasUsageAccess()
        val foregroundLocation =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        val backgroundLocation =
            Build.VERSION.SDK_INT < 29 ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val keyguard = context.getSystemService(KeyguardManager::class.java)
        val secureLock = keyguard?.isKeyguardSecure == true

        // Android intentionally does not expose a universal "developer options enabled"
        // API for ordinary apps. Keep this signal nullable rather than guessing.
        val developerOptions: Boolean? = null

        val reasons = mutableListOf<String>()
        if (!secureLock) reasons += "No se detecta un bloqueo de pantalla seguro."
        if (!usage) reasons += "El acceso a estadísticas de uso no está concedido."
        if (!foregroundLocation && foregroundLocationWasExpected()) {
            reasons += "La ubicación en primer plano no está concedida."
        }
        if (Build.VERSION.SDK_INT >= 29 && foregroundLocation && !backgroundLocation) {
            reasons += "La ubicación en segundo plano no está concedida; las zonas pueden no funcionar de forma continua."
        }

        val level = when {
            !secureLock -> SecurityLevel.ATTENTION
            reasons.size >= 2 -> SecurityLevel.ATTENTION
            else -> SecurityLevel.GOOD
        }

        return DeviceSecuritySnapshot(
            timestampMs = System.currentTimeMillis(),
            androidVersion = Build.VERSION.RELEASE ?: "desconocida",
            sdkInt = Build.VERSION.SDK_INT,
            isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
            hasSecureLockScreen = secureLock,
            isDeveloperOptionsEnabled = developerOptions,
            installedAppCount = installedCount,
            usageAccessGranted = usage,
            foregroundLocationGranted = foregroundLocation,
            backgroundLocationGranted = backgroundLocation,
            securityLevel = level,
            reasons = reasons
        )
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun foregroundLocationWasExpected(): Boolean =
        FamilyZoneStore(context).load().any { it.enabled }
}
