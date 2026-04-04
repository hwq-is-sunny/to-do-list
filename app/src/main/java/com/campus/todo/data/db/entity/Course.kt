package com.campus.todo.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String? = null,
    /** ARGB color for UI chips */
    val colorArgb: Int = 0xFF5B8FA8.toInt()
)
