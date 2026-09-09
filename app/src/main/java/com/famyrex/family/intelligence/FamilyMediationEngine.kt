package com.famyrex.family.intelligence

/** Local mediator: facilitates agreement instead of deciding who is right. */
object FamilyMediationEngine {
    data class Mediation(
        val situation: FamilySituationEngine.Situation,
        val perspectiveParent: String,
        val perspectiveChild: String,
        val proposal: List<String>,
        val agreement: String,
        val followUp: List<String>
    )

    fun mediate(text: String, appName: String? = null, evidence: List<FamilySituationEngine.Evidence> = emptyList()): Mediation {
        val s = FamilySituationEngine.analyze(text, appName, evidence)
        val parent = when (s.category) {
            FamilySituationEngine.Category.RULES -> "La preocupación puede estar relacionada con seguridad, horarios o cumplimiento de una norma."
            FamilySituationEngine.Category.DIGITAL_USE -> "La familia puede estar intentando proteger el equilibrio digital sin convertir el límite en una pelea."
            FamilySituationEngine.Category.TRUST -> "La preocupación puede ser perder visibilidad o confianza, mientras la otra parte puede percibir demasiado control."
            else -> "La familia puede estar intentando resolver un problema sin sentirse escuchada."
        }
        val child = when (s.category) {
            FamilySituationEngine.Category.RULES -> "La otra parte puede vivir el límite como pérdida de autonomía o falta de confianza."
            FamilySituationEngine.Category.DIGITAL_USE -> "El uso digital puede tener una función social o de ocio que la familia no está viendo completa."
            FamilySituationEngine.Category.TRUST -> "La supervisión puede sentirse como invasión de privacidad aunque exista una preocupación legítima."
            else -> "Puede haber una necesidad o preocupación que todavía no se ha podido expresar bien."
        }
        val proposal = if (s.severity == FamilySituationEngine.Severity.RED) {
            s.nextSteps
        } else {
            listOf("Describir un hecho concreto, sin acusaciones.", "Escuchar la versión de la otra persona sin interrumpir.", "Elegir una solución pequeña, concreta y revisable.", "Fijar una fecha para comprobar si el acuerdo funciona.")
        }
        val agreement = if (s.severity == FamilySituationEngine.Severity.RED) {
            "En una situación de riesgo, el acuerdo familiar queda en segundo plano frente a la seguridad."
        } else {
            "Durante los próximos días, ambas partes aplicarán la solución acordada y podrán pedir una revisión si no funciona."
        }
        return Mediation(s, parent, child, proposal, agreement, listOf("¿Ha disminuido el conflicto?", "¿Se ha cumplido el acuerdo?", "¿Necesita ajustarse alguna condición?"))
    }
}
