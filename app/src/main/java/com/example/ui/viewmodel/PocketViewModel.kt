package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalDatabase
import com.example.data.model.Bookmark
import com.example.data.model.Course
import com.example.data.model.Department
import com.example.data.model.Resource
import com.example.data.repository.ResourceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PocketViewModel(
    application: Application,
    private val repository: ResourceRepository
) : AndroidViewModel(application) {

    // Global states
    val departments: StateFlow<List<Department>> = repository.allDepartments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteResources: StateFlow<List<Resource>> = repository.favoriteResources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineResources: StateFlow<List<Resource>> = repository.offlineResources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingResources: StateFlow<List<Resource>> = repository.pendingResources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isAdminMode = MutableStateFlow(false)
    val adminUserEmail = MutableStateFlow<String?>(null)
    val adminUserName = MutableStateFlow<String?>(null)

    // Browser navigation states
    val selectedDept = MutableStateFlow<Department?>(null)
    val selectedYear = MutableStateFlow(1)      // Year 1-4/5
    val selectedSemester = MutableStateFlow(1)  // Sem 1-2

    private val _coursesList = MutableStateFlow<List<Course>>(emptyList())
    val coursesList = _coursesList.asStateFlow()

    val selectedCourse = MutableStateFlow<Course?>(null)

    private val _courseResources = MutableStateFlow<List<Resource>>(emptyList())
    val courseResources = _courseResources.asStateFlow()

    // Offline synchronization and course progress tracking configurations
    val isAutoSyncEnabled = MutableStateFlow(false)

    val courseProgress: StateFlow<Map<String, Float>> = repository.getAllResources()
        .map { resources ->
            resources.groupBy { it.courseId }
                .mapValues { (_, resList) ->
                    val total = resList.size
                    if (total == 0) 0f else {
                        val studied = resList.count { it.lastReadPage > 0 }
                        studied.toFloat() / total
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Offline simulation mode
    val isInSimulationOfflineMode = MutableStateFlow(false)

    // Text Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Resource>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    // Download monitoring states
    private val _activeDownloads = MutableStateFlow<Map<String, Int>>(emptyMap())
    val activeDownloads = _activeDownloads.asStateFlow()

    // PDF Reading State
    val activeResource = MutableStateFlow<Resource?>(null)
    private val _activeResourceBookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val activeResourceBookmarks = _activeResourceBookmarks.asStateFlow()

    private var coursesJob: Job? = null
    private var resourcesJob: Job? = null
    private var bookmarksJob: Job? = null

    init {
        // Seed the DB on launch in a background thread
        viewModelScope.launch {
            repository.seedDatabase()
        }

        // Reactively listen to filter combo and reload course data
        viewModelScope.launch {
            combine(selectedDept, selectedYear, selectedSemester) { dept, year, sem ->
                Triple(dept, year, sem)
            }.collect { (dept, year, sem) ->
                coursesJob?.cancel()
                if (dept != null) {
                    coursesJob = viewModelScope.launch {
                        repository.getCourses(dept.id, year, sem).collect { list ->
                            _coursesList.value = list
                        }
                    }
                } else {
                    _coursesList.value = emptyList()
                }
            }
        }

        // Reactively listen to selected course and load resources
        viewModelScope.launch {
            selectedCourse.collect { course ->
                resourcesJob?.cancel()
                if (course != null) {
                    resourcesJob = viewModelScope.launch {
                        repository.getResourcesForCourse(course.id).collect { list ->
                            _courseResources.value = list
                        }
                    }
                } else {
                    _courseResources.value = emptyList()
                }
            }
        }

        // Reactively listen to active resource to fetch its bookmarks
        viewModelScope.launch {
            activeResource.collect { res ->
                bookmarksJob?.cancel()
                if (res != null) {
                    bookmarksJob = viewModelScope.launch {
                        repository.getBookmarksForResource(res.id).collect { list ->
                            _activeResourceBookmarks.value = list
                        }
                    }
                } else {
                    _activeResourceBookmarks.value = emptyList()
                }
            }
        }

        // Automatic synchronization of uploaded materials for offline-first usage
        viewModelScope.launch {
            combine(isAutoSyncEnabled, selectedCourse, courseResources, isInSimulationOfflineMode) { autoSync, course, resources, offline ->
                if (autoSync && course != null && !offline) {
                    resources.forEach { res ->
                        if (!res.isDownloaded && !res.isPendingApproval && !_activeDownloads.value.containsKey(res.id)) {
                            downloadResource(res)
                        }
                    }
                }
            }.collect()
        }
    }

    // Browse filtering setters
    fun selectDepartment(dept: Department?) {
        selectedDept.value = dept
        selectedYear.value = if (dept?.college == "Freshman Program") 1 else 2
        selectedCourse.value = null
        if (dept?.id == "FRESH-NS") {
            if (selectedSemester.value !in 1..3) {
                selectedSemester.value = 1
            }
        } else {
            if (selectedSemester.value !in 1..2) {
                selectedSemester.value = 1
            }
        }
    }

    fun selectYear(year: Int) {
        selectedYear.value = year
        selectedCourse.value = null
    }

    fun selectSemester(sem: Int) {
        selectedSemester.value = sem
        selectedCourse.value = null
    }

    fun selectCourse(course: Course?) {
        selectedCourse.value = course
    }

    // Toggle simulated hardware network switch
    fun setSimulationOfflineMode(offline: Boolean) {
        isInSimulationOfflineMode.value = offline
    }

    // Safe search updater with instant/debounce response
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.trim().isEmpty()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            repository.searchResources(query).collect { list ->
                _searchResults.value = list
            }
        }
    }

    // Toggle favorite state
    fun toggleFavorite(resource: Resource) {
        viewModelScope.launch {
            repository.toggleFavorite(resource.id, resource.isFavorite)
            // If the active resource is updated, refresh it too
            if (activeResource.value?.id == resource.id) {
                activeResource.value = resource.copy(isFavorite = !resource.isFavorite)
            }
        }
    }

    // Trigger high-fidelity download state transitions
    fun downloadResource(resource: Resource) {
        if (isInSimulationOfflineMode.value) return // Block if hardware simulation is offline
        if (resource.isDownloaded) return

        _activeDownloads.value = _activeDownloads.value + (resource.id to 0)
        viewModelScope.launch {
            repository.simulateDownload(resource.id) { progress ->
                _activeDownloads.value = _activeDownloads.value + (resource.id to progress)
                if (progress >= 100) {
                    _activeDownloads.value = _activeDownloads.value - resource.id
                }
            }
        }
    }

    private var activeResourceJob: Job? = null

    // In-built PDF Reading controls
    fun openResourceReader(resource: Resource) {
        activeResource.value = resource
        activeResourceJob?.cancel()
        activeResourceJob = viewModelScope.launch {
            // Keep activeResource flow locked to database updates
            repository.getResourceById(resource.id).collect { updated ->
                if (updated != null && activeResource.value?.id == updated.id) {
                    activeResource.value = updated
                }
            }
        }
    }

    fun closeResourceReader() {
        activeResourceJob?.cancel()
        activeResource.value = null
    }

    fun updateReadingPage(page: Int) {
        val current = activeResource.value ?: return
        viewModelScope.launch {
            repository.updateLastReadPage(current.id, page)
        }
    }

    fun addPageBookmark(pageNumber: Int, note: String) {
        val current = activeResource.value ?: return
        viewModelScope.launch {
            repository.addBookmark(current.id, pageNumber, note)
        }
    }

    fun removePageBookmark(bookmarkId: Int) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmarkId)
        }
    }

    fun insertCustomCourse(course: Course) {
        viewModelScope.launch {
            repository.addCustomCourse(course)
        }
    }

    fun insertCustomResource(resource: Resource) {
        viewModelScope.launch {
            repository.addCustomResource(resource)
        }
    }

    fun purgeOfflineResource(resource: Resource) {
        viewModelScope.launch {
            repository.purgeResource(resource.id)
        }
    }

    fun setAdminMode(enabled: Boolean) {
        if (!enabled) {
            isAdminMode.value = false
        } else {
            if (adminUserEmail.value?.lowercase() == "justabrish707@gmail.com") {
                isAdminMode.value = true
            }
        }
    }

    fun signInAdmin(email: String, name: String): Boolean {
        adminUserEmail.value = email.trim()
        adminUserName.value = name.trim()
        if (email.trim().lowercase() == "justabrish707@gmail.com") {
            isAdminMode.value = true
            return true
        } else {
            isAdminMode.value = false
            return false
        }
    }

    fun signOutAdmin() {
        adminUserEmail.value = null
        adminUserName.value = null
        isAdminMode.value = false
    }

    fun approveResource(resourceId: String) {
        viewModelScope.launch {
            repository.approveResource(resourceId)
        }
    }

    fun rejectResource(resourceId: String) {
        viewModelScope.launch {
            repository.rejectResource(resourceId)
        }
    }

    // Factory companion object
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PocketViewModel::class.java)) {
                val db = LocalDatabase.getDatabase(application)
                val repo = ResourceRepository(
                    db.departmentDao(),
                    db.courseDao(),
                    db.resourceDao(),
                    db.bookmarkDao()
                )
                @Suppress("UNCHECKED_CAST")
                return PocketViewModel(application, repo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
