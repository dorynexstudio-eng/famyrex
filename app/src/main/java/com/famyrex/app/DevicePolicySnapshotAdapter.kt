package com.famyrex.app

/**
 * Bridges the canonical local parental configuration and the transport-oriented
 * DevicePolicySnapshot used by the future family sync layer.
 */
object DevicePolicySnapshotAdapter {
    fun fromLocalConfig(
        config: ParentalControlConfig,
        deviceId: String,
        revision: Long = 0L,
        webPolicyVersion: Long = 0L
    ): DevicePolicySnapshot = DevicePolicySnapshot(
        deviceId = deviceId,
        dailyLimitMinutes = config.screenTimeLimit?.takeIf { it.enabled }?.dailyMinutes,
        bedtimeStartMinutes = config.pauseSchedules.firstOrNull { it.enabled }?.startMinuteOfDay,
        bedtimeEndMinutes = config.pauseSchedules.firstOrNull { it.enabled }?.endMinuteOfDay,
        apps = config.appRestrictions.map { restriction ->
            AppPolicy(
                packageName = restriction.packageName,
                displayName = restriction.packageName,
                blocked = restriction.blocked,
                dailyLimitMinutes = restriction.dailyMinutes
            )
        },
        webPolicyVersion = webPolicyVersion,
        revision = revision
    )

    /**
     * Applies only the policy fields currently representable by the local store.
     * Transport-only metadata and unsupported web controls are intentionally ignored.
     */
    fun toLocalConfig(snapshot: DevicePolicySnapshot): ParentalControlConfig = ParentalControlConfig(
        screenTimeLimit = snapshot.dailyLimitMinutes?.let { ScreenTimeLimit(it) },
        pauseSchedules = if (snapshot.bedtimeStartMinutes != null && snapshot.bedtimeEndMinutes != null) {
            listOf(
                PauseSchedule(
                    startMinuteOfDay = snapshot.bedtimeStartMinutes,
                    endMinuteOfDay = snapshot.bedtimeEndMinutes
                )
            )
        } else {
            emptyList()
        },
        appRestrictions = snapshot.apps.map { app ->
            AppRestriction(
                packageName = app.packageName,
                dailyMinutes = app.dailyLimitMinutes,
                blocked = app.blocked
            )
        }
    )
}
