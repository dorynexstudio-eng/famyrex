package com.famyrex.app

/** Explicaciones deterministas basadas exclusivamente en señales ya observadas. */
object FamilyIntelligenceExplanation {
    fun explain(
        summary: FamilyIntelligenceSummary,
        trend: FamilyUsageTrend?
    ): String {
        if (summary.parentalStatus == ParentalStatus.WHITE) {
            return "No puedo valorar completamente la situación todavía: faltan datos o permisos de uso y protección."
        }
        if (summary.communicationAlertCount > 0) {
            return "Hay ${summary.communicationAlertCount} señal${if (summary.communicationAlertCount == 1) "" else "es"} de comunicación pendiente${if (summary.communicationAlertCount == 1) "" else "s"} de revisión."
        }
        return when (summary.parentalStatus) {
            ParentalStatus.RED -> "El uso de pantalla ha alcanzado o superado un límite configurado. Conviene revisar el límite y el uso de hoy."
            ParentalStatus.ORANGE -> "El uso de pantalla se acerca a un límite configurado. Es un buen momento para revisar cómo va el día."
            ParentalStatus.GREEN -> when (trend?.direction) {
                FamilyUsageTrendDirection.INCREASING -> "La protección está en orden, aunque el uso de hoy va por encima de la referencia reciente."
                FamilyUsageTrendDirection.DECREASING -> "La protección está en orden y el uso de hoy está por debajo de la referencia reciente."
                else -> "La protección está en orden y no hay señales que requieran acción inmediata."
            }
            ParentalStatus.WHITE -> "No hay datos suficientes."
        }
    }
}
