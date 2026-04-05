package com.campus.todo.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    @Query("SELECT * FROM courses WHERE name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<Course>

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: Course): Long

    @Update
    suspend fun update(course: Course)

    @Delete
    suspend fun delete(course: Course)

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteById(id: Long)
}
