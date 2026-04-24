package com.mynix.planlekcji.data.local

import androidx.room.*
import com.mynix.planlekcji.data.model.CustomEvent
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface EventDao {
    @Query("SELECT * FROM custom_events")
    fun getAllEvents(): Flow<List<CustomEvent>>

    @Query("SELECT * FROM custom_events WHERE date = :date ORDER BY startTime ASC")
    fun getEventsByDate(date: LocalDate): Flow<List<CustomEvent>>

    @Query("SELECT * FROM custom_events WHERE id = :id")
    suspend fun getEventById(id: Long): CustomEvent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CustomEvent)

    @Update
    suspend fun updateEvent(event: CustomEvent)

    @Delete
    suspend fun deleteEvent(event: CustomEvent)
}
