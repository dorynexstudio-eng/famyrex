package com.famyrex.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transition = event.geofenceTransition
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            transition != Geofence.GEOFENCE_TRANSITION_EXIT) return

        // Keep the event local for now. Notification/sync will be connected
        // in later roadmap phases, after family synchronization is implemented.
        val ids = event.triggeringGeofences?.map { it.requestId }.orEmpty()
        GeofenceEventStore(context).save(
            transition = transition,
            zoneIds = ids,
            timestampMs = System.currentTimeMillis()
        )
    }
}
