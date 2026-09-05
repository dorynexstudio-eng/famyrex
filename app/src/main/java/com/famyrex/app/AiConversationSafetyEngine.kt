package com.famyrex.app

/**
 * Analizador local y conservador para texto procedente de asistentes de IA.
 *
 * Importante:
 * - analiza texto solo en memoria;
 * - no almacena la conversación ni devuelve el texto original;
 * - produce señales, no diagnósticos ni conclusiones sobre el menor;
 * - usa contexto para reducir falsos positivos de ficción, tareas y preguntas hipotéticas.
 */
object AiConversationSafetyEngine {

    private data class Rule(
        val category: CommunicationRiskType,
        val confidence: RiskConfidence,
        val reason: String,
        val phrases: List<String>
    )

    private val CONTEXT_EXEMPTIONS = listOf(
        "para una novela", "para mi novela", "para un cuento", "para una historia",
        "estoy escribiendo una historia", "estoy escribiendo un cuento", "es ficcion",
        "en una novela", "en mi novela", "en una historia", "en mi historia",
        "para una tarea", "para un trabajo", "para clase", "es un ejercicio",
        "es un ejemplo", "ejemplo hipotetico", "hipoteticamente", "en teoria",
        "que significa", "que pasa si", "personaje"
    )

    fun analyze(
        text: String,
        sourcePackage: String? = null,
        timestampMs: Long = System.currentTimeMillis()
    ): List<CommunicationRiskSignal> {
        val normalized = normalize(text)
        if (normalized.isBlank()) return emptyList()

        val matches = RULES.filter { rule -> rule.phrases.any(normalized::contains) }
        if (matches.isEmpty()) return emptyList()

        // El contexto académico, hipotético o de ficción evita generar una señal personal.
        // Es una salvaguarda deliberada para reducir falsos positivos.
        if (CONTEXT_EXEMPTIONS.any(normalized::contains)) return emptyList()

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
            "com.google.android.apps.gemini",
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
            reason = "El texto contiene una expresión explícita relacionada con posible autolesión o suicidio.",
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
            category = CommunicationRiskType.SOCIAL_ISOLATION,
            confidence = RiskConfidence.MEDIUM,
            reason = "El texto plantea una posible situación de aislamiento, rechazo o falta de apoyo social.",
            phrases = listOf(
                "me dejan de lado", "me dejando de lado", "dejando de lado", "mis amigas me dejan de lado",
                "mis amigas me estén dejando de lado", "mis amigos me dejan de lado", "nadie quiere estar conmigo",
                "me siento solo", "me siento sola", "no tengo amigos", "no tengo amigas", "me han dejado de lado"
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
