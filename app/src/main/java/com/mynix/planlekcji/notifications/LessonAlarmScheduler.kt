package com.mynix.planlekcji.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mynix.planlekcji.data.model.Lesson
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object LessonAlarmScheduler {

    fun scheduleAll(context: Context, lessons: List<Lesson>, minutesBefore: Int) {
        cancelAll(context, lessons)
        val am = context.getSystemService(AlarmManager::class.java) ?: return

        // Android 12+ wymaga uprawnienia SCHEDULE_EXACT_ALARM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) return  // cicho wyjdź — uprawnienie brak
        }

        val now = LocalDateTime.now()
        lessons.forEach { lesson ->
            try {
                val today = LocalDate.now().dayOfWeek.value
                val daysUntil = (lesson.dayOfWeek.value - today + 7) % 7
                var notifyDt = LocalDate.now()
                    .plusDays(daysUntil.toLong())
                    .atTime(lesson.startTime)
                    .minusMinutes(minutesBefore.toLong())

                // Jeśli czas już minął — przesuń o tydzień
                if (notifyDt.isBefore(now)) {
                    android.util.Log.d("AlarmScheduler", "Czas $notifyDt już minął (teraz: $now), przesuwam na kolejny tydzień")
                    notifyDt = notifyDt.plusWeeks(1)
                }

                val triggerMs = notifyDt
                    .atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()

                val intent = Intent(context, LessonAlarmReceiver::class.java).apply {
                    putExtra("subject", lesson.subject)
                    putExtra("room", lesson.room)
                    putExtra("minutes", minutesBefore)
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    lesson.id.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                android.util.Log.d("AlarmScheduler", "Planowanie alarmu dla ${lesson.subject} na ${notifyDt}")
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            } catch (e: Exception) {
                android.util.Log.e("AlarmScheduler", "Błąd planowania alarmu: ${e.message}")
            }
        }
    }

    fun cancelAll(context: Context, lessons: List<Lesson>) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        lessons.forEach { lesson ->
            val intent = Intent(context, LessonAlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                lesson.id.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let { 
                am.cancel(it)
                it.cancel()
            }
        }
    }
}
