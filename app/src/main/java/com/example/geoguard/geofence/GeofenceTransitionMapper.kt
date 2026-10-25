package com.example.geoguard.geofence

import com.example.geoguard.data.model.ZoneEvent
import com.google.android.gms.location.Geofence

object GeofenceTransitionMapper {

    /** Maps a Play Services transition code to our event type, or null if we don't report it. */
    fun toEventType(transition: Int): String? = when (transition) {
        Geofence.GEOFENCE_TRANSITION_EXIT -> ZoneEvent.TYPE_EXIT
        Geofence.GEOFENCE_TRANSITION_ENTER -> ZoneEvent.TYPE_ENTER
        else -> null
    }
}