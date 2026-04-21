package com.savings.tracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.savings.tracker.data.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Receives the weekly alarm broadcast.
 * 1. Shows the savings reminder notification.
 * 2. Immediately schedules the alarm for the same day/time next week.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Show the notification right away
        NotificationHelper.showReminder(context)

        // goAsync() tells Android "don't kill this receiver yet — we have async work to do".
        // Without it, the process can be killed before the coroutine finishes, breaking
        // the reschedule chain and meaning no more weekly reminders after the first one.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val prefs = PreferencesManager(context).notificationPrefs.first()
                if (prefs.enabled) {
                    AlarmScheduler.schedule(context, prefs.dayOfWeek, prefs.hour, prefs.minute)
                }
            } finally {
                pendingResult.finish() // Signal to Android that async work is complete
            }
        }
    }
}
