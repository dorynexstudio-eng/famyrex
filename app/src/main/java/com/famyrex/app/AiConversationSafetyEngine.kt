package com.famyrex.app

/**
 * Analizador local y conservador para texto procedente de asistentes de IA.
 *
 * Importante:
 * - analiza texto solo en memoria;
 * - no almacena la conversación ni devuelve el texto original;
 * - produce señales, no diagnósticos ni conclusiones sobre el menor;
 * - una coincidencia aislada no se considera prueba de una situación de riesgo.
 */
object AiConversationSafetyEngine {

    private data class Rule(
        val category: CommunicationRiskType,
        val confidence: RiskConfidence,
        val reason: String,
        val phrases: List<String>
    )

    fun analyze(text: String, sourcePackage: String? = null, timestampMs: Long = System.currentTimeMillis()): List<CommunicationRiskSignal> {
        val normalized = normalize(text)
        if (normalized.isBlank()) return emptyList()

        val matches = RULES.filter { rule -> rule.phrases.any(normalized::contains) }
        if (matches.isEmpty()) return emptyList()

        return matches.map { rule ->
            CommunicationRiskSignal(
                type = rule.category,
                confidence = rule.confidence,
                reason = rule.reason,
                sourcePackage = sourcePackage,
                timestampMs = timestampMs
            )
        }
    }

    fun isAiAssistantPackage(packageName: String?): Boolean {
        return when (packageName?.lowercase()) {
            "com.openai.chatgpt",
            "com.google.android.apps.bard",
            "com.microsoft.copilot" -> true
            else -> packageName?.contains("chatgpt", ignoreCase = true) == true ||
                packageName?.contains("gemini", ignoreCase = true) == true ||
                packageName?.contains("copilot", ignoreCase = true) == true
        }
    }

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

    private val RULES = listOf(
        Rule(
            category = CommunicationRiskType.SELF_HARM,
            confidence = RiskConfidence.HIGH,
            reason = "El texto contiene una expresión explícita relacionada con autolesión.",
            phrases = listOf(
                "quiero hacerme daño", "quiero hacerme dano", "me quiero suicidar",
                "quiero suicidarme", "quiero morir", "no quiero vivir", "quiero desaparecer",
                "como puedo suicidarme", "como puedo matarme", "como hacerme daño", "como hacerme dano"
            )
        ),
        Rule(
            category = CommunicationRiskType.THREAT,
            confidence = RiskConfidence.HIGH,
            reason = "El texto contiene una expresión explícita relacionada con violencia o amenaza.",
            phrases = listOf(
                "quiero matar", "como matar a", "como hacer daño a", "como hacer dano a",
                "voy a hacerle daño", "voy a hacerle dano", "me quieren matar", "me van a matar"
            )
        ),
        Rule(
            category = CommunicationRiskType.SEXUAL_REQUEST,
            confidence = RiskConfidence.MEDIUM,
            reason = "El texto plantea una situación relacionada con contenido sexual o íntimo.",
            phrases = listOf(
                "me pide nudes", "me piden nudes", "me pide fotos desnudo", "me pide fotos desnuda",
                "foto intima", "foto íntima", "contenido sexual", "sexo con un adulto",
                "un adulto me pide", "alguien me pide nudes"
            )
        ),
        Rule(
            category = CommunicationRiskType.GROOMING,
            confidence = RiskConfidence.MEDIUM,
            reason = "El texto plantea contacto potencialmente preocupante con una persona desconocida o adulta.",
            phrases = listOf(
                "un desconocido quiere verme", "un desconocido quiere conocerme",
                "un adulto quiere verme", "un adulto quiere conocerme", "conocer a alguien que conoci online",
                "quedar con alguien que conoci", "quedar con alguien que conocí", "me pide que quede en secreto"
            )
        ),
        Rule(
            category = CommunicationRiskType.BULLYING,
            confidence = RiskConfidence.MEDIUM,
            reason = "El texto describe una posible situación de acoso, humillación o intimidación.",
            phrases = listOf(
                "me hacen bullying", "me hacen acoso", "se meten conmigo", "se rien de mi",
                "se ríen de mi", "me insultan todos", "me amenazan en el colegio", "me estan acosando",
                "me están acosando"
            )
        ),
        Rule(
            category = CommunicationRiskType.SECRET_KEEPING,
            confidence = RiskConfidence.MEDIUM,
            reason = "El texto contiene una petición o situación de secretismo potencialmente relevante.",
            phrases = listOf(
                "no se lo puedo contar a mis padres", "no quiero que mis padres sepan",
                "no se lo digas a mis padres", "que mis padres no se enteren",
                "quiero mantenerlo en secreto", "nadie puede saberlo"
            )
        )
    )
}
