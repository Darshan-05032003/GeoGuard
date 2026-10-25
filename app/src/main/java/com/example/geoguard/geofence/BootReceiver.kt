package com.example.geoguard.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-registers geofences after a reboot or an app update (both clear them). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> GeofenceResyncWorker.enqueue(context)
        }
    }
}