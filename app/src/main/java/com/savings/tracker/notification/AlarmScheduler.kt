package com.savings.tracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {
    private const val REQUEST_CODE = 1001

    /**
     * Fix #22: Replaced locale-dependent Calendar.DAY_OF_WEEK with explicit
     * day-difference arithmetic to avoid locale-specific week start day bugs.
     */
    fun schedule(context: Context, dayOfWeek: Int, hour: Int, minute: Int) {
        val alarmManager  = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Calculate days to add to reach target day of week
        val todayDow   = now.get(Calendar.DAY_OF_WEEK)    // 1=Sun … 7=Sat
        var daysUntil  = (dayOfWeek - todayDow + 7) % 7
        // If same day but time already passed, schedule for next week
        if (daysUntil == 0 && target.timeInMillis <= now.timeInMillis) {
            daysUntil = 7
        }
        target.add(Calendar.DAY_OF_YEAR, daysUntil)

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            target.timeInMillis,
            pendingIntent
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context))
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
