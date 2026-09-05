package com.famyrex.app

import android.content.Context
import java.util.UUID

/**
 * Keeps the current location UI data connected to the real Android geofencing API.
 * Legacy zones are migrated idempotently so zones created after the first launch
 * are also imported into FamilyZoneStore and registered with Android.
 */
object GeofenceBootstrap {
    fun sync(context: Context) {
        val zoneStore = FamilyZoneStore(context)
        val existingIds = zoneStore.load().mapTo(hashSetOf()) { it.id }
        val prefs = context.getSharedPreferences("famyrex_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("geo_zones", "").orEmpty()

        if (raw.isNotBlank()) {
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
            }.forEach { zone ->
                if (zone.id !in existingIds) zoneStore.save(zone)
            }
        }

        zoneStore.load()
            .filter { it.enabled }
            .forEach { zone ->
                GeofenceManager(context).register(zone) { _, _ -> }
            }
    }
}
