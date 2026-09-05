package com.famyrex.app

/**
 * Configuración local de controles parentales. No contiene contenido de chats
 * ni depende de un servidor remoto.
 */
data class ScreenTimeLimit(
    val dailyMinutes: Int,
    val enabled: Boolean = true
) {
    init {
        require(dailyMinutes in 1..1440) { "dailyMinutes must be between 1 and 1440" }
    }
}

data class PauseSchedule(
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val enabled: Boolean = true
) {
    init {
        require(startMinuteOfDay in 0..1439) { "startMinuteOfDay must be between 0 and 1439" }
        require(endMinuteOfDay in 0..1439) { "endMinuteOfDay must be between 0 and 1439" }
    }
}

data class AppRestriction(
    val packageName: String,
    val dailyMinutes: Int? = null,
    val blocked: Boolean = false
) {
    init {
        require(packageName.isNotBlank()) { "packageName must not be blank" }
        require(dailyMinutes == null || dailyMinutes in 1..1440) {
            "dailyMinutes must be null or between 1 and 1440"
        }
    }
}

data class ParentalControlConfig(
    val screenTimeLimit: ScreenTimeLimit? = null,
    val pauseSchedules: List<PauseSchedule> = emptyList(),
    val appRestrictions: List<AppRestriction> = emptyList()
)
