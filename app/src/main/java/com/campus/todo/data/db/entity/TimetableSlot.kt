package com.campus.todo.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timetable_slots",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId"), Index("dayOfWeek"), Index("dayOfWeek", "startMinuteOfDay")]
)
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    /** ISO: Monday=1 … Sunday=7 (java.time DayOfWeek) */
    val dayOfWeek: Int,
    /** Minutes from midnight, e.g. 8:00 -> 480 */
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val location: String? = null,
    val note: String? = null
)
