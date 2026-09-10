package com.famyrex.family.intelligence

/**
 * Local, explainable situation model used by Famyrex intelligence and mediation.
 * It deliberately separates observed data, indicators, hypotheses and confirmed
 * information supplied by a family member. It never turns an indicator into a fact.
 */
object FamilySituationEngine {
    enum class Category { DIGITAL_USE, RELATIONSHIP, COMMUNICATION, TRUST, RULES, WELLBEING, SAFETY, FAMILY_CONFLICT }
    enum class Severity { GREEN, YELLOW, ORANGE, RED }
    enum class EvidenceKind { OBSERVED_DATA, CHANGE, INDICATOR, USER_REPORTED, CONFIRMED }

    data class Evidence(
        val kind: EvidenceKind,
        val text: String,
        val source: String? = null,
        val confidence: Int = 50
    )

    data class Situation(
        val category: Category,
        val severity: Severity,
        val title: String,
        val evidence: List<Evidence>,
        val questions: List<String>,
        val nextSteps: List<String>
    )

    fun analyze(text: String, appName: String? = null, evidence: List<Evidence> = emptyList()): Situation {
        val q = text.lowercase()
        val immediateRisk = listOf(
            "suicid", "autoles", "amenaza de muerte", "matar", "peligro inmediato",
            "agresion fisica", "agresión física", "violencia fisica", "violencia física"
        ).any(q::contains)
        val safety = listOf(
            "amenaza", "amenazas", "acoso", "bullying", "bulin", "chantaje", "extors",
            "humill", "miedo"
        ).any(q::contains)
        val digital = listOf("tiktok", "instagram", "whatsapp", "discord", "youtube", "móvil", "movil", "telefono", "teléfono", "pantalla", "juego", "videojuego").any(q::contains)
        val trust = listOf("oculta", "secret", "control", "privacidad", "espiar", "confío", "confio", "confianza").any(q::contains)
        val rules = listOf("límite", "limite", "norma", "castigo", "quitar", "horario", "no me deja", "no hace caso").any(q::contains)
        val communication = listOf("no habla", "no quiere hablar", "discut", "pelea", "enfad", "enoj", "me escucha", "escuchar").any(q::contains)

        val category = when {
            immediateRisk || safety -> Category.SAFETY
            digital && rules -> Category.RULES
            trust -> Category.TRUST
            communication -> Category.COMMUNICATION
            digital -> Category.DIGITAL_USE
            else -> Category.FAMILY_CONFLICT
        }
        val severity = when {
            immediateRisk -> Severity.RED
            safety -> Severity.ORANGE
            trust || rules || communication || digital -> Severity.YELLOW
            else -> Severity.GREEN
        }

        val title = when (category) {
            Category.SAFETY -> "Posible situación de seguridad"
            Category.DIGITAL_USE -> "Situación relacionada con el uso digital"
            Category.RULES -> "Conflicto sobre normas o límites"
            Category.TRUST -> "Situación relacionada con confianza y privacidad"
            Category.COMMUNICATION -> "Dificultad de comunicación familiar"
            else -> "Situación familiar que merece atención"
        }

        val allEvidence = buildList {
            addAll(evidence)
            if (!appName.isNullOrBlank()) add(Evidence(EvidenceKind.USER_REPORTED, "La situación menciona o está relacionada con $appName.", appName, 60))
            add(Evidence(EvidenceKind.USER_REPORTED, "La situación procede de lo que ha expresado la familia.", "Asistente Familiar", 80))
        }

        val questions = when (category) {
            Category.SAFETY -> listOf("¿Qué ocurrió exactamente y cuándo?", "¿En qué aplicación o entorno ocurrió?", "¿Existe una amenaza inmediata o riesgo físico?")
            Category.DIGITAL_USE -> listOf("¿Qué aplicación o actividad está generando el problema?", "¿Está afectando al sueño, estudios o convivencia?", "¿Qué norma existe actualmente?")
            Category.RULES -> listOf("¿Qué norma se está discutiendo?", "¿La norma fue acordada previamente?", "¿Qué considera razonable cada parte?")
            Category.TRUST -> listOf("¿Qué hecho concreto ha generado la desconfianza?", "¿Qué parte es un dato y qué parte es una interpretación?", "¿Qué nivel de privacidad espera cada persona?")
            Category.COMMUNICATION -> listOf("¿Qué ocurrió justo antes del conflicto?", "¿Qué cree cada parte que necesita la otra?", "¿Pueden hablar del problema sin discutir ahora mismo?")
            else -> listOf("¿Qué ocurrió exactamente?", "¿Cómo lo interpreta cada persona?", "¿Qué solución sería aceptable para ambas partes?")
        }

        val next = when (severity) {
            Severity.RED -> listOf("Priorizar la seguridad inmediata.", "Buscar ayuda de un adulto responsable o profesional adecuado.", "No intentar resolver una situación de riesgo únicamente mediante el mediador.")
            Severity.ORANGE -> listOf("Separar hechos comprobables de interpretaciones.", "Conservar la información relevante sin acusar a nadie.", "Hablar con el menor y escuchar su versión antes de concluir.")
            Severity.YELLOW -> listOf("Escuchar las dos perspectivas.", "Definir un problema concreto en lugar de discutir sobre todo a la vez.", "Proponer un acuerdo pequeño y revisable.")
            Severity.GREEN -> listOf("Hablar del problema con calma.", "Acordar una acción concreta.", "Revisar si la solución funciona.")
        }

        return Situation(category, severity, title, allEvidence, questions, next)
    }

    fun bullyingGuidance(appName: String? = null): String {
        val place = appName?.takeIf { it.isNotBlank() }?.let { " en $it" } ?: ""
        return "Famyrex puede detectar indicadores relacionados con una posible situación de acoso$place, pero un indicador no confirma por sí solo que exista bullying ni identifica automáticamente al responsable. Cuando existan datos suficientes, mostrará qué fuente los originó, qué se ha observado y qué sigue siendo una hipótesis."
    }
}
