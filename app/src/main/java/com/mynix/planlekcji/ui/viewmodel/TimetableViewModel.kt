package com.mynix.planlekcji.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.toArgb
import com.mynix.planlekcji.data.IcsParser
import com.mynix.planlekcji.data.local.AppDatabase
import com.mynix.planlekcji.data.model.Lesson
import com.mynix.planlekcji.ui.timetable.subjectColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

class TimetableViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val lessonDao = db.lessonDao()

    private val _importResult = MutableStateFlow<ImportStatus?>(null)
    val importResult: StateFlow<ImportStatus?> = _importResult.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    data class ImportStatus(val success: Boolean, val count: Int, val errorCount: Int, val source: String)

    fun clearImportResult() {
        _importResult.value = null
    }

    val lessons: StateFlow<List<Lesson>> = lessonDao.getAllLessons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun lessonsForDay(day: DayOfWeek): Flow<List<Lesson>> = lessons.map { list ->
        list.filter { it.dayOfWeek == day }.sortedBy { it.startTime }
    }

    fun insertLesson(lesson: Lesson) {
        viewModelScope.launch(Dispatchers.IO) {
            lessonDao.insertLesson(lesson)
        }
    }

    fun updateLesson(lesson: Lesson) {
        viewModelScope.launch(Dispatchers.IO) {
            lessonDao.updateLesson(lesson)
        }
    }

    fun deleteLesson(lesson: Lesson) {
        viewModelScope.launch(Dispatchers.IO) {
            lessonDao.deleteLesson(lesson)
        }
    }

    fun clearAllLessons() {
        viewModelScope.launch(Dispatchers.IO) {
            val allLessons = lessonDao.getAllLessons().first()
            lessonDao.deleteAllLessons()
            com.mynix.planlekcji.notifications.LessonAlarmScheduler.cancelAll(getApplication(), allLessons)
        }
    }

    fun importFromIcs(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isImporting.value = true
            try {
                val stream = getApplication<Application>().contentResolver.openInputStream(uri) ?: return@launch
                val parsed = IcsParser.parse(stream)
                stream.close()

                if (parsed.isEmpty()) {
                    _importResult.value = ImportStatus(false, 0, 0, "")
                    return@launch
                }

                lessonDao.deleteAllLessons()
                parsed.forEach { p ->
                    lessonDao.insertLesson(
                        Lesson(
                            dayOfWeek = DayOfWeek.of(p.dayOfWeek),
                            startTime = p.startTime,
                            endTime = p.endTime,
                            subject = p.subject,
                            teacher = p.teacher,
                            room = "",   // Librus does not provide room
                            color = subjectColor(p.subject).toArgb(),
                            notifyMinutesBefore = 0,
                            note = null,
                            substituteTeacher = null,
                            substituteSubject = null
                        )
                    )
                }
                _importResult.value = ImportStatus(true, parsed.size, 0, "Librus ICS")
            } catch (e: Exception) {
                _importResult.value = ImportStatus(false, 0, 1, "Błąd: ${e.message}")
            } finally {
                _isImporting.value = false
            }
        }
    }
}
