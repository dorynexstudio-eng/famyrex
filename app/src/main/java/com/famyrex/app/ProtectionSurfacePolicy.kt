package com.famyrex.app

/**
 * Decides which foreground packages may be evaluated by the parental guard.
 * Famyrex must never cover its own UI, Android settings, or the launcher: those
 * surfaces are required to review, change, or recover protection settings.
 */
object ProtectionSurfacePolicy {
    private val settingsPackages = setOf(
        "com.android.settings",
        "com.google.android.settings"
    )

    fun shouldEvaluate(
        targetPackage: String,
        ownPackage: String,
        launcherPackage: String?
    ): Boolean {
        if (targetPackage.isBlank()) return false
        if (targetPackage == ownPackage) return false
        if (launcherPackage != null && targetPackage == launcherPackage) return false
        if (targetPackage in settingsPackages) return false
        return true
    }
}
