package com.famyrex.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.security.MessageDigest

object FamyrexNotificationManager {
    private const val CHANNEL_CRITICAL = "famyrex_critical"
    private const val CHANNEL_IMPORTANT = "famyrex_important"
    private const val CHANNEL_ATTENTION = "famyrex_attention"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_CRITICAL, "Famyrex · Críticas", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Avisos que requieren atención inmediata."
                },
                NotificationChannel(CHANNEL_IMPORTANT, "Famyrex · Importantes", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Avisos importantes de protección familiar."
                },
                NotificationChannel(CHANNEL_ATTENTION, "Famyrex · Atención", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Cambios que conviene revisar."
                }
            )
        )
    }

    fun notify(context: Context, alert: SmartAlert) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        ensureChannels(context)
        val channel = when (alert.severity) {
            AlertSeverity.IMPORTANT -> CHANNEL_CRITICAL
            AlertSeverity.ATTENTION -> CHANNEL_IMPORTANT
            AlertSeverity.INFO -> CHANNEL_ATTENTION
        }
        val priority = when (alert.severity) {
            AlertSeverity.IMPORTANT -> NotificationCompat.PRIORITY_MAX
            AlertSeverity.ATTENTION -> NotificationCompat.PRIORITY_HIGH
            AlertSeverity.INFO -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(priority)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(alert.id), notification)
    }

    internal fun notificationId(alertId: String): Int {
        val digest = MessageDigest.getInstance("SHA-256").digest(alertId.toByteArray(Charsets.UTF_8))
        return (digest[0].toInt() and 0xFF) or
            ((digest[1].toInt() and 0xFF) shl 8) or
            ((digest[2].toInt() and 0xFF) shl 16) or
            ((digest[3].toInt() and 0xFF) shl 24)
    }
}
