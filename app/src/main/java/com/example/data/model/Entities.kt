package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "departments")
data class Department(
    @PrimaryKey val id: String,
    val name: String,
    val code: String,
    val description: String,
    val durationYears: Int = 4,
    val college: String? = "Other"
)

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey val id: String,
    val name: String,
    val code: String,
    val departmentId: String,
    val year: Int,      // 1 to 5
    val semester: Int   // 1 or 2
)

@Entity(tableName = "resources")
data class Resource(
    @PrimaryKey val id: String,
    val courseId: String,
    val title: String,
    val type: String,          // "Lecture Notes", "Handouts", "Assignments", "Lab Reports", "Past Exams", "Project Examples"
    val fileSize: String,      // e.g. "2.4 MB"
    val description: String,   // Course details/resource summary
    val pageCount: Int = 15,   // Simulated page count for PDF reader
    val localPath: String? = null,
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
    val lastReadPage: Int = 0,
    val downloadProgress: Int = 0, // for UI animating progress (0 - 100)
    val isPendingApproval: Boolean = false,
    val contributorName: String? = null
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val resourceId: String,
    val pageNumber: Int,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)
