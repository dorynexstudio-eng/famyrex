package com.famyrex.app

import com.famyrex.family.intelligence.FamilySituationEngine
import java.util.Locale

/** Mediador familiar local: no decide quién tiene razón. */
object FamilyMediationEngine {
    data class Perspective(val label: String, val summary: String)
    data class Mediation(
        val situation: FamilySituationEngine.Situation,
        val perspectives: List<Perspective>,
        val facts: List<String>,
        val indicators: List<String>,
        val unknowns: List<String>,
        val agreement: List<String>,
        val followUp: List<String>
    )

    fun mediate(context: android.content.Context, text: String, appName: String? = null): Mediation {
        val now = System.currentTimeMillis()
        val incidents = CommunicationRiskIncidentStore(context).load()
            .filter { now - it.createdAtMs <= 24 * 60 * 60 * 1000L }
            .filter { appName.isNullOrBlank() || it.sourcePackage?.contains(appName, true) == true }
        val evidence = incidents.map {
            FamilySituationEngine.Evidence(
                FamilySituationEngine.EvidenceKind.INDICATOR,
                "Señal ${it.type.name.lowercase(Locale.getDefault())} con confianza ${it.confidence.name.lowercase(Locale.getDefault())}.",
                it.sourcePackage,
                when (it.confidence) { RiskConfidence.HIGH -> 85; RiskConfidence.MEDIUM -> 65; RiskConfidence.LOW -> 40 }
            )
        }
        val situation = FamilySituationEngine.analyze(text, appName, evidence)
        val facts = situation.evidence.filter { it.kind == FamilySituationEngine.EvidenceKind.OBSERVED_DATA || it.kind == FamilySituationEngine.EvidenceKind.CONFIRMED }.map { it.text }
        val indicators = situation.evidence.filter { it.kind == FamilySituationEngine.EvidenceKind.INDICATOR || it.kind == FamilySituationEngine.EvidenceKind.CHANGE }.map { it.text }
        val unknowns = listOf(
            "Una señal digital no confirma por sí sola la intención de otra persona ni un diagnóstico.",
            "Famyrex no determina automáticamente quién tiene razón.",
            "La información disponible depende de los permisos y de lo que Android exponga al dispositivo."
        )
        val perspectives = listOf(
            Perspective("Persona preocupada", "Qué hecho concreto le preocupa y qué necesita para sentirse segura o escuchada."),
            Perspective("Otra parte", "Qué ocurrió desde su punto de vista y qué considera una solución razonable."),
            Perspective("Famyrex", "Qué datos observa, qué indicadores encuentra y qué todavía no puede confirmar.")
        )
        val agreement = when (situation.severity) {
            FamilySituationEngine.Severity.RED -> listOf("Priorizar la seguridad y buscar ayuda humana adecuada.", "No usar el mediador como sustituto de intervención ante un riesgo inmediato.")
            FamilySituationEngine.Severity.ORANGE -> listOf("Revisar los hechos sin acusaciones automáticas.", "Escuchar por separado a las personas implicadas.", "Acordar una acción de protección y revisarla en 24–48 horas.")
            else -> listOf("Definir un solo problema para resolver primero.", "Acordar una acción concreta que ambas partes acepten.", "Fijar una revisión para comprobar si el acuerdo funciona.")
        }
        return Mediation(situation, perspectives, facts, indicators, unknowns, agreement, listOf("¿Se ha cumplido el acuerdo?", "¿Ha disminuido el problema?", "¿Hay que modificar la solución o pedir ayuda?"))
    }

    fun answer(context: android.content.Context, text: String, appName: String? = null): String {
        val m = mediate(context, text, appName)
        val source = m.situation.evidence.firstOrNull { !it.source.isNullOrBlank() }?.source
        val observed = m.indicators.take(3).joinToString(" ").ifBlank { "No hay indicadores adicionales registrados." }
        return buildString {
            append("Mediación familiar. ${m.situation.title}. ")
            if (source != null) append("Aplicación relacionada: $source. ")
            append("Nivel: ${m.situation.severity.name.lowercase(Locale.getDefault())}. ")
            append("Lo observado: $observed ")
            append("No confirmado: ${m.unknowns.first()} ")
            append("Siguiente paso: ${m.agreement.first()}")
        }
    }
}
