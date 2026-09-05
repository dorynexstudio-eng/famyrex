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
 * Cuando la fuente es un asistente de IA, delega además en AiConversationSafetyEngine.
 */
object CommunicationSignalDetector {
    private const val WINDOW_MS = 30 * 60 * 1000L
    private const val SOCIAL_WINDOW_MS = 7 * 24 * 60 * 60 * 1000L
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

        val socialRecent = ordered
            .filter { end - it.timestampMs in 0..SOCIAL_WINDOW_MS }
            .takeLast(MAX_OBSERVATIONS)
            .map { it.copy(normalizedText = normalize(it.normalizedText)) }
            .filter { it.normalizedText.isNotBlank() }

        if (recent.isEmpty() && socialRecent.isEmpty()) return CommunicationRiskEngine.evaluate(emptyList())

        val signals = mutableListOf<CommunicationRiskSignal>()

        fun add(type: CommunicationRiskType, confidence: RiskConfidence, reason: String, observation: CommunicationObservation) {
            signals += CommunicationRiskSignal(
                type = type,
                confidence = confidence,
                reason = reason,
                sourcePackage = observation.sourcePackage,
                timestampMs = observation.timestampMs,
                direction = when {
                    observation.isIncoming -> CommunicationDirection.INCOMING
                    else -> CommunicationDirection.OUTGOING
                }
            )
        }

        recent.forEach { observation ->
            if (AiConversationSafetyEngine.isAiAssistantPackage(observation.sourcePackage)) {
                signals += AiConversationSafetyEngine.analyze(
                    text = observation.normalizedText,
                    sourcePackage = observation.sourcePackage,
                    timestampMs = observation.timestampMs
                )
            }
        }

        val observationsWith = recent.map { observation -> flagsFor(observation) }
        val socialObservationsWith = socialRecent.map { observation -> flagsFor(observation) }

        val personal = observationsWith.filter { it.personalInfo }
        val secrets = observationsWith.filter { it.secrecy }
        val meetings = observationsWith.filter { it.meeting }
        val sexual = observationsWith.filter { it.sexual }
        val threats = observationsWith.filter { it.threat }
        val bullying = observationsWith.filter { it.bullying && it.observation.isIncoming }
        val outgoingBullying = observationsWith.filter { it.bullying && !it.observation.isIncoming }
        val selfHarm = observationsWith.filter { it.selfHarm }
        val isolation = observationsWith.filter { it.isolation }
        val unknownContacts = observationsWith.filter { it.unknownContact }
        val socialConflicts = socialObservationsWith.filter { it.socialConflict }
        val socialIsolation = socialObservationsWith.filter { it.isolation }
        val socialBullying = socialObservationsWith.filter { it.bullying && it.observation.isIncoming }

        if (unknownContacts.isNotEmpty()) {
            add(CommunicationRiskType.GROOMING, RiskConfidence.LOW,
                "Se mencionó un contacto nuevo o desconocido; por sí solo no implica peligro.", unknownContacts.first().observation)
        }

        if (personal.isNotEmpty()) {
            add(CommunicationRiskType.GROOMING, RiskConfidence.LOW,
                "Se solicitó información personal; por sí sola no implica una situación de riesgo.", personal.first().observation)
        }

        if (unknownContacts.isNotEmpty() && (personal.isNotEmpty() || secrets.isNotEmpty() || meetings.isNotEmpty())) {
            add(CommunicationRiskType.GROOMING, RiskConfidence.MEDIUM,
                "Un contacto nuevo o desconocido aparece junto a secretismo, datos personales o una propuesta de encuentro.",
                (personal.lastOrNull() ?: secrets.lastOrNull() ?: meetings.last()).observation)
        }

        if (personal.isNotEmpty() && (meetings.isNotEmpty() || secrets.isNotEmpty())) {
            val evidence = personal.first()
            add(CommunicationRiskType.GROOMING,
                if (secrets.isNotEmpty() && meetings.isNotEmpty()) RiskConfidence.MEDIUM else RiskConfidence.LOW,
                "Se combinaron referencias a información personal con secretismo o propuesta de encuentro.", evidence.observation)
        }

        if (sexual.isNotEmpty()) {
            add(CommunicationRiskType.SEXUAL_REQUEST, RiskConfidence.HIGH,
                "Se detectó una petición de contenido íntimo.", sexual.last().observation)
        }

        if (sexual.isNotEmpty() && (personal.isNotEmpty() || meetings.isNotEmpty() || secrets.isNotEmpty())) {
            add(CommunicationRiskType.SEXUAL_REQUEST, RiskConfidence.HIGH,
                "Petición de contenido íntimo combinada con otra señal contextual.", sexual.last().observation)
        }

        if (sexual.isNotEmpty() && secrets.isNotEmpty()) {
            add(CommunicationRiskType.SECRET_KEEPING, RiskConfidence.HIGH,
                "Una petición de contenido íntimo aparece acompañada de una petición explícita de mantenerlo en secreto.", secrets.last().observation)
        }

