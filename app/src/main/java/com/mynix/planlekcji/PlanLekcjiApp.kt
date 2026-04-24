package com.mynix.planlekcji

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class PlanLekcjiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Kanał dla powiadomień przed lekcją
            val lessonsChannel = NotificationChannel(
                "lessons",
                "Powiadomienia o lekcjach",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Przypomnienia przed rozpoczęciem lekcji"
                enableVibration(true)
            }
            manager.createNotificationChannel(lessonsChannel)
        }
    }
}
