package com.example.geoguard

import android.app.Application
import com.example.geoguard.notification.NotificationHelper

class GeoGuardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}