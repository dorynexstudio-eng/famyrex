package com.famyrex.app

import android.content.Context
import android.content.pm.ApplicationInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Segunda capa de detección: compara el inventario visible para Android con
 * el último inventario guardado. El primer escaneo establece una línea base
 * y no genera una avalancha de alertas.
 */
object InstalledAppsReconciler {
    fun reconcile(context: Context) {
        val store = InstalledAppsSnapshotStore(context)
        val current = visiblePackages(context)
        if (current.isEmpty()) return

        if (!store.isInitialized()) {
            store.save(current)
            store.markInitialized()
            return
        }

        val previous = store.load()
        if (previous.isEmpty()) {
            store.save(current)
            return
        }

        val installed = current - previous
        val removed = previous - current
        installed.forEach { emit(context, it, installed = true) }
        removed.forEach { emit(context, it, installed = false) }
        store.save(current)
    }

    private fun visiblePackages(context: Context): Set<String> = runCatching {
        context.packageManager.getInstalledApplications(0)
            .asSequence()
            .map(ApplicationInfo::packageName)
            .filter { it != context.packageName }
            .toSet()
    }.getOrDefault(emptySet())

    private fun emit(context: Context, packageName: String, installed: Boolean) {
        val label = if (installed) {
            runCatching {
                context.packageManager.getApplicationInfo(packageName, 0)
                    .loadLabel(context.packageManager).toString().ifBlank { null }
            }.getOrNull()
        } else null

        val category = AppCategoryClassifier.classify(packageName, label)
        val now = System.currentTimeMillis()
        val action = if (installed) "instalada" else "desinstalada"
        val alert = SmartAlert(
            id = "app_reconcile_${if (installed) "installed" else "removed"}_${packageName}_$now",
            type = if (installed) AlertType.APP_INSTALLED else AlertType.APP_UNINSTALLED,
            severity = if (installed && category.isSensitive) AlertSeverity.IMPORTANT else AlertSeverity.ATTENTION,
            title = if (installed) "Nueva aplicación detectada" else "Aplicación eliminada detectada",
            message = "Se ha $action: ${label ?: packageName}${if (category != AppCategory.UNKNOWN) " (categoría: ${category.label})" else ""}",
            date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(now)),
            packageName = packageName
        )
        if (AlertStore(context).appendIfNew(alert)) {
            FamyrexNotificationManager.notify(context, alert)
        }
    }
}
