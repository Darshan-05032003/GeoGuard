package com.example.geoguard.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

/**
 * Called by the OS when the phone crosses a group's geofence, even if the app is not running.
 * A receiver only has ~10 seconds, so it does no network work itself: it hands the event to
 * WorkManager, which uploads it (and retries if the phone is offline).
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.e(TAG, "Geofence error: ${GeofenceStatusCodes.getStatusCodeString(event.errorCode)}")
            return
        }

        val type = GeofenceTransitionMapper.toEventType(event.geofenceTransition) ?: return
        // Capture the time of the crossing now, not when the upload eventually succeeds.
        val timestamp = System.currentTimeMillis()

        event.triggeringGeofences.orEmpty().forEach { geofence ->
            Log.d(TAG, "$type for group ${geofence.requestId}")
            ReportTransitionWorker.enqueue(context, geofence.requestId, type, timestamp)
        }
    }

    private companion object {
        const val TAG = "GeofenceReceiver"
    }
}