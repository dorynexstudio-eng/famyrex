package com.famyrex.app

enum class ProtectionComponentStatus { ACTIVE, DEGRADED, NOT_CONFIGURED }

data class ProtectionComponent(
    val key: String,
    val name: String,
    val status: ProtectionComponentStatus,
    val detail: String
)

object ProtectionComponentChecker {
    fun check(context: android.content.Context): List<ProtectionComponent> {
        val zones = FamilyZoneStore(context).load().any { it.enabled }
        val health = ProtectionHealthChecker.check(context)
        val locationReady = health.reasons.none { it.contains("ubicación necesaria") || it.contains("segundo plano") }
        val notificationsReady = health.reasons.none { it.contains("notificaciones") }
        val listenerReady = CommunicationMonitoringSettings.isNotificationListenerEnabled(context)

        return listOf(
            ProtectionComponent(
                "notifications", "Avisos de emergencia", if (notificationsReady) ProtectionComponentStatus.ACTIVE else ProtectionComponentStatus.DEGRADED,
                if (notificationsReady) "Famyrex puede mostrar avisos locales." else "Las notificaciones no están disponibles; un aviso local podría no llegar."
            ),
            ProtectionComponent(
                "location", "Ubicación", when { !zones -> ProtectionComponentStatus.NOT_CONFIGURED; locationReady -> ProtectionComponentStatus.ACTIVE; else -> ProtectionComponentStatus.DEGRADED },
                when { !zones -> "No hay geozonas activas que vigilar."; locationReady -> "Los permisos necesarios para las geozonas están disponibles."; else -> "Revisa los permisos de ubicación para mantener la vigilancia de las geozonas." }
            ),
            ProtectionComponent(
                "geofences", "Geozonas", if (zones && locationReady) ProtectionComponentStatus.ACTIVE else ProtectionComponentStatus.NOT_CONFIGURED,
                if (zones && locationReady) "Las geozonas configuradas pueden vigilarse." else "Configura una geozona y los permisos de ubicación para activarla."
            ),
            ProtectionComponent(
                "communications", "Señales de comunicación", if (listenerReady) ProtectionComponentStatus.ACTIVE else ProtectionComponentStatus.NOT_CONFIGURED,
                if (listenerReady) "El análisis autorizado de notificaciones está disponible y no conserva el texto original." else "La supervisión transparente de notificaciones no está activada."
            ),
            ProtectionComponent(
                "periodic_check", "Comprobación periódica", ProtectionComponentStatus.ACTIVE,
                "Famyrex ejecuta comprobaciones periódicas de salud y señales disponibles."
            )
        )
    }
}
