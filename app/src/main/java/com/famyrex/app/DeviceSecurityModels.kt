package com.famyrex.app

enum class SecurityLevel { GOOD, ATTENTION, ELEVATED }

data class DeviceSecuritySnapshot(
    val timestampMs: Long,
    val androidVersion: String,
    val sdkInt: Int,
    val isDebuggable: Boolean,
    val hasSecureLockScreen: Boolean,
    val isDeveloperOptionsEnabled: Boolean?,
    val installedAppCount: Int,
    val usageAccessGranted: Boolean,
    val foregroundLocationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
    val securityLevel: SecurityLevel,
    val reasons: List<String>
)
