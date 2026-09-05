package com.famyrex.app

/**
 * Entrada normalizada para el detector. El texto completo debe vivir únicamente
 * en memoria durante el análisis; este modelo no lo almacena de forma persistente.
 */
data class CommunicationObservation(
    val timestampMs: Long,
    val sourcePackage: String?,
    val normalizedText: String,
    val isIncoming: Boolean
)

/**
 * Detector conservador de secuencias. Analiza cada observación por separado para
 * no perder quién dijo qué ni convertir una coincidencia genérica en un riesgo.
 */
object CommunicationSignalDetector {
    private const val WINDOW_MS = 30 * 60 * 1000L
    private const val MAX_OBSERVATIONS = 40

    fun detect(observations: List<CommunicationObservation>): CommunicationRiskSummary {
        if (observations.isEmpty()) return CommunicationRiskEngine.evaluate(emptyList())

        val ordered = observations.sortedBy { it.timestampMs }
        val end = ordered.last().timestampMs
        val recent = ordered
            .filter { end - it.timestampMs in 0..WINDOW_MS }
            .takeLast(MAX_OBSERVATIONS)
            .map { it.copy(normalizedText = normalize(it.normalizedText)) }
            .filter { it.normalizedText.isNotBlank() }

        if (recent.isEmpty()) return CommunicationRiskEngine.evaluate(emptyList())

        val signals = mutableListOf<CommunicationRiskSignal>()

        fun add(type: CommunicationRiskType, confidence: RiskConfidence, reason: String, observation: CommunicationObservation) {
            signals += CommunicationRiskSignal(
                type = type,
                confidence = confidence,
                reason = reason,
                sourcePackage = observation.sourcePackage,
                timestampMs = observation.timestampMs
            )
        }

        val observationsWith = recent.map { observation ->
            ObservationFlags(
                observation = observation,
                personalInfo = containsAny(observation.normalizedText, PERSONAL_INFO),
                secrecy = containsAny(observation.normalizedText, SECRECY),
                meeting = containsAny(observation.normalizedText, MEETING),
                sexual = containsAny(observation.normalizedText, SEXUAL),
                threat = containsAny(observation.normalizedText, THREATS),
                bullying = containsAny(observation.normalizedText, BULLYING),
                selfHarm = containsAny(observation.normalizedText, SELF_HARM)
            )
        }

        val personal = observationsWith.filter { it.personalInfo }
        val secrets = observationsWith.filter { it.secrecy }
        val meetings = observationsWith.filter { it.meeting }
        val sexual = observationsWith.filter { it.sexual }
        val threats = observationsWith.filter { it.threat }
        val bullying = observationsWith.filter { it.bullying && it.observation.isIncoming }
        val selfHarm = observationsWith.filter { it.selfHarm }

        if (personal.isNotEmpty()) {
            add(
                CommunicationRiskType.GROOMING,
                RiskConfidence.LOW,
                "Se solicitó información personal; por sí sola no implica una situación de riesgo.",
                personal.first().observation
            )
        }

        if (personal.isNotEmpty() && (meetings.isNotEmpty() || secrets.isNotEmpty())) {
            val evidence = personal.first()
            add(
                CommunicationRiskType.GROOMING,
                if (secrets.isNotEmpty() && meetings.isNotEmpty()) RiskConfidence.MEDIUM else RiskConfidence.LOW,
                "Se combinaron referencias a información personal con secretismo o propuesta de encuentro.",
                evidence.observation
            )
        }

        if (sexual.isNotEmpty() && (personal.isNotEmpty() || meetings.isNotEmpty() || secrets.isNotEmpty())) {
            add(
                CommunicationRiskType.SEXUAL_REQUEST,
                RiskConfidence.HIGH,
                "Petición de contenido íntimo combinada con otra señal contextual.",
                sexual.last().observation
            )
        }

        if (sexual.isNotEmpty() && secrets.isNotEmpty()) {
            add(
                CommunicationRiskType.SECRET_KEEPING,
                RiskConfidence.HIGH,
                "Una petición de contenido íntimo aparece acompañada de una petición explícita de mantenerlo en secreto.",
                secrets.last().observation
            )
        }

        if (threats.size >= 2 || (threats.isNotEmpty() && secrets.isNotEmpty())) {
            add(
                CommunicationRiskType.THREAT,
                RiskConfidence.HIGH,
                "Se detectó una amenaza explícita con repetición o contexto adicional.",
                threats.last().observation
            )
        }

        val bullyingSources = bullying.groupBy { it.observation.sourcePackage }
        if (bullying.size >= 3 || bullyingSources.any { it.value.size >= 2 }) {
            add(
                CommunicationRiskType.BULLYING,
                RiskConfidence.MEDIUM,
                "Se detectó un patrón repetido de lenguaje hostil en mensajes entrantes.",
                bullying.last().observation
            )
        }

        if (selfHarm.size >= 2 || (selfHarm.isNotEmpty() && threats.isNotEmpty())) {
            add(
                CommunicationRiskType.SELF_HARM,
                RiskConfidence.HIGH,
                "Se detectaron expresiones de posible autolesión con repetición o contexto de amenaza.",
                selfHarm.last().observation
            )
        }

        if (secrets.size >= 2 && meetings.isNotEmpty()) {
            add(
                CommunicationRiskType.SECRET_KEEPING,
                RiskConfidence.MEDIUM,
                "Se detectó secretismo repetido junto con una propuesta de encuentro.",
                secrets.last().observation
            )
        }

        return CommunicationRiskEngine.evaluate(signals)
    }

    private data class ObservationFlags(
        val observation: CommunicationObservation,
        val personalInfo: Boolean,
        val secrecy: Boolean,
        val meeting: Boolean,
        val sexual: Boolean,
        val threat: Boolean,
        val bullying: Boolean,
        val selfHarm: Boolean
    )

    private fun normalize(text: String): String = text
        .lowercase()
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")
        .replace("ü", "u")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun containsAny(text: String, phrases: List<String>): Boolean = phrases.any(text::contains)

    private val PERSONAL_INFO = listOf(
        "direccion", "donde vives", "colegio", "instituto", "ubicacion", "telefono", "numero de telefono"
    )
    private val SECRECY = listOf(
        "no se lo digas", "que quede entre nosotros", "en secreto", "es nuestro secreto", "no digas nada", "que nadie se entere", "borra el mensaje"
    )
    private val MEETING = listOf(
        "quedamos a solas", "ven a verme", "ven solo", "ven sola", "nos vemos a escondidas", "donde nos vemos", "ven sin tus padres", "te recojo"
    )
    private val SEXUAL = listOf(
        "foto desnudo", "foto desnuda", "foto intima", "manda nudes", "manda nude", "desnudate", "contenido sexual"
    )
    private val THREATS = listOf(
        "te voy a hacer daño", "te hare daño", "te matare", "si hablas te", "vas a pagar", "te voy a encontrar"
    )
    private val BULLYING = listOf(
        "eres un inutil", "das asco", "nadie te quiere", "callate", "te vamos a echar", "todos se rien de ti"
    )
    private val SELF_HARM = listOf(
        "quiero hacerme daño", "quiero hacerme dano", "quiero desaparecer", "no quiero vivir", "me quiero morir", "hacerme daño", "hacerme dano"
    )
}
