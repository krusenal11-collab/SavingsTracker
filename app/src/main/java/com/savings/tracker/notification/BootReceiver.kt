package com.savings.tracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.savings.tracker.data.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * AlarmManager alarms are cleared when the device reboots.
 * This receiver listens for BOOT_COMPLETED and reschedules the alarm
 * automatically so the user never misses a reminder.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // goAsync() keeps the receiver process alive while we read DataStore
        // and reschedule the alarm asynchronously.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val prefs = PreferencesManager(context).notificationPrefs.first()
                if (prefs.enabled) {
                    AlarmScheduler.schedule(context, prefs.dayOfWeek, prefs.hour, prefs.minute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
