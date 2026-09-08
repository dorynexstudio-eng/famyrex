package com.famyrex.app

import java.util.Calendar

enum class ApplicationDecision {
    ALLOW,
    BLOCK,
    APPROVAL_REQUIRED,
    UNMANAGED
}

/**
 * Pure local policy evaluation. It deliberately does not assume intent or guilt:
 * it only answers whether a package is currently allowed by the configured policy.
 */
object ApplicationPolicyEngine {
    fun decision(
        policy: AppPolicy?,
        usedMinutesToday: Int,
        nowMs: Long = System.currentTimeMillis()
    ): ApplicationDecision {
        if (policy == null) return ApplicationDecision.UNMANAGED
        if (policy.blocked) return ApplicationDecision.BLOCK
        if (policy.dailyLimitMinutes != null && usedMinutesToday >= policy.dailyLimitMinutes) {
            return ApplicationDecision.BLOCK
        }
        if (policy.approvalRequired) return ApplicationDecision.APPROVAL_REQUIRED
        return ApplicationDecision.ALLOW
    }

    /** Evaluates installation policy separately from runtime-use policy. */
    fun installDecision(
        policy: AppPolicy?,
        source: AppInstallSource
    ): ApplicationDecision {
        if (policy == null) return ApplicationDecision.UNMANAGED
        if (!policy.installAllowed) return ApplicationDecision.BLOCK
        if (policy.approvalRequired && source != AppInstallSource.GOOGLE_PLAY) {
            return ApplicationDecision.APPROVAL_REQUIRED
        }
        return ApplicationDecision.ALLOW
    }

    fun isWithinSchedule(
        startMinute: Int,
        endMinute: Int,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (startMinute !in 0..1439 || endMinute !in 0..1439) return false
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMs }
        val minute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return if (startMinute <= endMinute) {
            minute in startMinute..endMinute
        } else {
            minute >= startMinute || minute <= endMinute
        }
    }

    fun validateLimit(minutes: Int?): Boolean = minutes == null || minutes in 1..1440
}
