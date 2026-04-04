package com.campus.todo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.campus.todo.data.db.entity.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Course>>

    @Query("SELECT * FROM courses ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Course>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getById(id: Long): Course?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: Course): Long

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteById(id: Long)
}
