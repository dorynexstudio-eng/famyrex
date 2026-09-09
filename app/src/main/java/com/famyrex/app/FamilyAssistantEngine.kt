package com.famyrex.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FamilyAssistantEngine {
    fun answer(context: android.content.Context, question: String): String {
        val original = question.trim()
        if (original.isBlank()) return "Escribe una pregunta sobre Famyrex. Puedo explicarte las funciones de la app o consultar los datos disponibles."

        val q = normalize(original)
        val history = UsageSnapshotStore(context).loadHistory().sortedBy { it.date }
        val alerts = AlertStore(context).load()
        val today = history.lastOrNull()
        val todayMinutes = (today?.totalTimeMs ?: 0L) / 60_000L

        return when {
            asksAboutCapabilities(q) -> capabilitiesAnswer()

            asksForMediation(q) -> FamilyMediationEngine.answer(context, original, detectAppName(q))

            asksAboutBullying(q) ->
                "Famyrex puede ayudar a detectar señales compatibles con acoso o ciberacoso a partir de indicadores autorizados, como patrones de comunicación de riesgo, señales repetidas en notificaciones y cambios de comportamiento o uso. La app puede asociar el incidente con la aplicación de origen cuando Android proporciona ese dato. No puede afirmar por sí sola que exista bullying ni identificar automáticamente al responsable."

            asksHowDetectionWorks(q) ->
                "La detección de riesgo no depende de una sola palabra. Famyrex puede combinar señales de comunicación autorizadas, evolución de incidentes, notificaciones observables, patrones de uso, bienestar y otras evidencias disponibles en el dispositivo. Cuando hay suficientes señales, el Centro de Inteligencia puede explicar qué datos han provocado la alerta y recomendar qué hacer."

            asksAboutLocation(q) ->
                "Famyrex puede trabajar con ubicación y geozonas cuando los permisos están concedidos. Esto permite registrar eventos de entrada o salida de zonas configuradas y mostrar el estado de ubicación disponible para el miembro supervisado."

            asksAboutApps(q) -> {
                val app = today?.topApps?.firstOrNull()
                if (app == null) "Famyrex puede registrar el uso de aplicaciones mediante UsageStatsManager y crear historial y tendencias. Todavía no hay datos suficientes para decir cuál se ha usado más hoy."
                else "Famyrex puede registrar qué aplicaciones se usan y durante cuánto tiempo. En el último registro de hoy, la aplicación con más uso es ${app.label}, con aproximadamente ${app.totalTimeMs / 60_000L} minutos."
            }

            asksAboutScreenTime(q) ->
                "Sí. Famyrex registra el tiempo de uso disponible, crea snapshots periódicos y puede convertirlos en historial diario, semanal y tendencias para detectar cambios de comportamiento digital."

            asksAboutAlerts(q) -> {
                val important = alerts.count { it.severity == AlertSeverity.IMPORTANT }
                val attention = alerts.count { it.severity == AlertSeverity.ATTENTION }
                "Famyrex tiene un sistema de alertas inteligentes. Ahora hay ${alerts.size} señales registradas: $important importantes y $attention de atención. Las alertas son indicadores basados en datos y no una prueba automática de que exista un problema."
            }

            asksAboutIntelligence(q) ->
                "El Centro de Inteligencia reúne evidencias, evolución de incidentes, explicaciones y recomendaciones. Su objetivo es responder no solo «qué ha pasado», sino también «por qué ha generado una señal» y «qué conviene hacer después»."

            asksAboutWellbeing(q) -> {
                val settings = WellbeingSettingsStore(context).load()
                val intervals = UsageIntervalStore(context).load(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                val assessment = WellbeingEngine.evaluate(todayMinutes, intervals, settings)
                "Famyrex también incluye bienestar digital. Objetivo diario: ${assessment.goalMinutes} minutos. Progreso: ${assessment.goalProgress}%. Pausas largas observadas entre muestras: ${assessment.breakCount}. ${assessment.recommendation}"
            }

            asksAboutHistory(q) ->
                if (history.size < 3) "El historial todavía es corto. Famyrex necesita acumular varios registros para mostrar tendencias con más contexto."
                else {
                    val recent = history.takeLast(7).map { it.totalTimeMs / 60_000.0 }
                    "Hay ${history.size} registros disponibles. En los últimos ${recent.size} días el promedio registrado es de ${recent.average().toLong()} minutos diarios."
                }

            asksAboutProtection(q) ->
                "Famyrex está pensado como una capa de protección familiar: supervisión del dispositivo, uso de aplicaciones, bienestar digital, ubicación y geozonas, alertas inteligentes, análisis de señales de comunicación autorizadas, historial y tendencias, Centro de Inteligencia, recomendaciones, mediación familiar y controles familiares remotos cuando estén disponibles y autorizados."

            asksWhatCanSee(q) ->
                "Puedo consultar la información que Famyrex haya registrado de forma autorizada en este dispositivo. Dependiendo de los permisos y del modo familiar, eso puede incluir uso de aplicaciones, historial y tendencias, alertas, bienestar, ubicación/geozonas y señales de comunicación observables. No significa que Famyrex pueda verlo todo: las notificaciones permiten analizar el contenido que Android expone, pero no equivalen a leer automáticamente chats privados completos."

            q.contains("privacidad") || q.contains("seguridad") || q.contains("espi") ->
                "Famyrex está diseñado para protección familiar, no para prometer vigilancia total. El asistente solo debe hablar de datos que la app realmente pueda obtener con los permisos correspondientes y debe distinguir entre una señal de riesgo y una conclusión."

            q.contains("cuanto") && (q.contains("hoy") || q.contains("pantalla") || q.contains("movil")) || q.contains("uso de hoy") ->
                "Hoy Famyrex registra aproximadamente $todayMinutes minutos de uso."

            q.contains("mas") && (q.contains("aplic") || q.contains("app")) || q.contains("que aplicacion") -> {
                val app = today?.topApps?.firstOrNull()
                if (app == null) "Todavía no hay datos suficientes de aplicaciones para responder."
                else "La aplicación con más uso en el último registro de hoy es ${app.label}, con aproximadamente ${app.totalTimeMs / 60_000L} minutos."
            }

            q.contains("alert") || q.contains("problema") || q.contains("preocup") -> {
                val important = alerts.count { it.severity == AlertSeverity.IMPORTANT }
                val attention = alerts.count { it.severity == AlertSeverity.ATTENTION }
                "Hay ${alerts.size} señales registradas: $important importantes y $attention de atención. Una alerta es una señal basada en datos de uso; no demuestra por sí sola que exista un problema personal."
            }

            q.contains("semana") || q.contains("tendencia") || q.contains("aument") || q.contains("disminu") ->
                if (history.size < 3) "Aún hay poco historial. Necesito varios días de datos para hablar de una tendencia con confianza."
                else {
                    val recent = history.takeLast(7).map { it.totalTimeMs / 60_000.0 }
                    "En los últimos ${recent.size} días el promedio registrado es de ${recent.average().toLong()} minutos diarios. La tendencia es más fiable cuanto más historial acumulemos."
                }

            q.contains("bienestar") || q.contains("objetivo") || q.contains("descanso") || q.contains("pausa") -> {
                val settings = WellbeingSettingsStore(context).load()
                val intervals = UsageIntervalStore(context).load(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                val assessment = WellbeingEngine.evaluate(todayMinutes, intervals, settings)
                "Objetivo diario: ${assessment.goalMinutes} minutos. Progreso: ${assessment.goalProgress}%. Pausas largas observadas entre muestras: ${assessment.breakCount}. ${assessment.recommendation}"
            }

            q.contains("que sabes") || q.contains("datos") -> asksWhatCanSeeAnswer()

            else ->
                "Puedo explicarte las funciones de Famyrex, consultar datos y ayudarte a mediar problemas familiares. Por ejemplo: «¿Qué funciones tiene Famyrex?», «¿Cómo detecta el bullying?», «¿Qué puede saber de mi hijo?», «¿Qué apps usa más?», «¿Dónde está?», «¿Qué alertas hay?», «Tenemos un conflicto por el móvil» o «¿Quién tiene razón?»"
        }
    }

    private fun normalize(value: String): String = value.lowercase(Locale.getDefault())
        .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u').replace('ü', 'u')
        .replace('¿', ' ').replace('?', ' ')

    private fun containsAny(q: String, vararg terms: String): Boolean = terms.any { q.contains(it) }

    private fun asksForMediation(q: String): Boolean = containsAny(
        q, "quien tiene razon", "quien tiene la razon", "tenemos un conflicto", "estamos discutiendo",
        "discutimos", "nos peleamos", "problema familiar", "conflicto familiar", "no quiere hablar",
        "no me escucha", "me controla demasiado", "controla demasiado", "que hacemos", "como lo solucionamos",
        "quiero crear un acuerdo", "acuerdo familiar", "nos peleamos por", "discusion por el movil",
        "discusion por el telefono", "discutimos por tiktok", "discutimos por instagram", "discutimos por whatsapp"
    )

    private fun detectAppName(q: String): String? = listOf("TikTok", "Instagram", "WhatsApp", "Discord", "YouTube", "Telegram")
        .firstOrNull { q.contains(it.lowercase(Locale.getDefault())) }

    private fun asksAboutCapabilities(q: String): Boolean = containsAny(q, "funciones", "que puede hacer", "que hace famyrex", "para que sirve", "caracteristicas", "todo lo que puede hacer")
    private fun asksAboutBullying(q: String): Boolean = containsAny(q, "bullying", "bulling", "acoso", "ciberacoso", "insultan a mi hijo", "se meten con mi hijo")
    private fun asksHowDetectionWorks(q: String): Boolean = containsAny(q, "como lo sabe", "como detecta", "como puede saber", "como detectar", "como descubre", "como identifica") && containsAny(q, "riesgo", "acoso", "bullying", "problema", "peligro", "comunicacion")
    private fun asksAboutLocation(q: String): Boolean = containsAny(q, "ubicacion", "localizacion", "donde esta", "donde se encuentra", "geozona", "geocerca")
    private fun asksAboutApps(q: String): Boolean = containsAny(q, "aplicaciones", "apps", "app usada", "aplicacion usada", "que aplicaciones")
    private fun asksAboutScreenTime(q: String): Boolean = containsAny(q, "tiempo de pantalla", "tiempo de uso", "cuanto usa el movil", "uso del movil", "horas de movil")
    private fun asksAboutAlerts(q: String): Boolean = containsAny(q, "alertas inteligentes", "alertas", "avisos", "señales de riesgo", "senales de riesgo")
    private fun asksAboutIntelligence(q: String): Boolean = containsAny(q, "centro de inteligencia", "inteligencia", "recomendaciones", "por que ha saltado", "por que hay una alerta")
    private fun asksAboutWellbeing(q: String): Boolean = containsAny(q, "bienestar", "descanso", "pausas", "objetivo diario", "salud digital")
    private fun asksAboutHistory(q: String): Boolean = containsAny(q, "historial", "historico", "tendencias", "tendencia semanal", "ultimos dias")
    private fun asksAboutProtection(q: String): Boolean = containsAny(q, "proteccion", "protege", "seguridad familiar", "como protege", "que controla")
    private fun asksWhatCanSee(q: String): Boolean = containsAny(q, "que puede ver", "que sabe de mi hijo", "que sabe de mis hijos", "que puede saber", "que ve de mi hijo", "puede verlo todo", "puede saber todo")

    private fun capabilitiesAnswer(): String = "Famyrex tiene varias capas de protección: 1) supervisión y estado del dispositivo; 2) uso de aplicaciones y tiempo de pantalla; 3) historial diario/semanal y tendencias; 4) ubicación y geozonas; 5) alertas inteligentes; 6) análisis de señales de comunicación autorizadas; 7) detección de patrones y evolución de incidentes; 8) bienestar digital; 9) Centro de Inteligencia con evidencias, explicaciones y recomendaciones; 10) vinculación segura entre dispositivos familiares; 11) mediación familiar y acuerdos revisables; y 12) controles familiares remotos cuando están configurados y autorizados. Puedo explicarte cualquiera de estas funciones."

    private fun asksWhatCanSeeAnswer(): String = "Famyrex puede trabajar con datos autorizados como uso de apps, tiempo de pantalla, historial y tendencias, ubicación/geozonas, alertas, bienestar y determinadas señales de comunicación observables. No puede ver absolutamente todo ni leer por defecto el contenido privado de todas las aplicaciones."
}
