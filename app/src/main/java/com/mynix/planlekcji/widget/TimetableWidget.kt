package com.mynix.planlekcji.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import com.mynix.planlekcji.data.local.AppDatabase
import com.mynix.planlekcji.data.model.Lesson
import java.time.*
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import com.mynix.planlekcji.MainActivity

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import androidx.glance.appwidget.updateAll
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimetableWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val data = getLessonsForWidget(db.lessonDao())

        provideContent {
            GlanceTheme {
                TimetableWidgetContent(data)
            }
        }
    }

    private suspend fun getLessonsForWidget(dao: com.mynix.planlekcji.data.local.LessonDao): WidgetData {
        val now = LocalTime.now()
        val today = LocalDate.now().dayOfWeek
        val todayInt = if (today in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) null else today.value
        
        val lessons = todayInt?.let {
            dao.getLessonsByDay(DayOfWeek.of(it)).first()
        } ?: emptyList()

        val current = lessons.firstOrNull { it.startTime <= now && it.endTime > now }
        val next = lessons.firstOrNull { it.startTime > now }
        val minsToNext = next?.let {
            ChronoUnit.MINUTES.between(now, it.startTime).toInt()
        }
        return WidgetData(current, next, minsToNext)
    }

    data class WidgetData(
        val current: Lesson?,
        val next: Lesson?,
        val minsToNext: Int?
    )

    @Composable
    private fun TimetableWidgetContent(data: WidgetData) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF0D0D0D))) // Darker for better contrast
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT — next lesson
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = if (data.next != null) "NASTĘPNA" else "KONIEC LEKCJI",
                    style = TextStyle(
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = ColorProvider(Color(0xFFBBBBBB))
                    )
                )
                if (data.next != null) {
                    val lessonColor = Color(data.next.color)
                    Text(
                        text = data.next.substituteSubject ?: data.next.subject,
                        style = TextStyle(
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(lessonColor)
                        ),
                        maxLines = 1
                    )
                    val countdown = when {
                        (data.minsToNext ?: 0) <= 0 -> "zaraz"
                        (data.minsToNext ?: 0) < 60 -> "za ${data.minsToNext} min"
                        else -> "za ${data.minsToNext!! / 60}h ${data.minsToNext % 60}m"
                    }
                    Text(
                        text = "${data.next.startTime}  •  $countdown",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = if ((data.minsToNext ?: 99) <= 5) ColorProvider(Color(0xFFFF5252)) else ColorProvider(Color(0xFFDDDDDD))
                        )
                    )
                }
            }

            // DIVIDER
            Box(
                modifier = GlanceModifier
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .background(ColorProvider(Color(0xFF333333)))
            ) {}

            Spacer(GlanceModifier.width(12.dp))

            // RIGHT — current lesson
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = if (data.current != null) "TERAZ" else "BRAK LEKCJI",
                    style = TextStyle(
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = ColorProvider(Color(0xFFBBBBBB))
                    )
                )
                if (data.current != null) {
                    val lessonColor = Color(data.current.color)
                    Text(
                        text = data.current.substituteSubject ?: data.current.subject,
                        style = TextStyle(
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(lessonColor)
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "s.${data.current.room}  do ${data.current.endTime}",
                        style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color(0xFFDDDDDD)))
                    )
                } else if (data.next == null) {
                    Text(
                        text = "Do jutra!",
                        style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color(0xFFDDDDDD)))
                    )
                }
            }
        }
    }
}

class TimetableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TimetableWidget()

    companion object {
        const val ACTION_MINUTE_TICK = "com.mynix.planlekcji.ACTION_MINUTE_TICK"
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleMinuteUpdates(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_MINUTE_TICK || intent.action == Intent.ACTION_TIME_TICK) {
            CoroutineScope(Dispatchers.IO).launch {
                TimetableWidget().updateAll(context)
            }
        }
    }

    private fun scheduleMinuteUpdates(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, TimetableWidgetReceiver::class.java).apply { action = ACTION_MINUTE_TICK },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextMinute = System.currentTimeMillis() / 60000 * 60000 + 60000
        am.setRepeating(AlarmManager.RTC, nextMinute, 60_000L, intent)
    }
}
