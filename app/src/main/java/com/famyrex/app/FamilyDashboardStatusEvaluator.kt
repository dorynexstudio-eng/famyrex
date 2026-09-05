package com.famyrex.app

/**
 * Keeps the dashboard headline conservative: incomplete intelligence can never
 * be presented as full protection.
 */
object FamilyDashboardStatusEvaluator {
    fun label(
        configured: Boolean,
        intelligenceStatus: ParentalStatus?,
        degradedComponents: Int,
        notConfiguredComponents: Int
    ): String {
        if (!configured) return "⚪ PENDIENTE"
        if (intelligenceStatus == ParentalStatus.RED || degradedComponents > 0) {
            return "🔴 PROTECCIÓN EN RIESGO"
        }
        if (intelligenceStatus == ParentalStatus.ORANGE) {
            return "🟠 PROTECCIÓN PARCIAL"
        }
        if (intelligenceStatus == ParentalStatus.WHITE || intelligenceStatus == null || notConfiguredComponents > 0) {
            return "⚪ DATOS INSUFICIENTES"
        }
        return "🟢 PROTEGIDO"
    }
}
