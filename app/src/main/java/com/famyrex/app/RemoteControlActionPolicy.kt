package com.famyrex.app

/**
 * Central allow-list for commands that the current child-device runtime can execute safely.
 * Keeping this explicit prevents the parent UI/backend from implying support that is not local.
 */
object RemoteControlActionPolicy {
    private val supported = setOf(
        FamilyControlAction.LOCK_DEVICE,
        FamilyControlAction.UNLOCK_DEVICE,
        FamilyControlAction.GRANT_EXTRA_TIME,
        FamilyControlAction.SET_DAILY_LIMIT,
        FamilyControlAction.SET_SCHEDULE,
        FamilyControlAction.BLOCK_APP,
        FamilyControlAction.ALLOW_APP,
        FamilyControlAction.SET_APP_LIMIT,
        FamilyControlAction.SYNC_POLICY
    )

    fun isSupported(action: FamilyControlAction): Boolean = action in supported

    fun supportedActions(): Set<FamilyControlAction> = supported
}
