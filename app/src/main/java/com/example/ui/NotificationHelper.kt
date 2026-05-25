package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R

fun showNotification(context: Context, title: String, message: String) {
    val channelId = "debtsync_reminders"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "DebtSync Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for debt reminders and sync."
        }
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground) // using standard icon as placeholder
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
}
