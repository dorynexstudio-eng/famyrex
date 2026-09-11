package com.famyrex.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.util.Calendar

/** Guardia opcional de aplicación para aplicar restricciones parentales locales. */
class FamyrexParentalAccessibilityService : AccessibilityService() {
    private var blockingView: TextView? = null
    private var blockedPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isSupervisedDeviceReady()) {
            removeBlockingOverlay()
            return
        }
        if (!AccessibilityConsentStore(this).isAccepted()) {
            removeBlockingOverlay()
            return
        }

        val targetPackage = event?.packageName?.toString()?.trim()?.takeIf { it.isNotBlank() } ?: run {
            removeBlockingOverlay()
            return
        }
        val launcherPackage = resolveLauncherPackage()

        // Recovery/control surfaces must stay usable even while the emergency lock is active.
        if (!ProtectionSurfacePolicy.shouldEvaluate(targetPackage, packageName, launcherPackage)) {
            removeBlockingOverlay()
            return
        }

        val identity = FamilyDeviceIdentityStore(applicationContext).current() ?: run {
            removeBlockingOverlay()
            return
        }
        if (DeviceEmergencyLockStore(this).isLocked(identity.deviceId)) {
            showBlockingOverlay(targetPackage, listOf("El dispositivo está bloqueado temporalmente por un adulto autorizado."))
            return
        }
        val monitor = ParentalUsageMonitor(this)
        if (!monitor.hasUsageAccess()) {
            removeBlockingOverlay()
            return
        }

        val now = System.currentTimeMillis()
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val usage = monitor.queryUsage(startOfDay, now)
        val appUsed = usage.firstOrNull { it.packageName == targetPackage }?.totalTimeInForeground?.div(60_000L) ?: 0L
        val rawTotalUsed = usage.sumOf { it.totalTimeInForeground }.div(60_000L)
        val extraTime = ExtraTimeAllowanceStore(this).grantedMinutes(LocalDate.now())
        val totalUsed = (rawTotalUsed - extraTime).coerceAtLeast(0L)
        val result = ParentalPolicyEngine.evaluate(
            config = ParentalControlStore(this).load(),
            packageName = targetPackage,
            appUsedTodayMinutes = appUsed,
            totalScreenTodayMinutes = totalUsed,
            nowMs = now,
            appApprovals = AppApprovalStore(this)
        )

        if (result.restricted) showBlockingOverlay(targetPackage, result.reasons) else removeBlockingOverlay()
    }

    override fun onInterrupt() = removeBlockingOverlay()
    override fun onDestroy() { removeBlockingOverlay(); super.onDestroy() }

    private fun isSupervisedDeviceReady(): Boolean {
        val store = FamilyStore(applicationContext)
        if (store.appMode() != FamyrexAppMode.SUPERVISED) return false
        val identity = FamilyDeviceIdentityStore(applicationContext).current() ?: return false
        if (!identity.isSupervised) return false
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        return user.isAnonymous && identity.firebaseUid == user.uid
    }

    private fun resolveLauncherPackage(): String? = runCatching {
        packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
    }.getOrNull()

    private fun showBlockingOverlay(targetPackage: String, reasons: List<String>) {
        if (blockedPackage == targetPackage && blockingView != null) return
        removeBlockingOverlay()
        val view = TextView(this).apply {
            text = "Famyrex\n\nEsta aplicación está temporalmente restringida.\n\n${reasons.joinToString("\n")}"
            textSize = 20f; gravity = Gravity.CENTER; setPadding(48, 48, 48, 48); isClickable = true; isFocusable = true
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        getSystemService(WindowManager::class.java).addView(view, params)
        blockingView = view; blockedPackage = targetPackage
    }

    private fun removeBlockingOverlay() {
        blockingView?.let { runCatching { getSystemService(WindowManager::class.java).removeView(it) } }
        blockingView = null; blockedPackage = null
    }
}
