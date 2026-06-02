package com.example.data.local

import androidx.room.*
import com.example.data.model.Bookmark
import com.example.data.model.Course
import com.example.data.model.Department
import com.example.data.model.Resource
import kotlinx.coroutines.flow.Flow

@Dao
interface DepartmentDao {
    @Query("SELECT * FROM departments ORDER BY name ASC")
    fun getAllDepartments(): Flow<List<Department>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepartments(departments: List<Department>)
}

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses WHERE departmentId = :deptId AND year = :year AND semester = :semester ORDER BY name ASC")
    fun getCourses(deptId: String, year: Int, semester: Int): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE id = :courseId")
    suspend fun getCourseById(courseId: String): Course?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<Course>)
}

@Dao
interface ResourceDao {
    @Query("SELECT * FROM resources")
    fun getAllResources(): Flow<List<Resource>>

    @Query("SELECT * FROM resources WHERE courseId = :courseId AND isPendingApproval = 0 ORDER BY title ASC")
    fun getResourcesForCourse(courseId: String): Flow<List<Resource>>

    @Query("SELECT * FROM resources WHERE id = :id")
    fun getResourceById(id: String): Flow<Resource?>

    @Query("SELECT * FROM resources WHERE id = :id")
    suspend fun getResourceByIdDirect(id: String): Resource?

    @Query("SELECT * FROM resources WHERE isFavorite = 1 AND isPendingApproval = 0")
    fun getFavoriteResources(): Flow<List<Resource>>

    @Query("SELECT * FROM resources WHERE isDownloaded = 1 AND isPendingApproval = 0")
    fun getDownloadedResources(): Flow<List<Resource>>

    @Query("""
        SELECT resources.* FROM resources 
        INNER JOIN courses ON resources.courseId = courses.id 
        WHERE (resources.isPendingApproval = 0) AND (resources.title LIKE '%' || :query || '%' 
               OR courses.name LIKE '%' || :query || '%' 
               OR courses.code LIKE '%' || :query || '%' 
               OR resources.type LIKE '%' || :query || '%')
    """)
    fun searchResources(query: String): Flow<List<Resource>>

    @Query("SELECT * FROM resources WHERE isPendingApproval = 1 ORDER BY title ASC")
    fun getPendingResources(): Flow<List<Resource>>

    @Query("UPDATE resources SET isPendingApproval = 0 WHERE id = :id")
    suspend fun approveResource(id: String)

    @Query("DELETE FROM resources WHERE id = :id")
    suspend fun rejectResource(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResources(resources: List<Resource>)

    @Query("UPDATE resources SET isDownloaded = :isDownloaded, downloadProgress = :progress, localPath = :path WHERE id = :id")
    suspend fun updateDownloadState(id: String, isDownloaded: Boolean, progress: Int, path: String?)

    @Query("UPDATE resources SET downloadProgress = :progress WHERE id = :id")
    suspend fun updateDownloadProgress(id: String, progress: Int)

    @Query("UPDATE resources SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavoriteState(id: String, isFav: Boolean)

    @Query("UPDATE resources SET lastReadPage = :page WHERE id = :id")
    suspend fun updateLastReadPage(id: String, page: Int)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE resourceId = :resourceId ORDER BY pageNumber ASC")
    fun getBookmarksForResource(resourceId: String): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Int)
}
