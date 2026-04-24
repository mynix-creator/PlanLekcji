package com.mynix.planlekcji.data.local

import androidx.room.*
import com.mynix.planlekcji.data.model.Lesson
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons")
    fun getAllLessons(): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getLessonsByDay(dayOfWeek: DayOfWeek): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    suspend fun getLessonsByDaySync(dayOfWeek: DayOfWeek): List<Lesson>

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getLessonById(id: Long): Lesson?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: Lesson): Long

    @Update
    suspend fun updateLesson(lesson: Lesson)

    @Delete
    suspend fun deleteLesson(lesson: Lesson)

    @Query("DELETE FROM lessons")
    suspend fun deleteAllLessons()
}
