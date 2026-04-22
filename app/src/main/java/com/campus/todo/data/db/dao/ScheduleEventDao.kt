package com.campus.todo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.campus.todo.data.db.entity.ScheduleEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleEventDao {
    @Query("SELECT * FROM schedule_events ORDER BY dateEpoch ASC, COALESCE(startAtEpoch, dateEpoch) ASC")
    fun observeAll(): Flow<List<ScheduleEvent>>

    @Query(
        """
        SELECT * FROM schedule_events
        WHERE dateEpoch BETWEEN :startEpoch AND :endEpoch
        ORDER BY dateEpoch ASC, COALESCE(startAtEpoch, dateEpoch) ASC
        """
    )
    fun observeBetween(startEpoch: Long, endEpoch: Long): Flow<List<ScheduleEvent>>

    @Query("SELECT * FROM schedule_events WHERE id = :id")
    suspend fun getById(id: Long): ScheduleEvent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ScheduleEvent): Long

    @Update
    suspend fun update(event: ScheduleEvent)

    @Query("DELETE FROM schedule_events WHERE id = :id")
    suspend fun deleteById(id: Long)
}
