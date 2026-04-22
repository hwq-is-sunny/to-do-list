package com.campus.todo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.campus.todo.data.db.dao.CandidateDao
import com.campus.todo.data.db.dao.CourseDao
import com.campus.todo.data.db.dao.ScheduleEventDao
import com.campus.todo.data.db.dao.TaskDao
import com.campus.todo.data.db.dao.TimetableDao
import com.campus.todo.data.db.entity.CandidateItem
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.ScheduleEvent
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TimetableSlot

@Database(
    entities = [Course::class, TimetableSlot::class, Task::class, CandidateItem::class, ScheduleEvent::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun timetableDao(): TimetableDao
    abstract fun taskDao(): TaskDao
    abstract fun candidateDao(): CandidateDao
    abstract fun scheduleEventDao(): ScheduleEventDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "campus_todo.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
