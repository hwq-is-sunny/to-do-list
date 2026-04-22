package com.campus.todo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.campus.todo.data.db.entity.TimetableSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query(
        """
        SELECT ts.* FROM timetable_slots ts
        INNER JOIN courses c ON c.id = ts.courseId
        ORDER BY ts.dayOfWeek, ts.startMinuteOfDay
        """
    )
    fun observeAll(): Flow<List<TimetableSlot>>

    @Query("SELECT * FROM timetable_slots WHERE courseId = :courseId ORDER BY dayOfWeek, startMinuteOfDay")
    fun observeForCourse(courseId: Long): Flow<List<TimetableSlot>>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dow ORDER BY startMinuteOfDay")
    suspend fun slotsForDay(dow: Int): List<TimetableSlot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(slot: TimetableSlot): Long

    @Query("DELETE FROM timetable_slots WHERE id = :id")
    suspend fun deleteById(id: Long)
}