        if (threats.size >= 2 || (threats.isNotEmpty() && secrets.isNotEmpty())) {
            add(CommunicationRiskType.THREAT, RiskConfidence.HIGH,
                "Se detectó una amenaza explícita con repetición o contexto adicional.", threats.last().observation)
        }

        val bullyingSources = bullying.groupBy { it.observation.sourcePackage }
        if (bullying.isNotEmpty()) {
            add(CommunicationRiskType.BULLYING,
                if (bullying.size >= 3 || bullyingSources.any { it.value.size >= 2 }) RiskConfidence.MEDIUM else RiskConfidence.LOW,
                "Se detectó lenguaje hostil dirigido al menor; una señal aislada requiere contexto y seguimiento.", bullying.last().observation)
        }

        if (bullying.size >= 3 || bullyingSources.any { it.value.size >= 2 }) {
            add(CommunicationRiskType.BULLYING, RiskConfidence.MEDIUM,
                "Se detectó un patrón repetido de lenguaje hostil en mensajes entrantes.", bullying.last().observation)
        }

        // El lenguaje hostil enviado desde el dispositivo se conserva como señal
        // independiente de dirección. No etiqueta al menor como agresor: solo
        // indica que conviene revisar el contexto y comprobar si existe un patrón.
        val outgoingBullyingSources = outgoingBullying.groupBy { it.observation.sourcePackage }
        if (outgoingBullying.isNotEmpty()) {
            add(CommunicationRiskType.BULLYING,
                if (outgoingBullying.size >= 3 || outgoingBullyingSources.any { it.value.size >= 2 }) RiskConfidence.MEDIUM else RiskConfidence.LOW,
                "Se detectó lenguaje hostil enviado desde el dispositivo; conviene revisar el contexto y si existe un patrón.",
                outgoingBullying.last().observation)
        }

        if (outgoingBullying.size >= 3 || outgoingBullyingSources.any { it.value.size >= 2 }) {
            add(CommunicationRiskType.BULLYING, RiskConfidence.MEDIUM,
                "Se detectó repetición de lenguaje hostil enviado desde el dispositivo; esto no determina por sí solo la intención ni la situación.",
                outgoingBullying.last().observation)
        }

        if (isolation.isNotEmpty()) {
            add(CommunicationRiskType.SOCIAL_ISOLATION,
                if (isolation.size >= 2) RiskConfidence.MEDIUM else RiskConfidence.LOW,
                "Se detectaron expresiones de posible aislamiento o exclusión social; conviene observar su evolución.", isolation.last().observation)
        }

        if (isolation.isNotEmpty() && bullying.isNotEmpty()) {
            add(CommunicationRiskType.SOCIAL_ISOLATION, RiskConfidence.MEDIUM,
                "Las expresiones de aislamiento aparecen junto a lenguaje hostil dirigido al menor.", isolation.last().observation)
        }

        if (socialConflicts.isNotEmpty()) {
            val conflictDays = socialConflicts.map { dayKey(it.observation.timestampMs) }.distinct().size
            add(CommunicationRiskType.SOCIAL_CONFLICT,
                when {
                    conflictDays >= 3 -> RiskConfidence.MEDIUM
                    socialConflicts.size >= 2 -> RiskConfidence.MEDIUM
                    else -> RiskConfidence.LOW
                },
                when {
                    conflictDays >= 3 -> "El conflicto entre iguales aparece en varios días y conviene revisar si está empeorando."
                    socialConflicts.size >= 2 -> "Se detectaron varias señales compatibles con tensión o conflicto entre iguales; conviene observar su evolución."
                    else -> "Se detectaron señales compatibles con tensión o conflicto entre iguales; una situación puntual no implica acoso."
                }, socialConflicts.last().observation)
        }

        if (socialConflicts.isNotEmpty() && socialBullying.isNotEmpty()) {
            add(CommunicationRiskType.SOCIAL_CONFLICT, RiskConfidence.MEDIUM,
                "El conflicto entre iguales aparece junto a lenguaje hostil dirigido al menor.", socialConflicts.last().observation)
        }

        if (socialConflicts.isNotEmpty() && socialIsolation.isNotEmpty()) {
            add(CommunicationRiskType.SOCIAL_CONFLICT, RiskConfidence.MEDIUM,
                "El conflicto entre iguales aparece acompañado de señales de exclusión o aislamiento.", socialConflicts.last().observation)
        }

        if (selfHarm.size >= 2 || (selfHarm.isNotEmpty() && threats.isNotEmpty())) {
            add(CommunicationRiskType.SELF_HARM, RiskConfidence.HIGH,
                "Se detectaron expresiones de posible autolesión con repetición o contexto de amenaza.", selfHarm.last().observation)
        }

