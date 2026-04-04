package com.campus.todo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.campus.todo.data.db.entity.CandidateItem
import com.campus.todo.data.db.entity.CandidateStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CandidateDao {
    @Query("SELECT * FROM candidates WHERE status = :status ORDER BY createdAtEpoch DESC")
    fun observeByStatus(status: CandidateStatus): Flow<List<CandidateItem>>

    @Query("SELECT * FROM candidates WHERE id = :id")
    suspend fun getById(id: Long): CandidateItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CandidateItem): Long

    @Update
    suspend fun update(item: CandidateItem)

    @Query("DELETE FROM candidates WHERE id = :id")
    suspend fun deleteById(id: Long)
}
