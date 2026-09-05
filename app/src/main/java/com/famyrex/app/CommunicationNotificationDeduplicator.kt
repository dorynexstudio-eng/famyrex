package com.famyrex.app

/**
 * Evita analizar dos veces el mismo contenido de una notificación actualizada.
 * La clave de Android identifica la notificación; el texto permite aceptar una
 * actualización real de esa misma notificación como una observación nueva.
 */
class CommunicationNotificationDeduplicator(
    private val maxEntries: Int = 200
) {
    private val fingerprints = LinkedHashMap<String, String>()

    @Synchronized
    fun shouldProcess(notificationKey: String, sourcePackage: String, text: String): Boolean {
        val key = "$sourcePackage\u0000$notificationKey"
        val previous = fingerprints[key]
        if (previous == text) return false

        fingerprints[key] = text
        while (fingerprints.size > maxEntries) {
            fingerprints.remove(fingerprints.entries.first().key)
        }
        return true
    }

    @Synchronized
    fun clear() {
        fingerprints.clear()
    }
}
