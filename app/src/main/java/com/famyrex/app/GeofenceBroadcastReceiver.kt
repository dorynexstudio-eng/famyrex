package com.famyrex.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transition = event.geofenceTransition
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            transition != Geofence.GEOFENCE_TRANSITION_EXIT) return

        val timestamp = System.currentTimeMillis()
        val ids = event.triggeringGeofences?.map { it.requestId }.orEmpty()
        GeofenceEventStore(context).save(
            transition = transition,
            zoneIds = ids,
            timestampMs = timestamp
        )

        val zones = FamilyZoneStore(context).load()
        val names = ids.mapNotNull { id -> zones.firstOrNull { it.id == id }?.name }
        val place = names.ifEmpty { ids }.joinToString(", ").ifBlank { "una zona configurada" }
        val entering = transition == Geofence.GEOFENCE_TRANSITION_ENTER
        val alert = SmartAlert(
            id = "geofence_${transition}_${ids.sorted().joinToString("_")}_$timestamp",
            type = if (entering) AlertType.GEOFENCE_ENTER else AlertType.GEOFENCE_EXIT,
            severity = AlertSeverity.IMPORTANT,
            title = if (entering) "Entrada en geozona" else "Salida de geozona",
            message = if (entering) {
                "Se ha detectado una entrada en $place."
            } else {
                "Se ha detectado una salida de $place."
            },
            date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        )

        if (AlertStore(context).appendIfNew(alert)) {
            FamyrexNotificationManager.notify(context, alert)
        }
    }
}
