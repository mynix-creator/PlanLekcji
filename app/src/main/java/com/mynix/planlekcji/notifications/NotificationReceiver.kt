package com.mynix.planlekcji.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mynix.planlekcji.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subject = intent.getStringExtra("subject") ?: "Lekcja"
        val room = intent.getStringExtra("room") ?: ""
        val minutesBefore = intent.getIntExtra("minutesBefore", 0)
        val lessonId = intent.getLongExtra("lessonId", 0L)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "lekcje"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lekcje",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val contentText = if (minutesBefore > 0) {
            "Za $minutesBefore min • sala $room"
        } else {
            "Zaczyna się teraz • sala $room"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Using launcher foreground for now
            .setContentTitle(subject)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(lessonId.toInt(), notification)
    }
}
