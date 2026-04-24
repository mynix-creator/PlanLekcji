package com.mynix.planlekcji.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalTime

@Entity(tableName = "lessons")
data class Lesson(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val teacher: String,
    val room: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val color: Int,
    val notifyMinutesBefore: Int? = null,
    val note: String? = null,
    val substituteTeacher: String? = null,
    val substituteSubject: String? = null
)
