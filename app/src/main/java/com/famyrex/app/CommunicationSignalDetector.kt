package com.famyrex.app

/**
 * Entrada normalizada para el detector. El texto completo debe vivir únicamente
 * en memoria durante el análisis; este modelo no lo almacena.
 */
data class CommunicationObservation(
    val timestampMs: Long,
    val sourcePackage: String?,
    val normalizedText: String,
    val isIncoming: Boolean
)

/**
 * Detector conservador de señales. Una coincidencia aislada nunca crea un incidente.
 * La correlación se hace sobre observaciones recientes y produce únicamente señales
 * estructuradas, sin conservar el contenido original.
 */
object CommunicationSignalDetector {
    private const val WINDOW_MS = 30 * 60 * 1000L

    fun detect(observations: List<CommunicationObservation>): CommunicationRiskSummary {
        val recent = observations
            .sortedBy { it.timestampMs }
            .takeLast(40)
            .filter { observations.lastOrNull()?.timestampMs?.minus(it.timestampMs) ?: 0L <= WINDOW_MS }

        if (recent.isEmpty()) return CommunicationRiskEngine.evaluate(emptyList())

        val signals = mutableListOf<CommunicationRiskSignal>()
        val text = recent.joinToString(" ") { it.normalizedText.lowercase() }
        val incoming = recent.count { it.isIncoming }

        fun add(type: CommunicationRiskType, confidence: RiskConfidence, reason: String) {
            signals += CommunicationRiskSignal(
                type = type,
                confidence = confidence,
                reason = reason,
                sourcePackage = recent.lastOrNull()?.sourcePackage,
                timestampMs = recent.lastOrNull()?.timestampMs ?: System.currentTimeMillis()
            )
        }

        val personalInfo = listOf("dirección", "donde vives", "dónde vives", "colegio", "instituto", "ubicación", "telefono", "teléfono").count { text.contains(it) }
        val secrecy = listOf("no se lo digas", "que quede entre nosotros", "en secreto", "no digas nada", "que nadie se entere").count { text.contains(it) }
        val meeting = listOf("quedar", "vernos", "ven solo", "ven sola", "encuentro", "te recojo").count { text.contains(it) }
        val sexual = listOf("foto desnuda", "nudes", "desnudo", "desnuda", "foto íntima", "foto intima").count { text.contains(it) }
        val threats = listOf("te voy a hacer daño", "te haré daño", "te matare", "te mataré", "amenaza", "si hablas").count { text.contains(it) }
        val harassment = listOf("idiota", "imbécil", "imbecil", "eres un inútil", "eres un inutil").count { text.contains(it) }
        val selfHarm = listOf("quiero morir", "no quiero vivir", "hacerme daño", "hacerme dano", "suicid").count { text.contains(it) }

        // Se exige contexto/repetición o combinación de señales antes de escalar.
        if (personalInfo > 0 && (meeting > 0 || secrecy > 0)) {
            add(CommunicationRiskType.GROOMING, RiskConfidence.MEDIUM,
                "Solicitud de información personal combinada con secretismo o propuesta de encuentro.")
        }
        if (sexual > 0 && (personalInfo > 0 || meeting > 0 || secrecy > 0)) {
            add(CommunicationRiskType.SEXUAL_REQUEST, RiskConfidence.HIGH,
                "Petición de contenido íntimo combinada con otra señal contextual.")
        }
        if (threats > 0 && recent.size >= 2) {
            add(CommunicationRiskType.THREAT, RiskConfidence.MEDIUM,
                "Señales de amenaza dentro de una secuencia de comunicación.")
        }
        if (harassment >= 2 && incoming >= 2) {
            add(CommunicationRiskType.BULLYING, RiskConfidence.MEDIUM,
                "Patrón repetido de lenguaje hostil en varias observaciones.")
        }
        if (selfHarm > 0 && recent.size >= 2) {
            add(CommunicationRiskType.SELF_HARM, RiskConfidence.MEDIUM,
                "Señales de posible autolesión dentro de un contexto de comunicación.")
        }
        if (secrecy >= 2) {
            add(CommunicationRiskType.SECRET_KEEPING, RiskConfidence.MEDIUM,
                "Petición repetida de mantener la comunicación en secreto.")
        }

        return CommunicationRiskEngine.evaluate(signals)
    }
}
