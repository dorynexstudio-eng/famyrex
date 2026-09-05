package com.famyrex.app

/**
 * Mantiene el análisis de una conversación aislado por aplicación de origen.
 * Una señal de WhatsApp, por ejemplo, no debe completar el contexto de otra app.
 */
object CommunicationObservationScope {
    fun forSource(
        observations: List<CommunicationObservation>,
        sourcePackage: String
    ): List<CommunicationObservation> = observations.filter { it.sourcePackage == sourcePackage }
}
