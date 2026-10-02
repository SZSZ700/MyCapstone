package com.example.myfinaltopapplication
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

// Channel ID for notifications.
private const val CHANNEL_ID = "water_reminders"

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Class to notify the user of events that happen.
        // This is how you tell the user that something has happened in the background.
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel (Android 8+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create notification channel.
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Water reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            // Register notification channel.
            nm.createNotificationChannel(channel)
        }

        // NotificationCompat -> Helper for accessing features in Notification.
        // Builder -> Builder class for NotificationCompat objects,
        // constructing the typical notification layouts.
        val b = NotificationCompat.Builder(context, CHANNEL_ID)
            // Set icon for notification.
            .setSmallIcon(R.drawable.waterdropmini)
            // Set title for notification.
            .setContentTitle("Water reminder")
            // Set content text.
            .setContentText("Time to drink water 💧")
            // Set notification to auto-cancel when tapped.
            .setAutoCancel(true)
            // Set priority of notification to default.
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Send notification with notification manager.
        nm.notify(1001, b.build())
    }
}