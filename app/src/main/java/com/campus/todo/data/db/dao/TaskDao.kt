package com.campus.todo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TaskStatus
import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.UrgencyLevel
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY updatedAtEpoch DESC")
    fun observeAll(): Flow<List<Task>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE status = 'PENDING'
        ORDER BY 
            CASE WHEN dueAtEpoch IS NULL THEN 1 ELSE 0 END,
            dueAtEpoch ASC
        """
    )
    fun observeAllPending(): Flow<List<Task>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE status = 'PENDING' AND courseId = :courseId
        ORDER BY dueAtEpoch ASC, updatedAtEpoch DESC
        """
    )
    fun observePendingForCourse(courseId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: Long): Flow<Task?>

    @Query(
        """
        SELECT * FROM tasks
        WHERE status = 'PENDING'
        AND dueAtEpoch IS NOT NULL
        AND dueAtEpoch >= :startInclusive
        AND dueAtEpoch < :endExclusive
        ORDER BY dueAtEpoch ASC
        """
    )
    suspend fun pendingDueBetween(startInclusive: Long, endExclusive: Long): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Query("UPDATE tasks SET title = :title, updatedAtEpoch = :updated WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String, updated: Long)

    @Query(
        """
        UPDATE tasks SET title = :title, description = :description, dueAtEpoch = :dueAt,
        location = :location, taskType = :taskType, category = :category, urgency = :urgency,
        reminderAtEpoch = :reminderAt, updatedAtEpoch = :updated WHERE id = :id
        """
    )
    suspend fun updateEditableFields(
        id: Long,
        title: String,
        description: String?,
        dueAt: Long?,
        location: String?,
        taskType: TaskType,
        category: String,
        urgency: UrgencyLevel,
        reminderAt: Long?,
        updated: Long
    )

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE tasks SET status = :status, updatedAtEpoch = :updated, completedAtEpoch = :completed WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TaskStatus, updated: Long, completed: Long?)

    @Query(
        """
        SELECT * FROM tasks
        WHERE status = 'PENDING' AND dueAtEpoch IS NOT NULL
        ORDER BY dueAtEpoch ASC
        """
    )
    suspend fun getAllPendingWithDue(): List<Task>
}
