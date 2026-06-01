package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Bookmark
import com.example.data.model.Course
import com.example.data.model.Department
import com.example.data.model.Resource

@Database(
    entities = [Department::class, Course::class, Resource::class, Bookmark::class],
    version = 8,
    exportSchema = false
)
abstract class LocalDatabase : RoomDatabase() {

    abstract fun departmentDao(): DepartmentDao
    abstract fun courseDao(): CourseDao
    abstract fun resourceDao(): ResourceDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getDatabase(context: Context): LocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalDatabase::class.java,
                    "student_pocket_db_v8"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
