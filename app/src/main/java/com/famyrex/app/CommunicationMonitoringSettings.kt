package com.famyrex.app

import android.content.Context
import android.content.Intent
import android.provider.Settings

object CommunicationMonitoringSettings {
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
        return enabled.split(":").any { component ->
            component.equals("${context.packageName}/${FamyrexNotificationListenerService::class.java.name}", ignoreCase = true)
        }
    }

    fun openSystemSettings(context: Context) {
        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
