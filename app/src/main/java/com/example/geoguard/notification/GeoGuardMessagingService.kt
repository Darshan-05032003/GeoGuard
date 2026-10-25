package com.example.geoguard.notification

import com.example.geoguard.data.repository.TokenRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class GeoGuardMessagingService : FirebaseMessagingService() {

    /**
     * FCM rotates tokens occasionally; keep Firestore up to date so pushes keep arriving.
     * This already runs on a background thread, and the service may stop right after it returns,
     * so we finish the save here (bounded by a timeout) instead of launching a coroutine that could be cancelled.
     */
    override fun onNewToken(token: String) {
        runBlocking {
            withTimeoutOrNull(10_000) { TokenRepository().saveToken(token) }
        }
    }

    /**
     * When the app is in the background the system shows the notification by itself.
     * When it's in the foreground FCM calls this instead, so we show it ourselves.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: return
        NotificationHelper.showZoneAlert(this, title, body)
    }
}