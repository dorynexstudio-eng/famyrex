package com.famyrex.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName == context.packageName) return

        // Android emits these while replacing/updating an app. Do not report an update as
        // an uninstall/install pair because that would create false alarms.
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return

        val installed = intent.action == Intent.ACTION_PACKAGE_ADDED
        val removed = intent.action == Intent.ACTION_PACKAGE_REMOVED
        if (!installed && !removed) return

        val label = resolveLabel(context, packageName)
        val category = AppCategoryClassifier.classify(packageName, label)
        val now = System.currentTimeMillis()
        val actionText = if (installed) "instalada" else "desinstalada"
        val title = if (installed) "Nueva aplicación instalada" else "Aplicación desinstalada"
        val detail = buildString {
            append("Se ha $actionText: ")
            append(label ?: packageName)
            if (category != AppCategory.UNKNOWN) {
                append(" (categoría: ")
                append(category.label)
                append(")")
            }
        }

        val alert = SmartAlert(
            id = "app_change_${if (installed) "installed" else "removed"}_${packageName}_$now",
            type = if (installed) AlertType.APP_INSTALLED else AlertType.APP_UNINSTALLED,
            severity = if (installed && category.isSensitive) AlertSeverity.IMPORTANT else AlertSeverity.ATTENTION,
            title = title,
            message = detail,
            date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(now)),
            packageName = packageName
        )

        if (AlertStore(context).appendIfNew(alert)) {
            FamyrexNotificationManager.notify(context, alert)
        }
    }

    private fun resolveLabel(context: Context, packageName: String): String? =
        runCatching {
            context.packageManager.getApplicationInfo(packageName, 0)
                .loadLabel(context.packageManager)
                .toString()
                .ifBlank { null }
        }.getOrNull()
}

enum class AppCategory(val label: String, val isSensitive: Boolean) {
    SOCIAL("social", true),
    BROWSER("navegador", false),
    VPN("VPN", true),
    CLONER_OR_VAULT("clonador/bóveda", true),
    GAMES("juegos", false),
    UNKNOWN("desconocida", false)
}

object AppCategoryClassifier {
    fun classify(packageName: String, label: String?): AppCategory {
        val value = "$packageName ${label.orEmpty()}".lowercase(Locale.ROOT)
        return when {
            listOf("vpn", "wireguard", "openvpn", "tailscale").any(value::contains) -> AppCategory.VPN
            listOf("clone", "cloner", "parallel space", "dual space", "app vault", "vault", "hide apps").any(value::contains) -> AppCategory.CLONER_OR_VAULT
            listOf("instagram", "tiktok", "snapchat", "discord", "telegram", "messenger", "facebook").any(value::contains) -> AppCategory.SOCIAL
            listOf("chrome", "firefox", "brave", "edge", "opera", "browser").any(value::contains) -> AppCategory.BROWSER
            listOf("game", "games", "play games").any(value::contains) -> AppCategory.GAMES
            else -> AppCategory.UNKNOWN
        }
    }
}
