package com.example.geoguard.geofence

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.geoguard.data.repository.EventRepository
import java.util.concurrent.TimeUnit

/** Uploads one ENTER/EXIT event. Waits for network and retries with exponential backoff. */
class ReportTransitionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val groupId = inputData.getString(KEY_GROUP_ID) ?: return Result.failure()
        val type = inputData.getString(KEY_TYPE) ?: return Result.failure()
        val timestamp = inputData.getLong(KEY_TIMESTAMP, 0L).takeIf { it > 0 } ?: return Result.failure()

        val outcome = EventRepository().reportTransition(groupId, type, timestamp)
        return when {
            outcome.isSuccess -> Result.success()
            runAttemptCount < MAX_ATTEMPTS -> {
                Log.w(TAG, "Upload failed, will retry", outcome.exceptionOrNull())
                Result.retry()
            }
            else -> {
                Log.e(TAG, "Giving up after $runAttemptCount attempts", outcome.exceptionOrNull())
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "ReportTransitionWorker"
        private const val KEY_GROUP_ID = "groupId"
        private const val KEY_TYPE = "type"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val MAX_ATTEMPTS = 8

        fun enqueue(context: Context, groupId: String, type: String, timestamp: Long) {
            val request = OneTimeWorkRequestBuilder<ReportTransitionWorker>()
                .setInputData(
                    workDataOf(
                        KEY_GROUP_ID to groupId,
                        KEY_TYPE to type,
                        KEY_TIMESTAMP to timestamp
                    )
                )
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()

            // Unique name = same transition is never queued twice.
            WorkManager.getInstance(context).enqueueUniqueWork(
                "transition-$groupId-$type-$timestamp",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}