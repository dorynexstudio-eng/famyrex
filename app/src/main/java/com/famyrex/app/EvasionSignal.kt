package com.famyrex.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import java.io.File

enum class SignalConfidence { LOW, MEDIUM, HIGH }

data class EvasionSignal(
    val key: String,
    val title: String,
    val detail: String,
    val confidence: SignalConfidence
)

object EvasionSignalChecker {
    fun check(context: Context): List<EvasionSignal> = buildList {
        if (isVpnActive(context)) {
            add(EvasionSignal(
                key = "vpn_active",
                title = "VPN activa",
                detail = "Se detecta una conexión VPN activa. Esto puede modificar cómo se aplican algunas reglas de red.",
                confidence = SignalConfidence.HIGH
            ))
        }

        val developerOptions = runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        }.getOrDefault(false)
        if (developerOptions) {
            add(EvasionSignal(
                key = "developer_options",
                title = "Opciones de desarrollador activadas",
                detail = "Las opciones de desarrollador están activadas. Por sí solas no indican una evasión.",
                confidence = SignalConfidence.MEDIUM
            ))
        }

        val adbEnabled = runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        }.getOrDefault(false)
        if (adbEnabled) {
            add(EvasionSignal(
                key = "adb_enabled",
                title = "Depuración USB activa",
                detail = "La depuración USB está activa. Es una señal de configuración avanzada, no una prueba de evasión.",
                confidence = SignalConfidence.MEDIUM
            ))
        }

        if (looksRooted()) {
            add(EvasionSignal(
                key = "root_indicators",
                title = "Indicadores de modificación del sistema",
                detail = "Se han encontrado indicios técnicos compatibles con un sistema modificado. No se puede confirmar root solo con esta señal.",
                confidence = SignalConfidence.LOW
            ))
        }
    }

    private fun isVpnActive(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        return cm.allNetworks.any { network ->
            cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }
    }

    private fun looksRooted(): Boolean {
        val tags = Build.TAGS.orEmpty()
        if (tags.contains("test-keys", ignoreCase = true)) return true
        val paths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/system/app/Superuser.apk", "/system/app/SuperSU.apk",
            "/system/xbin/busybox"
        )
        return paths.any { File(it).exists() }
    }
}
