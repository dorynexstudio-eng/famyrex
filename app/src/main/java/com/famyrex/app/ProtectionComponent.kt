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
        val parentalConfig = ParentalControlStore(context).load()
        val parentalEnforcementRequired = context.isParentalEnforcementRequiredForComponents(parentalConfig)
        val parentalReasons = health.reasons.filter {
            it.contains("control parental") || it.contains("acceso al uso de aplicaciones")
        }
        val parentalReady = parentalEnforcementRequired && parentalReasons.isEmpty()

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
                "parental_controls", "Control parental de aplicaciones",
                when {
                    !parentalEnforcementRequired -> ProtectionComponentStatus.NOT_CONFIGURED
                    parentalReady -> ProtectionComponentStatus.ACTIVE
                    else -> ProtectionComponentStatus.DEGRADED
                },
                when {
                    !parentalEnforcementRequired -> "No hay límites, restricciones o pausas parentales activos que requieran este componente."
                    parentalReady -> "El servicio de accesibilidad y el acceso al uso de aplicaciones necesarios para aplicar las reglas están disponibles."
                    else -> "El control parental no está completamente disponible; revisa la accesibilidad y el acceso al uso de aplicaciones."
                }
            ),
            ProtectionComponent(
                "periodic_check", "Comprobación periódica", ProtectionComponentStatus.ACTIVE,
                "Famyrex ejecuta comprobaciones periódicas de salud y señales disponibles."
            )
        )
    }

    private fun android.content.Context.isParentalEnforcementRequiredForComponents(config: ParentalControlConfig): Boolean {
        if (FamilyStore(this).appMode() != FamyrexAppMode.SUPERVISED) return false
        if (!AccessibilityConsentStore(this).isAccepted()) return false

        return config.screenTimeLimit?.enabled == true ||
            config.appRestrictions.isNotEmpty() ||
            config.pauseSchedules.any { it.enabled }
    }
}
