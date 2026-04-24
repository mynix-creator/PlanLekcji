package com.mynix.planlekcji.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.PendingIntent
import android.content.pm.PackageManager
import com.mynix.planlekcji.MainActivity

class LessonAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subject = intent.getStringExtra("subject") ?: return
        val room    = intent.getStringExtra("room") ?: ""
        val minutes = intent.getIntExtra("minutes", 10)

        // Upewnij się że kanał istnieje (na wypadek gdyby App.onCreate nie odpalił)
        ensureChannelExists(context)

        // Sprawdź uprawnienie (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) return
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        android.util.Log.d("AlarmReceiver", "Otrzymano alarm dla: $subject")

        val notification = NotificationCompat.Builder(context, "lessons")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(subject)
            .setContentText("Za $minutes min" + if (room.isNotBlank()) " • sala $room" else "")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(System.currentTimeMillis().toInt(), notification)
            android.util.Log.d("AlarmReceiver", "Powiadomienie wysłane pomyślnie")
        } catch (e: Exception) {
            android.util.Log.e("AlarmReceiver", "Błąd przy wysyłaniu powiadomienia: ${e.message}")
        }
    }

    private fun ensureChannelExists(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel("lessons") == null) {
                val channel = NotificationChannel(
                    "lessons",
                    "Powiadomienia o lekcjach",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Przypomnienia przed lekcją"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
