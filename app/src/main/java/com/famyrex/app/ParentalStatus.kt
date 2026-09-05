package com.famyrex.app

/** Estados visibles del centro de protección parental. */
enum class ParentalStatus(val icon: String, val label: String) {
    GREEN("🟢", "En orden"),
    ORANGE("🟠", "Revisar"),
    RED("🔴", "Acción necesaria"),
    WHITE("⚪", "Sin datos suficientes")
}

/**
 * Calcula un estado conservador: la ausencia de datos nunca se presenta como
 * un estado verde. Los umbrales cercanos al límite pasan a revisión.
 */
object ParentalStatusEvaluator {
    fun overall(
        usageAccess: Boolean,
        accessibilityEnabled: Boolean,
        totalUsageMinutes: Long?,
        screenTimeLimit: ScreenTimeLimit?
    ): ParentalStatus {
        if (!usageAccess || !accessibilityEnabled || totalUsageMinutes == null) {
            return ParentalStatus.WHITE
        }

        val limit = screenTimeLimit?.takeIf { it.enabled }?.dailyMinutes ?: return ParentalStatus.GREEN
        if (totalUsageMinutes >= limit) return ParentalStatus.RED
        if (totalUsageMinutes >= (limit * 0.8f).toInt()) return ParentalStatus.ORANGE
        return ParentalStatus.GREEN
    }

    fun app(
        usageAvailable: Boolean,
        usedMinutes: Long?,
        restriction: AppRestriction?
    ): ParentalStatus {
        if (!usageAvailable || usedMinutes == null) return ParentalStatus.WHITE
        if (restriction?.blocked == true) return ParentalStatus.RED
        val limit = restriction?.dailyMinutes ?: return ParentalStatus.GREEN
        if (usedMinutes >= limit) return ParentalStatus.RED
        if (usedMinutes >= (limit * 0.8f).toInt()) return ParentalStatus.ORANGE
        return ParentalStatus.GREEN
    }
}
