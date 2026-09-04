package com.famyrex.app

data class ProtectionSettings(
    val nightStartMinutes: Int = 0,
    val nightEndMinutes: Int = 360,
    val nightMinutesThreshold: Long = 30,
    val dailyMinutesThreshold: Long = 240,
    val appSpikePercent: Int = 60,
    val sensitivity: Int = 2
)
