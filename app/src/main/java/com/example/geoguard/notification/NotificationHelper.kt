package com.example.geoguard.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.geoguard.MainActivity
import com.example.geoguard.R
import com.example.geoguard.ui.permissions.AppPermissions

object NotificationHelper {

    /** Must match the channelId the Cloud Function sends, and the default channel in the manifest. */
    const val CHANNEL_ZONE_ALERTS = "zone_alerts"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ZONE_ALERTS,
            "Zone alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when a group member leaves the group's zone"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Used when a push arrives while the app is in the foreground (FCM doesn't auto-display then). */
    @SuppressLint("MissingPermission")
    fun showZoneAlert(context: Context, title: String, body: String) {
        if (!AppPermissions.hasNotifications(context)) return

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ZONE_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
}