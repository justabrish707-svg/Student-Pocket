package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Course
import com.example.data.model.Resource
import com.example.ui.viewmodel.PocketViewModel

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
    val selectedCourse by viewModel.selectedCourse.collectAsState()
    val courseResources by viewModel.courseResources.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val isOfflineMode by viewModel.isInSimulationOfflineMode.collectAsState()

    var showOfflineErrorDialog by remember { mutableStateOf(false) }
    var offlineErrorResourceTitle by remember { mutableStateOf("") }

    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showAddMaterialDialog by remember { mutableStateOf(false) }

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
                    Text(
                        text = "Courses Syllabus",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    
                    if (coursesList.isEmpty()) {
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
                                    text = "No course handouts loaded for this semester.",
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
                            items(coursesList) { course ->
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
                                    Row(
                                        modifier = Modifier.padding(16.dp),
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
                        // Group resources by category
                        val grouped = courseResources.groupBy { it.type }
                        
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
        var title by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf("Lecture Notes") }
        var description by remember { mutableStateOf("") }
        var isTitleError by remember { mutableStateOf(false) }
        var isDescError by remember { mutableStateOf(false) }

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
                    Text(
                        text = "Adding custom study material under course \"${selectedCourse?.name ?: ""}\". Everything is kept fully offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            isTitleError = it.isBlank()
                        },
                        label = { Text("Resource Title / File Name") },
                        placeholder = { Text("e.g. 2024 Lab Exercises Complete") },
                        singleLine = true,
                        isError = isTitleError,
                        modifier = Modifier.fillMaxWidth().testTag("add_material_title_input"),
                        supportingText = {
                            if (isTitleError) {
                                Text("Title is required", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )

                    // Material Type flow row of selectable chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "MATERIAL TYPE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        types.chunked(2).forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                chunk.forEach { type ->
                                    FilterChip(
                                        selected = selectedType == type,
                                        onClick = { selectedType = type },
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
                        isTitleError = title.isBlank()
                        isDescError = description.isBlank()
                        if (!isTitleError && !isDescError) {
                            val activeCourseId = selectedCourse?.id ?: ""
                            val sizeInMB = String.format("%.1f", (1.0 + (Math.random() * 4.0))) + " MB"
                            val newResource = Resource(
                                id = "res_${activeCourseId}_${System.currentTimeMillis()}",
                                courseId = activeCourseId,
                                title = title.trim(),
                                type = selectedType,
                                fileSize = sizeInMB,
                                description = description.trim(),
                                pageCount = (8..25).random(),
                                isDownloaded = true, // instantly marked offline as contributed by user locally
                                isFavorite = false,
                                lastReadPage = 0
                            )
                            viewModel.insertCustomResource(newResource)
                            showAddMaterialDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_material_btn")
                ) {
                    Text("Add Material")
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
