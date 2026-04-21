package com.savings.tracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.savings.tracker.MainActivity
import com.savings.tracker.R

object NotificationHelper {
    private const val CHANNEL_ID      = "savings_reminder_channel"
    private const val CHANNEL_NAME    = "Savings Reminders"
    private const val NOTIFICATION_ID = 2001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Weekly reminder to transfer money to your savings account" }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    fun showReminder(context: Context) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPending = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Fix #34: Use our custom Nest leaf notification icon
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("💚 Savings Reminder")
            .setContentText("Time to transfer to your savings account!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Don't forget your weekly savings transfer. Tap to open the app and log your deposit."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }
}
