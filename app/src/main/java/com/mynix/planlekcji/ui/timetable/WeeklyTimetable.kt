package com.mynix.planlekcji.ui.timetable

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mynix.planlekcji.data.model.Lesson
import com.mynix.planlekcji.ui.theme.HighContrastColors
import com.mynix.planlekcji.ui.timetable.WeekViewDefaults.DAY_COLUMN_WIDTH
import com.mynix.planlekcji.ui.timetable.WeekViewDefaults.END_HOUR
import com.mynix.planlekcji.ui.timetable.WeekViewDefaults.HOUR_HEIGHT_DP
import com.mynix.planlekcji.ui.timetable.WeekViewDefaults.START_HOUR
import com.mynix.planlekcji.ui.timetable.WeekViewDefaults.TIME_GUTTER_WIDTH
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

@Composable
fun WeeklyTimetable(
    lessons: List<Lesson>,
    onLessonClick: (Lesson) -> Unit,
    onEmptySpaceLongClick: (DayOfWeek, LocalTime) -> Unit,
    onDayHeaderClick: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()
    
    val days = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )

    val density = LocalDensity.current
    val dayColumnWidthPx = with(density) { DAY_COLUMN_WIDTH.toPx() }

    // Auto-scroll to today
    LaunchedEffect(Unit) {
        val today = java.time.LocalDate.now().dayOfWeek
        val dayIndex = days.indexOf(today)
        if (dayIndex > 0) {
            hScroll.scrollTo((dayIndex * dayColumnWidthPx).toInt())
        }
    }
    
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(60_000L)
        }
    }

    val hourHeightPx = with(density) { HOUR_HEIGHT_DP.dp.toPx() }
    val totalGridHeight = ((END_HOUR - START_HOUR + 1) * HOUR_HEIGHT_DP).toFloat()

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0F0F0F))) {
        // Sticky Header Row
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(TIME_GUTTER_WIDTH))
            Row(modifier = Modifier.horizontalScroll(hScroll)) {
                days.forEach { day ->
                    DayHeaderItem(day = day, onClick = { onDayHeaderClick(day) })
                }
            }
        }

        // Scrollable Body
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(vScroll)
        ) {
            // TIME GUTTER — fixed, outside horizontal scroll
            TimeGutter(
                currentTime = currentTime,
                modifier = Modifier
                    .width(TIME_GUTTER_WIDTH)
                    .height(totalGridHeight.dp)
            )

            // DAY COLUMNS — scroll horizontally only
            Row(modifier = Modifier.horizontalScroll(hScroll)) {
                days.forEach { day ->
                    key(day) {
                        DayColumn(
                            day = day,
                            lessons = lessons,
                            onLessonClick = onLessonClick,
                            onEmptySpaceLongClick = onEmptySpaceLongClick,
                            hourHeightPx = hourHeightPx,
                            gridHeight = totalGridHeight,
                            currentTime = currentTime
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeGutter(currentTime: LocalTime, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        for (hour in START_HOUR..END_HOUR) {
            Box(
                modifier = Modifier
                    .height(HOUR_HEIGHT_DP.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                val isCurrentHour = hour == currentTime.hour
                Text(
                    text = String.format("%02d:00", hour),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrentHour) Color.Red else Color(0xFF9E9E9E),
                    fontWeight = if (isCurrentHour) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DayHeaderItem(day: DayOfWeek, onClick: () -> Unit) {
    val dayName = day.getDisplayName(TextStyle.FULL, Locale("pl"))
    Box(
        modifier = Modifier
            .width(DAY_COLUMN_WIDTH)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayName.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun DayColumn(
    day: DayOfWeek,
    lessons: List<Lesson>,
    onLessonClick: (Lesson) -> Unit,
    onEmptySpaceLongClick: (DayOfWeek, LocalTime) -> Unit,
    hourHeightPx: Float,
    gridHeight: Float,
    currentTime: LocalTime
) {
    val isToday = java.time.LocalDate.now().dayOfWeek == day
    val dayLessons = remember(lessons, day) {
        lessons.filter { it.dayOfWeek == day }.sortedBy { it.startTime }
    }
    
    Box(
        modifier = Modifier
            .width(DAY_COLUMN_WIDTH)
            .height(gridHeight.dp)
            .background(if (isToday) Color(0xFF90CAF9).copy(alpha = 0.05f) else Color.Transparent)
            .border(0.5.dp, Color(0xFF222222))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        val totalMinutes = (offset.y / hourHeightPx) * 60
                        val hour = START_HOUR + (totalMinutes / 60).toInt()
                        val minute = (totalMinutes % 60).toInt()
                        if (hour in START_HOUR..END_HOUR) {
                            onEmptySpaceLongClick(day, LocalTime.of(hour, (minute / 5) * 5))
                        }
                    }
                )
            }
    ) {
        // Horizontal lines for hours
        for (i in 0..(END_HOUR - START_HOUR)) {
            HorizontalDivider(
                modifier = Modifier.offset(y = (i * HOUR_HEIGHT_DP).dp),
                color = Color(0xFF222222),
                thickness = 0.5.dp
            )
        }

        dayLessons.forEach { lesson ->
            key(lesson.id) {
                LessonItem(
                    lesson = lesson,
                    onClick = { onLessonClick(lesson) }
                )
            }
        }

        // Current time indicator
        if (currentTime.hour in START_HOUR..END_HOUR) {
            val minutesFromStart = (currentTime.hour - START_HOUR) * 60 + currentTime.minute
            val topDp = (minutesFromStart / 60f) * HOUR_HEIGHT_DP
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = topDp.dp)
                    .zIndex(10f),
                contentAlignment = Alignment.CenterStart
            ) {
                HorizontalDivider(
                    color = Color.Red.copy(alpha = if (isToday) 1f else 0.25f),
                    thickness = 2.dp
                )
                if (isToday) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .offset(x = (-4).dp)
                            .background(Color.Red, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun LessonItem(
    lesson: Lesson,
    onClick: () -> Unit
) {
    val startMinutes = (lesson.startTime.hour - START_HOUR) * 60 + lesson.startTime.minute
    val durationMinutes = (lesson.endTime.hour - lesson.startTime.hour) * 60 + (lesson.endTime.minute - lesson.startTime.minute)
    
    val topDp = (startMinutes / 60f) * HOUR_HEIGHT_DP
    val heightDp = (durationMinutes / 60f) * HOUR_HEIGHT_DP

    val cardColor = subjectColor(lesson.subject)
    val isSubstitute = !lesson.substituteSubject.isNullOrBlank() || !lesson.substituteTeacher.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .offset(y = topDp.dp)
            .padding(horizontal = 2.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(cardColor.copy(alpha = 0.92f))
            .border(
                width = if (isSubstitute) 2.dp else 3.dp,
                color = if (isSubstitute) Color.Yellow else cardColor,
                shape = if (isSubstitute) RoundedCornerShape(8.dp) else RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            val displaySubject = lesson.substituteSubject ?: lesson.subject
            Text(
                text = displaySubject,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Bold,
                    color = if (isSubstitute) Color.Yellow else Color.White
                ),
                maxLines = 2,
                lineHeight = 10.sp,
                textAlign = TextAlign.Start
            )
            if (heightDp > 40) {
                val displayTeacher = lesson.substituteTeacher ?: lesson.teacher
                Text(
                    text = displayTeacher,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    textAlign = TextAlign.Start
                )
            }
            if (heightDp > 25 && lesson.room.isNotEmpty()) {
                Text(
                    text = lesson.room,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
