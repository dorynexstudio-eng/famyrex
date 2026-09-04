package com.famyrex.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FamilyAssistantEngine {
    fun answer(context: android.content.Context, question: String): String {
        val q = question.trim().lowercase(Locale.getDefault())
        if (q.isBlank()) return "Escribe una pregunta sobre los datos que Famyrex tiene disponibles."

        val history = UsageSnapshotStore(context).loadHistory().sortedBy { it.date }
        val alerts = AlertStore(context).load()
        val today = history.lastOrNull()
        val todayMinutes = (today?.totalTimeMs ?: 0L) / 60_000L

        return when {
            q.contains("cuánto") && (q.contains("hoy") || q.contains("pantalla") || q.contains("móvil") || q.contains("movil")) ||
                q.contains("uso de hoy") ->
                "Hoy Famyrex registra aproximadamente $todayMinutes minutos de uso."

            q.contains("más") && (q.contains("aplic") || q.contains("app")) || q.contains("qué aplicación") || q.contains("que aplicacion") -> {
                val app = today?.topApps?.firstOrNull()
                if (app == null) "Todavía no hay datos suficientes de aplicaciones para responder." 
                else "La aplicación con más uso en el último registro de hoy es ${app.label}, con aproximadamente ${app.totalTimeMs / 60_000L} minutos."
            }

            q.contains("alert") || q.contains("problema") || q.contains("preocup") -> {
                val important = alerts.count { it.severity == AlertSeverity.IMPORTANT }
                val attention = alerts.count { it.severity == AlertSeverity.ATTENTION }
                "Hay ${alerts.size} señales registradas: $important importantes y $attention de atención. Una alerta es una señal basada en datos de uso; no demuestra por sí sola que exista un problema personal."
            }

            q.contains("semana") || q.contains("tendencia") || q.contains("aument") || q.contains("disminu") -> {
                if (history.size < 3) "Aún hay poco historial. Necesito varios días de datos para hablar de una tendencia con confianza."
                else {
                    val recent = history.takeLast(7).map { it.totalTimeMs / 60_000.0 }
                    val avg = recent.average()
                    "En los últimos ${recent.size} días el promedio registrado es de ${avg.toLong()} minutos diarios. La tendencia es más fiable cuanto más historial acumulemos."
                }
            }

            q.contains("bienestar") || q.contains("objetivo") || q.contains("descanso") || q.contains("pausa") -> {
                val settings = WellbeingSettingsStore(context).load()
                val intervals = UsageIntervalStore(context).load(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                )
                val assessment = WellbeingEngine.evaluate(todayMinutes, intervals, settings)
                "Objetivo diario: ${assessment.goalMinutes} minutos. Progreso: ${assessment.goalProgress}%. Pausas largas observadas entre muestras: ${assessment.breakCount}. ${assessment.recommendation}"
            }

            q.contains("qué sabes") || q.contains("que sabes") || q.contains("datos") || q.contains("privacidad") ->
                "Puedo consultar el uso registrado por Famyrex, tendencias del historial, alertas y datos de bienestar disponibles localmente. No puedo ver conversaciones privadas de WhatsApp, Instagram, Telegram, correo u otras aplicaciones."

            else ->
                "Puedo ayudarte con uso de hoy, aplicaciones más usadas, tendencias, alertas, bienestar y los datos que Famyrex tiene disponibles. Prueba: «¿Cuánto se ha usado hoy?»."
        }
    }
}
