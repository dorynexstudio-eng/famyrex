package com.famyrex.app

/** Applies remotely issued control commands to the locally enforceable policy model. */
object FamilyControlCommandApplier {
    fun apply(command: FamilyControlCommand, current: ParentalControlConfig): CommandApplicationResult = when (command.action) {
        FamilyControlAction.SET_DAILY_LIMIT -> {
            val minutes = command.value?.toIntOrNull()
                ?: return CommandApplicationResult.Rejected("El límite diario no es válido.")
            if (!ApplicationPolicyEngine.validateLimit(minutes)) {
                return CommandApplicationResult.Rejected("El límite diario debe estar entre 1 y 1440 minutos.")
            }
            CommandApplicationResult.Applied(current.copy(screenTimeLimit = ScreenTimeLimit(minutes)))
        }
        FamilyControlAction.SET_SCHEDULE -> {
            val parts = command.value?.split('-')
            if (parts == null || parts.size != 2) return CommandApplicationResult.Rejected("El horario debe usar inicio-fin en minutos.")
            val start = parts[0].toIntOrNull()
            val end = parts[1].toIntOrNull()
            if (start == null || end == null || start !in 0..1439 || end !in 0..1439) {
                return CommandApplicationResult.Rejected("El horario debe estar entre 0 y 1439 minutos.")
            }
            CommandApplicationResult.Applied(current.copy(pauseSchedules = listOf(PauseSchedule(start, end))))
        }
        FamilyControlAction.BLOCK_APP, FamilyControlAction.ALLOW_APP -> {
            val packageName = command.value?.trim().orEmpty()
            if (!isPackageName(packageName)) return CommandApplicationResult.Rejected("El paquete de la aplicación no es válido.")
            val existing = current.appRestrictions.firstOrNull { it.packageName == packageName }
            val replacement = (existing ?: AppRestriction(packageName)).copy(blocked = command.action == FamilyControlAction.BLOCK_APP)
            CommandApplicationResult.Applied(current.copy(appRestrictions = upsert(current.appRestrictions, replacement)))
        }
        FamilyControlAction.SET_APP_LIMIT -> {
            val parts = command.value?.split(':', limit = 2)
            if (parts == null || parts.size != 2) return CommandApplicationResult.Rejected("El límite de aplicación debe usar paquete:minutos.")
            val packageName = parts[0].trim()
            val minutes = parts[1].trim().toIntOrNull()
            if (!isPackageName(packageName) || !ApplicationPolicyEngine.validateLimit(minutes)) {
                return CommandApplicationResult.Rejected("El límite de aplicación no es válido.")
            }
            val existing = current.appRestrictions.firstOrNull { it.packageName == packageName }
            val replacement = (existing ?: AppRestriction(packageName)).copy(dailyMinutes = minutes)
            CommandApplicationResult.Applied(current.copy(appRestrictions = upsert(current.appRestrictions, replacement)))
        }
        FamilyControlAction.SYNC_POLICY -> CommandApplicationResult.Unsupported("SYNC_POLICY requiere un snapshot de política.")
        else -> CommandApplicationResult.Unsupported("La acción ${command.action.name} todavía no tiene ejecución local segura.")
    }

    private fun upsert(items: List<AppRestriction>, replacement: AppRestriction): List<AppRestriction> =
        items.filterNot { it.packageName == replacement.packageName } + replacement

    private fun isPackageName(value: String): Boolean = value.length in 1..255 &&
        value.split('.').all { segment ->
            segment.isNotEmpty() && segment[0].isLetter() && segment.all { it.isLetterOrDigit() || it == '_' }
        }
}

sealed class CommandApplicationResult {
    data class Applied(val config: ParentalControlConfig) : CommandApplicationResult()
    data class Rejected(val reason: String) : CommandApplicationResult()
    data class Unsupported(val reason: String) : CommandApplicationResult()
}
