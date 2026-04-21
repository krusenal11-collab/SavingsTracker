package com.savings.tracker

import android.app.Application
import com.savings.tracker.notification.NotificationHelper

/**
 * Custom Application class. Registered in AndroidManifest via android:name=".SavingsApp".
 * Runs once when the app process starts — perfect for one-time setup like notification channels.
 */
class SavingsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
