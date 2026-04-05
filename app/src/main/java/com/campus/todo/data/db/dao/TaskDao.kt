package com.campus.todo.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
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

    @Query("SELECT * FROM tasks WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Task>

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

    @Query(
        """
        SELECT * FROM tasks
        WHERE status = 'PENDING' AND dueAtEpoch IS NOT NULL
        ORDER BY dueAtEpoch ASC
        """
    )
    suspend fun getAllPendingWithDue(): List<Task>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = :status")
    suspend fun countByStatus(status: TaskStatus): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'PENDING' AND courseId = :courseId")
    suspend fun countPendingByCourse(courseId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<Task>): List<Long>

    @Query("UPDATE tasks SET status = :status, updatedAtEpoch = :updated, completedAtEpoch = :completed WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TaskStatus, updated: Long, completed: Long?)

    @Query("UPDATE tasks SET status = :status, updatedAtEpoch = :updated, completedAtEpoch = :completed WHERE id IN (:ids)")
    suspend fun updateStatusBatch(ids: List<Long>, status: TaskStatus, updated: Long, completed: Long?)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM tasks WHERE status = :status")
    suspend fun deleteByStatus(status: TaskStatus)
}
