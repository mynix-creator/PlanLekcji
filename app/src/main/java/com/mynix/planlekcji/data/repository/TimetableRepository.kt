package com.mynix.planlekcji.data.repository

import com.mynix.planlekcji.data.local.EventDao
import com.mynix.planlekcji.data.local.LessonDao
import com.mynix.planlekcji.data.model.CustomEvent
import com.mynix.planlekcji.data.model.Lesson
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate

class TimetableRepository(
    private val lessonDao: LessonDao,
    private val eventDao: EventDao
) {
    // Lessons
    fun getAllLessons(): Flow<List<Lesson>> = lessonDao.getAllLessons()
    
    fun getLessonsByDay(dayOfWeek: DayOfWeek): Flow<List<Lesson>> = lessonDao.getLessonsByDay(dayOfWeek)
    
    suspend fun getLessonById(id: Long): Lesson? = lessonDao.getLessonById(id)
    
    suspend fun insertLesson(lesson: Lesson): Long = lessonDao.insertLesson(lesson)
    
    suspend fun updateLesson(lesson: Lesson) = lessonDao.updateLesson(lesson)
    
    suspend fun deleteLesson(lesson: Lesson) = lessonDao.deleteLesson(lesson)

    // Events
    fun getAllEvents(): Flow<List<CustomEvent>> = eventDao.getAllEvents()
    
    fun getEventsByDate(date: LocalDate): Flow<List<CustomEvent>> = eventDao.getEventsByDate(date)
    
    suspend fun getEventById(id: Long): CustomEvent? = eventDao.getEventById(id)
    
    suspend fun insertEvent(event: CustomEvent) = eventDao.insertEvent(event)
    
    suspend fun updateEvent(event: CustomEvent) = eventDao.updateEvent(event)
    
    suspend fun deleteEvent(event: CustomEvent) = eventDao.deleteEvent(event)
}
