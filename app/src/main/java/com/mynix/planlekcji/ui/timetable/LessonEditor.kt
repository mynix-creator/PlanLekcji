package com.mynix.planlekcji.ui.timetable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.mynix.planlekcji.data.model.Lesson
import com.mynix.planlekcji.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

import android.Manifest
import android.os.Build
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LessonEditor(
    lesson: Lesson?, // null for new lesson
    onDismiss: () -> Unit,
    onSave: (Lesson) -> Unit,
    onDelete: (Lesson) -> Unit
) {
    val postNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    var subject by remember { mutableStateOf(lesson?.subject ?: "") }
    var teacher by remember { mutableStateOf(lesson?.teacher ?: "") }
    var room by remember { mutableStateOf(lesson?.room ?: "") }
    var note by remember { mutableStateOf(lesson?.note ?: "") }
    var substituteTeacher by remember { mutableStateOf(lesson?.substituteTeacher ?: "") }
    var substituteSubject by remember { mutableStateOf(lesson?.substituteSubject ?: "") }
    
    var selectedDay by remember { mutableStateOf(lesson?.dayOfWeek ?: DayOfWeek.MONDAY) }
    var startTime by remember { mutableStateOf(lesson?.startTime ?: LocalTime.of(8, 0)) }
    var endTime by remember { mutableStateOf(lesson?.endTime ?: LocalTime.of(8, 45)) }
    var selectedColor by remember { mutableStateOf(lesson?.color ?: HighContrastColors.DarkBlue.toArgb()) }

    val colorOptions = HighContrastColors.AllColors

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (lesson == null) "Dodaj lekcję" else "Edytuj lekcję",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = subject,
            onValueChange = { 
                subject = it 
                if (lesson == null) {
                    selectedColor = HighContrastColors.getSubjectColor(it)
                }
            },
            label = { Text("Przedmiot") },
            modifier = Modifier.fillMaxWidth(),
            isError = subject.isBlank()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = teacher,
                onValueChange = { teacher = it },
                label = { Text("Nauczyciel") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Sala") },
                modifier = Modifier.weight(0.6f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = substituteTeacher,
            onValueChange = { substituteTeacher = it },
            label = { Text("Nauczyciel zastępujący (opcjonalnie)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = if (lesson?.substituteSubject == null && substituteSubject.isEmpty()) "" else substituteSubject,
            onValueChange = { substituteSubject = it },
            label = { Text("Przedmiot zastępujący (opcjonalnie)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Notatka (opcjonalnie)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Dzień tygodnia", style = MaterialTheme.typography.labelLarge)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY).forEach { day ->
                FilterChip(
                    selected = selectedDay == day,
                    onClick = { selectedDay = day },
                    label = { Text(day.getDisplayName(TextStyle.SHORT, Locale("pl"))) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Start", style = MaterialTheme.typography.labelLarge)
                Button(
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text(startTime.toString())
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Koniec", style = MaterialTheme.typography.labelLarge)
                Button(
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text(endTime.toString())
                }
            }
        }

    Spacer(modifier = Modifier.height(16.dp))
    Text("Kolor", style = MaterialTheme.typography.labelLarge)
    LazyRow(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(colorOptions) { color ->
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { selectedColor = color.toArgb() },
                contentAlignment = Alignment.Center
            ) {
                if (selectedColor == color.toArgb()) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Wybrany",
                        tint = Color.White
                    )
                }
            }
        }
    }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (lesson != null) {
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Usuń")
                }
            }
            Button(
                onClick = {
                    if (subject.isNotBlank() && endTime.isAfter(startTime)) {
                        onSave(
                            Lesson(
                                id = lesson?.id ?: 0,
                                subject = subject,
                                teacher = teacher,
                                room = room,
                                dayOfWeek = selectedDay,
                                startTime = startTime,
                                endTime = endTime,
                                color = selectedColor,
                                notifyMinutesBefore = 0,
                                note = note.ifBlank { null },
                                substituteTeacher = substituteTeacher.ifBlank { null },
                                substituteSubject = substituteSubject.ifBlank { null }
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = subject.isNotBlank() && endTime.isAfter(startTime)
            ) {
                Text("Zapisz")
            }
        }
    }

    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = startTime.hour, initialMinute = startTime.minute)
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    endTime = startTime.plusMinutes(45)
                    showStartTimePicker = false
                }) { Text("OK") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = endTime.hour, initialMinute = endTime.minute)
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showEndTimePicker = false
                }) { Text("OK") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showDeleteDialog && lesson != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Usuń lekcję") },
            text = { Text("Czy na pewno chcesz usunąć tę lekcję?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(lesson)
                    showDeleteDialog = false
                }) { Text("Usuń", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Anuluj") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        text = { content() }
    )
}
