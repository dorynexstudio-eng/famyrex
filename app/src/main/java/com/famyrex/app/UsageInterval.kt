package com.famyrex.app

/**
 * Intervalo de uso agregado utilizado por el motor de bienestar.
 * No contiene contenido privado ni texto de comunicaciones.
 */
data class UsageInterval(
    val timestampMs: Long,
    val totalTimeMs: Long
)