        if (secrets.size >= 2 && meetings.isNotEmpty()) {
            add(CommunicationRiskType.SECRET_KEEPING, RiskConfidence.MEDIUM,
                "Se detectó secretismo repetido junto con una propuesta de encuentro.", secrets.last().observation)
        }

        return CommunicationRiskEngine.evaluate(signals.distinctBy { "${it.type}:${it.reason}:${it.sourcePackage}:${it.direction}" })
    }

    private fun flagsFor(observation: CommunicationObservation): ObservationFlags = ObservationFlags(
        observation = observation,
        personalInfo = containsAny(observation.normalizedText, PERSONAL_INFO),
        secrecy = containsAny(observation.normalizedText, SECRECY),
        meeting = containsAny(observation.normalizedText, MEETING),
        sexual = containsAny(observation.normalizedText, SEXUAL),
        threat = containsAny(observation.normalizedText, THREATS),
        bullying = containsAny(observation.normalizedText, BULLYING),
        selfHarm = containsAny(observation.normalizedText, SELF_HARM),
        isolation = containsAny(observation.normalizedText, ISOLATION),
        unknownContact = containsAny(observation.normalizedText, UNKNOWN_CONTACT),
        socialConflict = containsAny(observation.normalizedText, SOCIAL_CONFLICT)
    )

    private fun dayKey(timestampMs: Long): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date(timestampMs))

    private data class ObservationFlags(
        val observation: CommunicationObservation,
        val personalInfo: Boolean,
        val secrecy: Boolean,
        val meeting: Boolean,
        val sexual: Boolean,
        val threat: Boolean,
        val bullying: Boolean,
        val selfHarm: Boolean,
        val isolation: Boolean,
        val unknownContact: Boolean,
        val socialConflict: Boolean
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

    private val PERSONAL_INFO = listOf("direccion", "donde vives", "colegio", "instituto", "ubicacion", "telefono", "numero de telefono")
    private val UNKNOWN_CONTACT = listOf("no lo conozco", "no la conozco", "no conozco a este chico", "no conozco a esta chica", "es un chico nuevo", "es una chica nueva", "persona nueva", "alguien que no conozco", "un desconocido")
    private val SECRECY = listOf("no se lo digas", "que quede entre nosotros", "en secreto", "es nuestro secreto", "no digas nada", "que nadie se entere", "borra el mensaje")
    private val MEETING = listOf("quedamos a solas", "ven a verme", "ven solo", "ven sola", "nos vemos a escondidas", "donde nos vemos", "ven sin tus padres", "te recojo")
    private val SEXUAL = listOf("foto desnudo", "foto desnuda", "foto intima", "foto intimas", "fotos intimas", "manda nudes", "manda nude", "desnudate", "contenido sexual", "foto de tus partes", "fotos de tus partes", "fotos de tus partes intimas", "manda fotos de tu cuerpo")
    private val THREATS = listOf("te voy a hacer daño", "te hare daño", "te matare", "si hablas te", "vas a pagar", "te voy a encontrar")
    private val BULLYING = listOf("eres un inutil", "das asco", "nadie te quiere", "callate", "te vamos a echar", "todos se rien de ti")
    private val ISOLATION = listOf("me dejan de lado", "me estan dejando de lado", "me dejan sola", "me dejan solo", "nadie quiere estar conmigo", "nadie me habla", "me excluyen", "no me incluyen", "me siento apartado", "me siento apartada", "no tengo amigos", "todas mis amigas me dejan de lado", "todos mis amigos me dejan de lado")
    private val SOCIAL_CONFLICT = listOf("a mi amiga le gusta", "a mi amigo le gusta", "le gusta el mismo chico", "le gusta la misma chica", "le gusto yo", "le gusto a el", "le gusto a ella", "dicen que soy mas guapa", "dicen que soy mas guapo", "dice que soy mas guapa", "dice que soy mas guapo", "esta celosa", "esta celoso", "tiene celos", "celos por un chico", "celos por una chica", "me compara con", "me esta comparando con", "me echa la culpa por el chico", "me echa la culpa por la chica", "me estan echando la culpa por el chico", "me estan echando la culpa por la chica", "esta enfadada conmigo por el chico", "esta enfadado conmigo por la chica", "se ha enfadado conmigo por el chico", "se ha enfadado conmigo por la chica", "hablan mal de mi por el chico", "hablan mal de mi por la chica", "mis amigas estan contra mi", "mis amigos estan contra mi", "mis amigas se han puesto en mi contra", "mis amigos se han puesto en mi contra", "me estan diciendo cosas por el chico", "me estan diciendo cosas por la chica", "me insultan porque le gusto", "me insultan por el chico", "me insultan por la chica")
    private val SELF_HARM = listOf("quiero hacerme daño", "quiero hacerme dano", "quiero desaparecer", "no quiero vivir", "me quiero morir", "hacerme daño", "hacerme dano")
}
