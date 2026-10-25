package com.example.geoguard.geofence

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.geoguard.data.repository.GroupRepository

/** Android clears all geofences on reboot. This worker fetches the user's groups and registers them again. */
class GeofenceResyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val groups = GroupRepository().getMyGroupsOnce()
        if (GeofenceManager(applicationContext).sync(groups)) Result.success() else Result.retry()
    } catch (e: Exception) {
        if (runAttemptCount < 5) Result.retry() else Result.failure()
    }

    companion object {
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<GeofenceResyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("geofence-resync", ExistingWorkPolicy.REPLACE, request)
        }
    }
}