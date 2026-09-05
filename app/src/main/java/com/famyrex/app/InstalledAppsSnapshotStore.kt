package com.famyrex.app

import android.content.Context
import org.json.JSONArray

/**
 * Guarda únicamente identificadores de paquete. No almacena contenido privado.
 * Sirve como segunda capa para detectar cambios que pudieran ocurrir mientras
 * el proceso de Famyrex estaba detenido.
 */
class InstalledAppsSnapshotStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_installed_apps", Context.MODE_PRIVATE)

    fun load(): Set<String> {
        val raw = prefs.getString("packages", null) ?: return emptySet()
        return runCatching {
            val array = JSONArray(raw)
            buildSet {
                for (i in 0 until array.length()) add(array.getString(i))
            }
        }.getOrDefault(emptySet())
    }

    fun save(packages: Set<String>) {
        val array = JSONArray()
        packages.sorted().forEach(array::put)
        prefs.edit().putString("packages", array.toString()).apply()
    }

    fun isInitialized(): Boolean = prefs.getBoolean("initialized", false)

    fun markInitialized() {
        prefs.edit().putBoolean("initialized", true).apply()
    }
}
