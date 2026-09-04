package com.famyrex.app

/**
 * Analizador local y conservador de una notificación individual.
 * No persiste el texto original. Devuelve únicamente señales estructuradas.
 *
 * Una coincidencia aislada no se considera una acusación: el motor superior
 * debe correlacionar señales y contexto antes de generar una alerta.
 */
object NotificationRiskAnalyzer {
    fun analyze(text: String, nowMs: Long = System.currentTimeMillis()): List<CommunicationRiskSignal> {
        val normalized = normalize(text)
        if (normalized.isBlank()) return emptyList()

        val signals = mutableListOf<CommunicationRiskSignal>()

        val personalInfo = containsAny(normalized, listOf(
            "dime tu direccion", "dime donde vives", "tu direccion",
            "tu colegio", "tu instituto", "tu escuela", "pásame tu ubicacion",
            "pasame tu ubicacion", "manda tu ubicacion", "donde estudias"
        ))
        val secret = containsAny(normalized, listOf(
            "no se lo digas a nadie", "que quede entre nosotros", "es nuestro secreto",
            "no le digas a tus padres", "no se lo cuentes a tus padres", "borra el mensaje"
        ))
        val meeting = containsAny(normalized, listOf(
            "quedamos a solas", "ven a verme", "ven solo", "ven sola",
            "nos vemos a escondidas", "donde nos vemos", "ven sin tus padres"
        ))
        val sexual = containsAny(normalized, listOf(
            "foto desnudo", "foto desnuda", "foto intima", "manda nudes",
            "manda nude", "desnudate", "contenido sexual"
        ))
        val threat = containsAny(normalized, listOf(
            "te voy a hacer daño", "te hare daño", "te matare", "si hablas te",
            "vas a pagar", "te voy a encontrar"
        ))
        val bullying = containsAny(normalized, listOf(
            "eres un inutil", "das asco", "nadie te quiere", "callate",
            "te vamos a echar", "todos se rien de ti"
        ))
        val selfHarm = containsAny(normalized, listOf(
            "quiero hacerme daño", "quiero hacerme dano", "quiero desaparecer",
            "no quiero vivir", "me quiero morir", "hacerme daño", "hacerme dano"
        ))

        if (personalInfo) signals += signal(
            CommunicationRiskType.GROOMING, RiskConfidence.LOW,
            "Se detectó una referencia a información personal.", nowMs
        )
        if (secret) signals += signal(
            CommunicationRiskType.SECRET_KEEPING, RiskConfidence.MEDIUM,
            "Se detectó una petición de mantener la comunicación en secreto.", nowMs
        )
        if (meeting) signals += signal(
            CommunicationRiskType.GROOMING, RiskConfidence.LOW,
            "Se detectó una propuesta de encuentro o contacto fuera del entorno habitual.", nowMs
        )
        if (sexual) signals += signal(
            CommunicationRiskType.SEXUAL_REQUEST, RiskConfidence.HIGH,
            "Se detectó una petición de contenido íntimo o sexual.", nowMs
        )
        if (threat) signals += signal(
            CommunicationRiskType.THREAT, RiskConfidence.HIGH,
            "Se detectó lenguaje compatible con una amenaza.", nowMs
        )
        if (bullying) signals += signal(
            CommunicationRiskType.BULLYING, RiskConfidence.LOW,
            "Se detectó lenguaje potencialmente hostigador.", nowMs
        )
        if (selfHarm) signals += signal(
            CommunicationRiskType.SELF_HARM, RiskConfidence.HIGH,
            "Se detectó una expresión compatible con posible autolesión o desesperanza.", nowMs
        )

        return signals
    }

    private fun signal(
        type: CommunicationRiskType,
        confidence: RiskConfidence,
        reason: String,
        timestampMs: Long
    ) = CommunicationRiskSignal(type, confidence, reason, timestampMs = timestampMs)

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
}
