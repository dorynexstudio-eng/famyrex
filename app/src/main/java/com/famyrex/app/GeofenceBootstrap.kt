package com.famyrex.app

import android.content.Context
import java.util.UUID

/**
 * Keeps the current location UI data connected to the real Android geofencing API.
 * Existing legacy zones are migrated once into FamilyZoneStore and then registered.
 */
object GeofenceBootstrap {
    private const val MIGRATED = "famyrex_geozones_migrated"

    fun sync(context: Context) {
        val prefs = context.getSharedPreferences("famyrex_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("geo_zones", "").orEmpty()
        if (raw.isNotBlank() && !prefs.getBoolean(MIGRATED, false)) {
            raw.split(";").mapNotNull { row ->
                val parts = row.split("|")
                if (parts.size == 4) runCatching {
                    FamilyZone(
                        id = UUID.nameUUIDFromBytes("${parts[0]}|${parts[1]}|${parts[2]}".toByteArray()).toString(),
                        name = parts[0],
                        latitude = parts[1].toDouble(),
                        longitude = parts[2].toDouble(),
                        radiusMeters = parts[3].toFloat().coerceAtLeast(100f),
                        enabled = true
                    )
                }.getOrNull() else null
            }.forEach { FamilyZoneStore(context).save(it) }
            prefs.edit().putBoolean(MIGRATED, true).apply()
        }

        FamilyZoneStore(context).load()
            .filter { it.enabled }
            .forEach { zone ->
                GeofenceManager(context).register(zone) { _, _ -> }
            }
    }
}
