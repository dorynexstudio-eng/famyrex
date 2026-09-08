package com.famyrex.app

/** Applies a complete device policy snapshot through the existing local policy store. */
object FamilyControlPolicySync {
    fun apply(context: android.content.Context, snapshot: DevicePolicySnapshot): FamilyControlReceipt {
        require(snapshot.deviceId.isNotBlank())
        require(snapshot.revision >= 0L)
        snapshot.apps.forEach { policy ->
            require(policy.packageName.isNotBlank())
            require(policy.dailyLimitMinutes == null || policy.dailyLimitMinutes in 1..1440)
        }

        val adapter = DevicePolicySnapshotAdapter(context)
        val result = adapter.apply(snapshot)
        return FamilyControlReceipt(
            commandId = "policy-${snapshot.deviceId}-${snapshot.revision}",
            action = FamilyControlAction.SYNC_POLICY,
            acceptedAtMs = System.currentTimeMillis(),
            completedAtMs = System.currentTimeMillis(),
            success = result.success,
            reason = result.reason
        )
    }
}
