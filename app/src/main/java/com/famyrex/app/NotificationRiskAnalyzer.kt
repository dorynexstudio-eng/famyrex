package com.famyrex.app

/**
 * Analizador local y conservador de una notificación individual.
 * No persiste el texto original. Devuelve únicamente señales estructuradas.
 */
object NotificationRiskAnalyzer {
    fun analyze(text: String, nowMs: Long = System.currentTimeMillis()): List<CommunicationRiskSignal> {
        val normalized = text.lowercase().replace("\\s+".toRegex(), " ").trim()
        if (normalized.isBlank()) return emptyList()

        val signals = mutableListOf<CommunicationRiskSignal>()

        val personalInfo = containsAny(normalized, listOf(
            "dime tu dirección", "dime donde vives", "dime dónde vives", "tu dirección",
            "tu colegio", "tu instituto", "tu escuela", "pásame tu ubicación", "pasame tu ubicación",
            "manda tu ubicación", "manda tu foto", "dónde estudias", "donde estudias"
        ))
        val secret = containsAny(normalized, listOf(
            "no se lo digas a nadie", "que quede entre nosotros", "es nuestro secreto",
            "no le digas a tus padres", "no se lo cuentes a tus padres", "borra el mensaje"
        ))
        val meeting = containsAny(normalized, listOf(
            "quedamos a solas", "ven a verme", "ven solo", "ven sola", "nos vemos a escondidas",
            "dónde nos vemos", "donde nos vemos", "ven sin tus padres"
        ))
        val sexual = containsAny(normalized, listOf(
            "foto desnudo", "foto desnuda", "foto íntima", "foto intima", "manda nudes",
            "manda nude", "desnúdate", "desnudate", "contenido sexual"
        ))
        val threat = containsAny(normalized, listOf(
            "te voy a hacer daño", "te haré daño", "te hare daño", "te mataré", "te matare",
            "si hablas te", "vas a pagar", "te voy a encontrar"
        ))
        val bullying = containsAny(normalized, listOf(
            "eres un inútil", "eres un inutil", "das asco", "nadie te quiere", "cállate", "callate",
            "te vamos a echar", "todos se ríen de ti", "todos se rien de ti"
        ))
        val selfHarm = containsAny(normalized, listOf(
            "quiero hacerme daño", "quiero hacerme dano", "quiero desaparecer",
            "no quiero vivir", "me quiero morir", "hacerme daño", "hacerme dano"
        ))

        if (personalInfo) signals += CommunicationRiskSignal(
            CommunicationRiskType.GROOMING, RiskConfidence.MEDIUM,
            "Se detectó una solicitud o referencia a información personal."
        )
        if (secret) signals += CommunicationRiskSignal(
            CommunicationRiskType.SECRET_KEEPING, RiskConfidence.HIGH,
            "Se detectó una petición explícita de mantener la comunicación en secreto."
        )
        if (meeting) signals += CommunicationRiskSignal(
            CommunicationRiskType.GROOMING, RiskConfidence.MEDIUM,
            "Se detectó una propuesta de encuentro o contacto fuera del entorno habitual."
        )
        if (sexual) signals += CommunicationRiskSignal(
            CommunicationRiskType.SEXUAL_REQUEST, RiskConfidence.HIGH,
            "Se detectó una petición de contenido íntimo o sexual."
        )
        if (threat) signals += CommunicationRiskSignal(
            CommunicationRiskType.THREAT, RiskConfidence.HIGH,
            "Se detectó lenguaje compatible con una amenaza."
        )
        if (bullying) signals += CommunicationRiskSignal(
            CommunicationRiskType.BULLYING, RiskConfidence.MEDIUM,
            "Se detectó lenguaje potencialmente hostigador."
        )
        if (selfHarm) signals += CommunicationRiskSignal(
            CommunicationRiskType.SELF_HARM, RiskConfidence.HIGH,
            "Se detectó una expresión compatible con posible autolesión o desesperanza."
        )

        return signals.map { it.copy(timestampMs = nowMs) }
    }

    private fun containsAny(text: String, phrases: List<String>): Boolean = phrases.any(text::contains)
}
