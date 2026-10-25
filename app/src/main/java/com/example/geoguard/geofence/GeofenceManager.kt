package com.example.geoguard.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.geoguard.data.model.Group
import com.example.geoguard.ui.permissions.AppPermissions
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

/**
 * Registers one OS-managed circular geofence per group the user belongs to.
 * Android (Play Services) watches the circles, even when the app is killed, and wakes
 * [GeofenceBroadcastReceiver] on ENTER/EXIT. This is far cheaper on battery than polling GPS.
 */
class GeofenceManager(context: Context) {

    private val appContext = context.applicationContext
    private val client = LocationServices.getGeofencingClient(appContext)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(appContext, GeofenceBroadcastReceiver::class.java)
        // FLAG_MUTABLE is required: Play Services adds the transition details to this intent.
        PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Replaces all registered geofences with the given groups and waits until Play Services confirms.
     * Returns false if permissions are missing or registration failed.
     */
    @SuppressLint("MissingPermission")
    suspend fun sync(groups: List<Group>): Boolean {
        if (!AppPermissions.canMonitorGeofences(appContext)) {
            Log.w(TAG, "Skipping geofence sync: background location not granted")
            return false
        }

        val geofences = groups
            .filter { it.id.isNotBlank() }
            .take(MAX_GEOFENCES)
            .map { group ->
                Geofence.Builder()
                    .setRequestId(group.id)
                    .setCircularRegion(group.zone.lat, group.zone.lng, group.zone.radiusMeters.toFloat())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()
            }

        return try {
            client.removeGeofences(pendingIntent).await()
            if (geofences.isNotEmpty()) {
                val request = GeofencingRequest.Builder()
                    // 0 = no initial trigger: re-syncing on every app start must not spam ENTER/EXIT events.
                    .setInitialTrigger(0)
                    .addGeofences(geofences)
                    .build()
                client.addGeofences(request, pendingIntent).await()
            }
            Log.d(TAG, "Registered ${geofences.size} geofence(s)")
            true
        } catch (e: Exception) {
            // Typical cause: location turned off in system settings (GEOFENCE_NOT_AVAILABLE).
            Log.e(TAG, "Failed to register geofences", e)
            false
        }
    }

    fun removeAll() {
        client.removeGeofences(pendingIntent)
    }

    companion object {
        private const val TAG = "GeofenceManager"
        private const val REQUEST_CODE = 1001
        /** Play Services allows up to 100 geofences per app. */
        const val MAX_GEOFENCES = 100
    }
}