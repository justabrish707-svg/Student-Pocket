package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import com.example.data.model.Course
import com.example.data.model.Resource
import com.example.ui.viewmodel.PocketViewModel

data class AttachedMaterial(
    val uri: Uri,
    val name: String,
    val size: String,
    val localPath: String,
    val title: String,
    val type: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseBrowserScreen(
    viewModel: PocketViewModel,
    onBack: () -> Unit,
    onResourceClick: (Resource) -> Unit
) {
    val selectedDept by viewModel.selectedDept.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedSemester by viewModel.selectedSemester.collectAsState()
    val coursesList by viewModel.coursesList.collectAsState()
    val courseProgressMap by viewModel.courseProgress.collectAsState()
    val selectedCourse by viewModel.selectedCourse.collectAsState()
    val courseResources by viewModel.courseResources.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val isOfflineMode by viewModel.isInSimulationOfflineMode.collectAsState()

    var showOfflineErrorDialog by remember { mutableStateOf(false) }
    var offlineErrorResourceTitle by remember { mutableStateOf("") }

    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showAddMaterialDialog by remember { mutableStateOf(false) }
    var showSuccessUploadDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val contentResolver = context.contentResolver

    var contributorName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isDescError by remember { mutableStateOf(false) }
    var selectedCommonType by remember { mutableStateOf("Lecture Notes") }

    var attachedMaterials by remember { mutableStateOf<List<AttachedMaterial>>(emptyList()) }
    var uploadListErrorMsg by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        if (uris != null && uris.isNotEmpty()) {
            uploadListErrorMsg = null
            val newList = attachedMaterials.toMutableList()
            for (uri in uris) {
                var displayName = "Attached Material"
                var sizeInBytes: Long = 0
                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) displayName = cursor.getString(nameIndex)
                            if (sizeIndex != -1) sizeInBytes = cursor.getLong(sizeIndex)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val sizeStr = if (sizeInBytes > 0) {
                    String.format("%.1f MB", sizeInBytes / (1024.0 * 1024.0))
                } else {
                    String.format("%.1f MB", 1.0 + Math.random() * 4)
                }

                val ext = displayName.substringAfterLast(".").lowercase()
                val inferredType = if (ext == "pdf") "Lecture Notes" else "Lecture Notes"

                // Save file securely to custom materials folder
                try {
                    val fileNameUnique = "file_${System.currentTimeMillis()}_${(100..999).random()}_$displayName"
                    val destFolder = File(context.filesDir, "custom_materials")
                    destFolder.mkdirs()
                    val destFile = File(destFolder, fileNameUnique)
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    newList.add(
                        AttachedMaterial(
                            uri = uri,
                            name = displayName,
                            size = sizeStr,
                            localPath = destFile.absolutePath,
                            title = displayName.substringBeforeLast("."),
                            type = inferredType
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            attachedMaterials = newList
        }
    }

    // Reset contribution form states when launched
    LaunchedEffect(showAddMaterialDialog) {
        if (showAddMaterialDialog) {
            contributorName = ""
            description = ""
            isDescError = false
            selectedCommonType = "Lecture Notes"
            attachedMaterials = emptyList()
            uploadListErrorMsg = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedDept?.name ?: "Browse Department",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        val subtitleText = when {
                            selectedDept?.id == "FRESH-NS" -> {
                                when (selectedSemester) {
                                    1 -> "Freshman • 1st Semester"
                                    2 -> "Freshman • pre-Engineering"
                                    3 -> "Freshman • Other Natural Sciences"
                                    else -> "Freshman"
                                }
                            }
                            selectedDept?.college == "Freshman Program" -> {
                                "Freshman • Semester ${if (selectedSemester == 1) "I" else "II"}"
                            }
                            else -> {
                                "Year $selectedYear • Sem ${if (selectedSemester == 1) "I" else "II"}"
                            }
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedCourse != null) {
                            viewModel.selectCourse(null)
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedCourse == null) {
                        showAddCourseDialog = true
                    } else {
                        showAddMaterialDialog = true
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Contribute Button") },
                text = { Text(if (selectedCourse == null) "Add Course" else "Add Material") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("contribute_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Displays filter navigation (Years + Semester Selector) ONLY if no course is selected
            AnimatedVisibility(
                visible = selectedCourse == null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    // Year filter Pills
                    Text(
                        text = "SELECT ACADEMIC YEAR",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isFreshman = selectedDept?.college == "Freshman Program"
                        val duration = selectedDept?.durationYears ?: 4
                        val years = if (isFreshman) listOf(1) else (2..duration).toList()
                        items(years) { year ->
                            FilterChip(
                                selected = selectedYear == year,
                                onClick = { viewModel.selectYear(year) },
                                label = { Text("Year $year") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("filter_year_$year")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Semester toggle row
                    Text(
                        text = "SELECT SEMESTER",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    
                    val semesters = if (selectedDept?.id == "FRESH-NS") {
                        listOf(1, 2, 3)
                    } else {
                        listOf(1, 2)
                    }

                    val semesterScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { 
                                if (semesters.size > 2) it.horizontalScroll(semesterScrollState) else it 
                            }
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        semesters.forEach { sem ->
                            val semLabel = when {
                                selectedDept?.id == "FRESH-NS" -> {
                                    when (sem) {
                                        1 -> "1st Semester"
                                        2 -> "pre-Engineering"
                                        3 -> "Other natural sciences"
                                        else -> "Sem $sem"
                                    }
                                }
                                else -> if (sem == 1) "Semester I" else "Semester II"
                            }
                            Row(
                                modifier = Modifier
                                    .let { 
                                        if (semesters.size <= 2) it.weight(1f) else it 
                                    }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selectedSemester == sem) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { viewModel.selectSemester(sem) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (selectedSemester == sem) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Semester select status indicator",
                                    tint = if (selectedSemester == sem) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = semLabel,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedSemester == sem) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }

            // Central content: Courses List of active department / selected academic course resources
            if (selectedCourse == null) {
                // List of courses
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Courses Syllabus",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }

                    var searchFilterQuery by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = searchFilterQuery,
                        onValueChange = { searchFilterQuery = it },
                        placeholder = { Text("Search syllabus, codes...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchFilterQuery.isNotEmpty()) {
                                IconButton(onClick = { searchFilterQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("course_search_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    val filteredCourses = remember(coursesList, searchFilterQuery) {
                        if (searchFilterQuery.isBlank()) {
                            coursesList
                        } else {
                            coursesList.filter {
                                it.name.contains(searchFilterQuery, ignoreCase = true) ||
                                it.code.contains(searchFilterQuery, ignoreCase = true)
                            }
                        }
                    }
                    
                    if (filteredCourses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = "No courses icon",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (coursesList.isEmpty()) "No course handouts loaded for this semester." else "No courses found matching \"$searchFilterQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredCourses) { course ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectCourse(course) }
                                        .testTag("course_item_${course.id}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = course.code.take(2).uppercase(),
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = course.name,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = course.code,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Open course details arrow",
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                        }

                                        // Progress indicator showing studies completed
                                        val progress = courseProgressMap[course.id] ?: 0f
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LinearProgressIndicator(
                                                progress = progress,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = if (progress >= 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            Text(
                                                text = "${(progress * 100).toInt()}% studied",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (progress >= 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Focus: Specific Course hand-outs grouped by type
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    // Header Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Active book context",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedCourse?.code ?: "",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedCourse?.name ?: "",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }

                    if (courseResources.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Preparing specific PDFs and handouts.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        var searchResourceQuery by remember { mutableStateOf("") }
                        
                        OutlinedTextField(
                            value = searchResourceQuery,
                            onValueChange = { searchResourceQuery = it },
                            placeholder = { Text("Search materials by title or category...") },
                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                            trailingIcon = {
                                if (searchResourceQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchResourceQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear space")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("resource_search_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )

                        val filteredResources = remember(courseResources, searchResourceQuery) {
                            if (searchResourceQuery.isBlank()) {
                                courseResources
                            } else {
                                courseResources.filter {
                                    it.title.contains(searchResourceQuery, ignoreCase = true) ||
                                    it.type.contains(searchResourceQuery, ignoreCase = true)
                                }
                            }
                        }

                        if (filteredResources.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No course materials matching \"$searchResourceQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        } else {
                            // Group resources by category
                            val grouped = filteredResources.groupBy { it.type }
                            
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                            grouped.forEach { (category, items) ->
                                item {
                                    Text(
                                        text = category.uppercase(),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                
                                items(items) { resource ->
                                    val progress = activeDownloads[resource.id] ?: 0
                                    val isDownloading = activeDownloads.containsKey(resource.id)
                                    
                                    ResourceListRow(
                                        resource = resource,
                                        downloadProgress = progress,
                                        isDownloading = isDownloading,
                                        isDeviceOffline = isOfflineMode,
                                        onFavoriteToggle = { viewModel.toggleFavorite(resource) },
                                        onDownloadClick = {
                                            if (isOfflineMode) {
                                                offlineErrorResourceTitle = resource.title
                                                showOfflineErrorDialog = true
                                            } else {
                                                viewModel.downloadResource(resource)
                                            }
                                        },
                                        onOpenClick = { onResourceClick(resource) }
                                    )
                                }
                            }
                            
                            item {
                                Spacer(modifier = Modifier.height(30.dp))
                            }
                        }
                    }
                }
            }
        }

    // Modern High Contrast Warn PopUp when attempting online interactions under severance simulation (Offline Lock)
    if (showOfflineErrorDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineErrorDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "Alert Error Indicator",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Resource Unavailable",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "You are currently testing 'Simulated Offline Mode'. To download \"$offlineErrorResourceTitle\" to your vault, toggle 'Online Mode' in the top bar first."
                )
            },
            confirmButton = {
                Button(
                    onClick = { showOfflineErrorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Understood")
                }
            }
        )
    }

    if (showAddCourseDialog) {
        var courseName by remember { mutableStateOf("") }
        var courseCode by remember { mutableStateOf("") }
        var isNameError by remember { mutableStateOf(false) }
        var isCodeError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddCourseDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Add Course Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Add Course Syllabus",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val semName = when {
                        selectedDept?.id == "FRESH-NS" -> {
                            when (selectedSemester) {
                                1 -> "1st Semester"
                                2 -> "pre-Engineering"
                                3 -> "Other natural sciences"
                                else -> "Semester $selectedSemester"
                            }
                        }
                        else -> "Semester $selectedSemester"
                    }
                    val dept = selectedDept
                    val labelExplanation = if (dept?.college == "Freshman Program") {
                        "Adding a course to ${dept.name} under Freshman year ($semName)."
                    } else {
                        "Adding a course to ${dept?.name ?: "department"} under Year $selectedYear, $semName."
                    }
                    Text(
                        text = labelExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    OutlinedTextField(
                        value = courseName,
                        onValueChange = {
                            courseName = it
                            isNameError = it.isBlank()
                        },
                        label = { Text("Course Title") },
                        placeholder = { Text("e.g. Advanced Operating Systems") },
                        singleLine = true,
                        isError = isNameError,
                        modifier = Modifier.fillMaxWidth().testTag("add_course_name_input"),
                        supportingText = {
                            if (isNameError) {
                                Text("Course title is required", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )

                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = {
                            courseCode = it
                            isCodeError = it.isBlank()
                        },
                        label = { Text("Course Code / Abbreviation") },
                        placeholder = { Text("e.g. CoSc 4112") },
                        singleLine = true,
                        isError = isCodeError,
                        modifier = Modifier.fillMaxWidth().testTag("add_course_code_input"),
                        supportingText = {
                            if (isCodeError) {
                                Text("Course code is required", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isNameError = courseName.isBlank()
                        isCodeError = courseCode.isBlank()
                        if (!isNameError && !isCodeError) {
                            val activeDeptId = selectedDept?.id ?: ""
                            val newCourse = Course(
                                id = "c_${activeDeptId}_${System.currentTimeMillis()}",
                                name = courseName.trim(),
                                code = courseCode.trim().uppercase(),
                                departmentId = activeDeptId,
                                year = selectedYear,
                                semester = selectedSemester
                            )
                            viewModel.insertCustomCourse(newCourse)
                            showAddCourseDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_course_btn")
                ) {
                    Text("Add Course")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddCourseDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddMaterialDialog) {
        val types = listOf(
            "Lecture Notes",
            "Handouts",
            "Assignments",
            "Lab Reports",
            "Past Exams",
            "Project Examples"
        )

        AlertDialog(
            onDismissRequest = { showAddMaterialDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = "Upload Material Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Contribute Course Material",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security Status indicator",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Admin approval is required before this file is made visible to other students.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // --- Manual Material Selector Area ---
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (attachedMaterials.isEmpty()) {
                            Text(
                                text = "FILE ATTACHMENTS (DOCUMENTS/SLIDES)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Button(
                                onClick = { 
                                    filePickerLauncher.launch(
                                        arrayOf(
                                            "application/pdf", 
                                            "application/vnd.ms-powerpoint", 
                                            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                                        )
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth().testTag("pick_file_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile, 
                                    contentDescription = "Attach File Logo",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Attach PDFs or PPT/PPTX documents")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ATTACHED DOCUMENTS (${attachedMaterials.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                TextButton(
                                    onClick = {
                                        filePickerLauncher.launch(
                                            arrayOf(
                                                "application/pdf", 
                                                "application/vnd.ms-powerpoint", 
                                                "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                                            )
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add files", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add More", fontSize = 12.sp)
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val materialIndices = attachedMaterials.indices
                                for (index in materialIndices) {
                                    val material = attachedMaterials[index]
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                val isPdf = material.name.endsWith(".pdf", ignoreCase = true)
                                                val fileIcon = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.PresentToAll
                                                val fileIconColor = if (isPdf) Color(0xFFD32F2F) else Color(0xFFE65100)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = fileIcon,
                                                        contentDescription = "File Type icon",
                                                        tint = fileIconColor,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = material.name,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${material.size} • Ready Offline",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        attachedMaterials = attachedMaterials.toMutableList().apply { removeAt(index) }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Remove document",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            OutlinedTextField(
                                                value = material.title,
                                                onValueChange = { newTitle ->
                                                    attachedMaterials = attachedMaterials.mapIndexed { idx, m ->
                                                        if (idx == index) m.copy(title = newTitle) else m
                                                    }
                                                },
                                                label = { Text("Study Resource Title", fontSize = 11.sp) },
                                                placeholder = { Text("e.g. Lecture Notes Chapter ${index + 1}", fontSize = 11.sp) },
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (uploadListErrorMsg != null) {
                            Text(
                                text = uploadListErrorMsg!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = contributorName,
                        onValueChange = { contributorName = it },
                        label = { Text("Your Name / Contributor") },
                        placeholder = { Text("e.g. Abri (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_material_contributor_input")
                    )

                    // Material Type flow row of selectable chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "COMMON MATERIAL TYPE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        val chunkedTypes = types.chunked(2)
                        for (chunk in chunkedTypes) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (type in chunk) {
                                    FilterChip(
                                        selected = selectedCommonType == type,
                                        onClick = { selectedCommonType = type },
                                        label = { Text(type, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.weight(1f).testTag("dialog_type_chip_$type")
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            isDescError = it.isBlank()
                        },
                        label = { Text("Short Description & Details") },
                        placeholder = { Text("Summarize topics, questions covered, etc.") },
                        maxLines = 3,
                        isError = isDescError,
                        modifier = Modifier.fillMaxWidth().testTag("add_material_desc_input"),
                        supportingText = {
                            if (isDescError) {
                                Text("Description is required", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDescError = description.isBlank()
                        val hasNoFiles = attachedMaterials.isEmpty()
                        val hasNoTitles = attachedMaterials.any { it.title.isBlank() }

                        if (hasNoFiles) {
                            uploadListErrorMsg = "Please attach at least one PDF or PPT/PPTX document."
                        } else if (hasNoTitles) {
                            uploadListErrorMsg = "Each attached material requires a Study Title."
                        } else if (isDescError) {
                            uploadListErrorMsg = "Description is required."
                        } else {
                            val activeCourseId = selectedCourse?.id ?: ""
                            for (material in attachedMaterials) {
                                val isPdfFile = material.name.endsWith(".pdf", ignoreCase = true)
                                val newResource = Resource(
                                    id = "res_${activeCourseId}_${System.currentTimeMillis()}_${(1000..9999).random()}",
                                    courseId = activeCourseId,
                                    title = material.title.trim(),
                                    type = selectedCommonType,
                                    fileSize = material.size,
                                    description = description.trim(),
                                    pageCount = if (isPdfFile) 10 else (8..25).random(),
                                    localPath = material.localPath,
                                    isDownloaded = true, 
                                    isFavorite = false,
                                    lastReadPage = 0,
                                    isPendingApproval = true,
                                    contributorName = if (contributorName.isBlank()) "Anonymous" else contributorName.trim()
                                )
                                viewModel.insertCustomResource(newResource)
                            }
                            showAddMaterialDialog = false
                            showSuccessUploadDialog = true
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_material_btn")
                ) {
                    Text("Upload for Review")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddMaterialDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSuccessUploadDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessUploadDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Upload Successful!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Your contributed material was successfully uploaded to the database in a 'Pending Approval' state.\n\nTo review and approve it so it gets posted for other students, toggle 'Admin Mode' in the Explore Top Bar (Explore Tab) and look at the Admin Review Desk!"
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessUploadDialog = false }
                ) {
                    Text("OK, Got It!")
                }
            }
        )
    }
}

@Composable
fun ResourceListRow(
    resource: Resource,
    downloadProgress: Int,
    isDownloading: Boolean,
    isDeviceOffline: Boolean,
    onFavoriteToggle: () -> Unit,
    onDownloadClick: () -> Unit,
    onOpenClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("resource_item_${resource.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Action visual category icon
                Icon(
                    imageVector = when(resource.type) {
                        "Lecture Notes" -> Icons.Default.Description
                        "Handouts" -> Icons.Default.Note
                        "Assignments" -> Icons.Default.Assignment
                        "Lab Reports" -> Icons.Default.Science
                        "Past Exams" -> Icons.Default.Quiz
                        "Project Examples" -> Icons.Default.Palette
                        else -> Icons.Default.Article
                    },
                    contentDescription = resource.type,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resource.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "File size: ${resource.fileSize} • ${resource.pageCount} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Favorite Toggle button
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (resource.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Toggle favorite status",
                        tint = if (resource.isFavorite) Color(0xFFFBBF24) else MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = resource.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Bottom download / progress indicator bar / Action reader trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Download and Cache Status badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (resource.isDownloaded) Icons.Default.OfflinePin else Icons.Default.CloudQueue,
                        contentDescription = "Cache status indicator",
                        tint = if (resource.isDownloaded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (resource.isDownloaded) "Saved Offline" else "Cloud (Online)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (resource.isDownloaded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Action Buttons triggers
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (isDownloading) {
                        // High fidelity circular download indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = (downloadProgress.toFloat() / 100f).coerceIn(0f, 1f),
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$downloadProgress%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (resource.isDownloaded) {
                        // Launch reader in-app
                        Button(
                            onClick = onOpenClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ChromeReaderMode,
                                    contentDescription = "Open reader",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Read File", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    } else {
                        // Prompt download (Cloud file)
                        Button(
                            onClick = onDownloadClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download resource key",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}
