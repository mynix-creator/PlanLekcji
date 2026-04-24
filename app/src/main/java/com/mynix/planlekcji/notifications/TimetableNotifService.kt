package com.mynix.planlekcji.notifications

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mynix.planlekcji.MainActivity
import com.mynix.planlekcji.R
import com.mynix.planlekcji.data.local.AppDatabase
import com.mynix.planlekcji.data.model.Lesson
import kotlinx.coroutines.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class TimetableNotifService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var updateJob: Job? = null

    companion object {
        private const val CHANNEL_ID = "timetable_persistent_channel"
        private const val NOTIFICATION_ID = 1001
        
        fun startService(context: Context) {
            val intent = Intent(context, TimetableNotifService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimetableNotifService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("Ładowanie planu...", ""))
        
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            val database = AppDatabase.getDatabase(applicationContext)
            val lessonDao = database.lessonDao()
            
            while (isActive) {
                val now = LocalTime.now()
                val today = LocalDate.now().dayOfWeek
                
                // Only school days
                if (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY) {
                    updateNotification("Weekend - brak lekcji", "Odpocznij!")
                    delay(3600_000) // Check every hour on weekends
                    continue
                }

                val lessons = lessonDao.getLessonsByDaySync(today).sortedBy { it.startTime }
                
                val currentLesson = lessons.find { now >= it.startTime && now <= it.endTime }
                val nextLesson = lessons.find { it.startTime > now }

                if (currentLesson != null) {
                    val title = "Teraz: ${currentLesson.subject}"
                    val content = "Do ${currentLesson.endTime} (s. ${currentLesson.room})"
                    updateNotification(title, content)
                } else if (nextLesson != null) {
                    val title = "Następna: ${nextLesson.subject}"
                    val content = "O ${nextLesson.startTime} (s. ${nextLesson.room})"
                    updateNotification(title, content)
                } else {
                    updateNotification("Koniec lekcji na dziś", "Do zobaczenia jutro!")
                }

                delay(60_000) // Update every minute
            }
        }
        
        return START_STICKY
    }

    private fun updateNotification(title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(title, content))
    }

    private fun createNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this exists or use a generic one
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Plan Lekcji - Podgląd",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Pokazuje aktualną i następną lekcję na zablokowanym ekranie"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        updateJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
