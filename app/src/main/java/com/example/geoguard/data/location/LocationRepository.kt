package com.example.geoguard.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class LocationRepository(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun hasFineLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<Location> = runCatching {
        check(hasFineLocationPermission()) { "Precise location permission is required" }
        val cancellation = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token).await()
            ?: error("Couldn't get your location. Turn on location and try again.")
    }
}