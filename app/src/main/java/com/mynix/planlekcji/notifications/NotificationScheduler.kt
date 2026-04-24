package com.mynix.planlekcji.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mynix.planlekcji.data.model.Lesson
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNotification(lesson: Lesson) {
        val notifyMinutes = lesson.notifyMinutesBefore ?: return
        if (notifyMinutes <= 0) {
            cancelNotification(lesson.id)
            return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("subject", lesson.subject)
            putExtra("room", lesson.room)
            putExtra("minutesBefore", notifyMinutes)
            putExtra("lessonId", lesson.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            lesson.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextOccurrence = getNextOccurrence(lesson.dayOfWeek, lesson.startTime)
        val triggerTime = nextOccurrence.minusMinutes(notifyMinutes.toLong())

        val triggerMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (System.currentTimeMillis() < triggerMillis) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelNotification(lessonId: Long) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            lessonId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun getNextOccurrence(dayOfWeek: DayOfWeek, time: LocalTime): LocalDateTime {
        val now = LocalDateTime.now()
        var scheduledDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(dayOfWeek))
        var scheduledDateTime = LocalDateTime.of(scheduledDate, time)

        if (scheduledDateTime.isBefore(now)) {
            scheduledDate = LocalDate.now().with(TemporalAdjusters.next(dayOfWeek))
            scheduledDateTime = LocalDateTime.of(scheduledDate, time)
        }
        return scheduledDateTime
    }
}
