package com.famyrex.app

enum class AlertSeverity { INFO, ATTENTION, IMPORTANT }

enum class AlertType { NIGHT_USE, APP_SPIKE, DAILY_LIMIT, PATTERN_CHANGE }

data class SmartAlert(
    val id: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val title: String,
    val message: String,
    val date: String,
    val packageName: String? = null
)
