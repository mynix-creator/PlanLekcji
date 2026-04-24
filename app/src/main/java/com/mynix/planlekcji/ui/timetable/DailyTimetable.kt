package com.mynix.planlekcji.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mynix.planlekcji.data.model.Lesson
import com.mynix.planlekcji.ui.theme.HighContrastColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

fun DayOfWeek.toShortPolishName() = when(this) {
    DayOfWeek.MONDAY    -> "Pon"
    DayOfWeek.TUESDAY   -> "Wt"
    DayOfWeek.WEDNESDAY -> "Śr"
    DayOfWeek.THURSDAY  -> "Czw"
    DayOfWeek.FRIDAY    -> "Pt"
    else -> ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTimetable(
    lessons: List<Lesson>,
    selectedDay: DayOfWeek,
    onDaySelected: (DayOfWeek) -> Unit,
    onLessonClick: (Lesson) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            kotlinx.coroutines.delay(60_000L)
        }
    }

    val schoolDays = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )

    val pagerState = rememberPagerState(
        initialPage = schoolDays.indexOf(selectedDay).coerceAtLeast(0),
        pageCount = { schoolDays.size }
    )

    val scope = rememberCoroutineScope()

    // Sync from pager to state (when swiping)
    LaunchedEffect(pagerState.currentPage) {
        if (schoolDays[pagerState.currentPage] != selectedDay) {
            onDaySelected(schoolDays[pagerState.currentPage])
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = schoolDays.indexOf(selectedDay).coerceAtLeast(0),
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
            indicator = {},
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            schoolDays.forEachIndexed { index, dow ->
                val isSelected = selectedDay == dow
                Tab(
                    selected = isSelected,
                    onClick = { 
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                            else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    text = { 
                        Text(
                            text = dow.toShortPolishName(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) 
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val dailyLessons = lessons.filter { it.dayOfWeek == schoolDays[page] }
                .sortedBy { it.startTime }

            if (dailyLessons.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Brak zajęć w tym dniu",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val now = LocalTime.now()
                    val today = LocalDate.now().dayOfWeek
                    val isToday = schoolDays[page] == today

                    dailyLessons.forEachIndexed { index, lesson ->
                        // If it's today and current time is before this lesson, and either it's the first lesson 
                        // or current time is after the previous lesson, show the indicator.
                        if (isToday) {
                            val prevLesson = if (index > 0) dailyLessons[index - 1] else null
                            val showIndicatorBefore = if (prevLesson == null) {
                                now < lesson.startTime
                            } else {
                                now > prevLesson.endTime && now < lesson.startTime
                            }
                            
                            if (showIndicatorBefore) {
                                item(key = "indicator_$index") {
                                    CurrentTimeIndicatorRow(currentTime)
                                }
                            }
                        }

                        item(key = lesson.id) {
                            val isActive = isToday && 
                                          now >= lesson.startTime && 
                                          now <= lesson.endTime
                            
                            DailyLessonCard(
                                lesson = lesson, 
                                onClick = { onLessonClick(lesson) },
                                isActive = isActive
                            )
                        }

                        // If it's today and this is the last lesson and time is after it
                        if (isToday && index == dailyLessons.size - 1 && now > lesson.endTime) {
                            item(key = "indicator_end") {
                                CurrentTimeIndicatorRow(currentTime)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentTimeIndicatorRow(time: LocalTime) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(
            text = time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Red,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        HorizontalDivider(color = Color.Red, thickness = 2.dp)
    }
}

@Composable
fun DailyLessonCard(lesson: Lesson, onClick: () -> Unit, isActive: Boolean = false) {
    val borderColor = if (isActive) Color.Red else Color.Transparent
    val cardColor = subjectColor(lesson.subject)
    val isSubstitute = !lesson.substituteSubject.isNullOrBlank() || !lesson.substituteTeacher.isNullOrBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSubstitute) 3.dp else 2.dp,
                color = if (isSubstitute) Color.Yellow else borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${lesson.startTime} - ${lesson.endTime}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (lesson.room.isNotEmpty()) {
                    Text(
                        text = "s. ${lesson.room}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isSubstitute) {
                Text(
                    text = "ZASTĘPSTWO",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.Yellow,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            val displaySubject = if (!lesson.substituteSubject.isNullOrBlank()) {
                "${lesson.subject} ➔ ${lesson.substituteSubject}"
            } else {
                lesson.subject
            }

            Text(
                text = displaySubject,
                color = if (isSubstitute) Color.Yellow else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2
            )
            
            val displayTeacher = if (!lesson.substituteTeacher.isNullOrBlank()) {
                "${lesson.teacher} ➔ ${lesson.substituteTeacher}"
            } else {
                lesson.teacher
            }

            if (displayTeacher.isNotBlank()) {
                Text(
                    text = displayTeacher,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            if (!lesson.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lesson.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}
