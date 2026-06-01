package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bookmark
import com.example.data.model.Resource
import com.example.ui.viewmodel.PocketViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    viewModel: PocketViewModel,
    onBack: () -> Unit
) {
    val activeRes by viewModel.activeResource.collectAsState()
    val bookmarks by viewModel.activeResourceBookmarks.collectAsState()
    val context = LocalContext.current

    // Retain last non-null resource during exit animations to prevent crashing
    val safeResource = remember { mutableStateOf(activeRes) }
    if (activeRes != null) {
        safeResource.value = activeRes
    }
    val resource = safeResource.value ?: Resource(
        id = "dummy", courseId = "", title = "", type = "Lecture Notes", fileSize = "", description = ""
    )

    var currentPage by remember(resource.id) { mutableStateOf(resource.lastReadPage) }
    var zoomScale by remember(resource.id) { mutableStateOf(1.0f) }
    var showAddBookmarkSheet by remember(resource.id) { mutableStateOf(false) }
    var showBookmarksDrawer by remember(resource.id) { mutableStateOf(false) }
    var bookmarkNote by remember(resource.id) { mutableStateOf("") }

    // Display welcome resume banner once on open
    var showResumeToast by remember(resource.id) { mutableStateOf(resource.lastReadPage > 0) }
    LaunchedEffect(resource.id) {
        if (showResumeToast) {
            Toast.makeText(context, "Resumed reading from Page ${resource.lastReadPage + 1}", Toast.LENGTH_SHORT).show()
            showResumeToast = false
        }
    }

    // Capture progress updates whenever we scrub the slide page
    LaunchedEffect(currentPage) {
        kotlinx.coroutines.delay(300)
        viewModel.updateReadingPage(currentPage)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = resource.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Page ${currentPage + 1} of ${resource.pageCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit reader"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Zoom adjustment controls
                    IconButton(onClick = { zoomScale = (zoomScale + 0.25f).coerceIn(1.0f, 2.5f) }) {
                        Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Zoom In")
                    }
                    if (zoomScale > 1.0f) {
                        IconButton(onClick = { zoomScale = 1.0f }) {
                            Icon(imageVector = Icons.Default.ZoomOutMap, contentDescription = "Reset Zoom")
                        }
                    }
                    
                    // Bookmark trigger
                    IconButton(onClick = { showAddBookmarkSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Save Page Bookmark",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Bookmarks manager drawer trigger
                    IconButton(onClick = { showBookmarksDrawer = !showBookmarksDrawer }) {
                        Icon(
                            imageVector = Icons.Default.LinearScale,
                            contentDescription = "Open bookmark drawer",
                            tint = if (showBookmarksDrawer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Elegant page scrub controls
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val safeCurrentPage = currentPage.coerceIn(0, maxOf(0, resource.pageCount - 1))
                    val maxRangeValue = maxOf(1f, (resource.pageCount - 1).toFloat())
                    val safeSteps = maxOf(0, resource.pageCount - 2)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Prev",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (safeCurrentPage > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.clickable(enabled = safeCurrentPage > 0) { currentPage-- }
                        )

                        Text(
                            text = "Page ${safeCurrentPage + 1} of ${resource.pageCount}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "Next",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (safeCurrentPage < resource.pageCount - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.clickable(enabled = safeCurrentPage < resource.pageCount - 1) { currentPage++ }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Slider(
                        value = safeCurrentPage.toFloat(),
                        onValueChange = { currentPage = it.toInt() },
                        valueRange = 0f..maxRangeValue,
                        steps = safeSteps,
                        modifier = Modifier.fillMaxWidth().testTag("pdf_page_slider"),
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Warning note if zoomed
                if (zoomScale > 1.0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Zoom: ${scaleToText(zoomScale)} (Two-finger gesture ready)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Interactive Simulated PDF content sheet
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale
                            )
                            // Support premium pinch gesture
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    zoomScale = (zoomScale * zoom).coerceIn(1.0f, 2.5f)
                                }
                            }
                            .testTag("pdf_canvas_container"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Document Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SECURE LOCAL DECRYPTED",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Doc Ref: #${resource.id}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            
                            // Immersive course-specific text simulator
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Chapter ${categoryToChapterNum(resource.type, currentPage)}: ${pageTitleFor(resource, currentPage)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = academicLoreFor(resource, currentPage),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        lineHeight = 22.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                // Styled dynamic formula/takeaway card for academia look
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = academicFormulaFor(resource, currentPage),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            // Document Footer info
                            Text(
                                text = "STUDENT POCKET CACHE DECK • PAGE ${currentPage + 1} • COPYRIGHT AUTHORIZED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    textAlign = TextAlign.Center,
                                    fontSize = 8.sp,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            // Collapsible Bookmark list drawer (Slide drawer)
            AnimatedVisibility(
                visible = showBookmarksDrawer,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(280.dp)
                    .fillMaxHeight()
            ) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Page Bookmarks",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            IconButton(onClick = { showBookmarksDrawer = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Drawer")
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        if (bookmarks.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No bookmarks added.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(bookmarks) { bmk ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                currentPage = bmk.pageNumber
                                                showBookmarksDrawer = false
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Page ${bmk.pageNumber + 1}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                IconButton(
                                                    onClick = { viewModel.removePageBookmark(bmk.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete bookmark item",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            if (bmk.note.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = bmk.note,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog to add a new Page Bookmark with text notes
    if (showAddBookmarkSheet) {
        AlertDialog(
            onDismissRequest = { showAddBookmarkSheet = false },
            title = {
                Text(
                    text = "Bookmark Page ${currentPage + 1}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Add a study note for this page so you can find it later easily:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = bookmarkNote,
                        onValueChange = { bookmarkNote = it },
                        placeholder = { Text("e.g. Midterm exam formula...") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bookmark_note_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addPageBookmark(currentPage, bookmarkNote)
                        bookmarkNote = ""
                        showAddBookmarkSheet = false
                        Toast.makeText(context, "Page ${currentPage + 1} bookmarked", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save Bookmark")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmarkSheet = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Format mapping values helpers
private fun scaleToText(scale: Float): String {
    return when {
        scale <= 1.0f -> "100%"
        scale <= 1.5f -> "150%"
        scale <= 2.0f -> "200%"
        else -> "250%"
    }
}

private fun categoryToChapterNum(category: String, page: Int): String {
    return when(category) {
        "Lecture Notes" -> "${(page / 3) + 1}"
        "Handouts" -> "${(page / 2) + 1}"
        else -> "${(page / 4) + 1}"
    }
}

private fun pageTitleFor(resource: Resource, page: Int): String {
    val id = page % 5
    return when (resource.courseId) {
        "CS101" -> when(id) {
            0 -> "Understanding Computing Turing Bounds"
            1 -> "Boolean Math and Bit Registries"
            2 -> "The Von Neumann Bus Bottleneck"
            3 -> "Silicon Transistor Densities & Lithography"
            else -> "Modern Cloud Clusters & Computing Future"
        }
        "CS201" -> when(id) {
            0 -> "Linked Memory Pointers Representation"
            1 -> "Queue Indexing Ring Operations"
            2 -> "Recursive Binary Tree Formations"
            3 -> "Stack Overflow & Frame Allocation"
            else -> "Big O Optimization of Linear Lists"
        }
        "SE302" -> when(id) {
            0 -> "Compose Recomposition Trigger Pipes"
            1 -> "Remembering State Holders Safely"
            2 -> "Flow Observables Lifecycle Mapping"
            3 -> "Database SQLite Room Synchronization"
            else -> "Modular UI Layer Architectural Partition"
        }
        else -> when(id) {
            0 -> "Skeletal Anatomical Formations"
            1 -> "Microcellular Tissue Alignments"
            2 -> "Primary Clinical Diagnostic Metrics"
            3 -> "Endocrinological Feedback Loops"
            else -> "Pharmacological Absorption Rates"
        }
    }
}

private fun academicLoreFor(resource: Resource, page: Int): String {
    val id = page % 4
    return when (resource.courseId) {
        "CS101" -> when(id) {
            0 -> "The Alan Turing bounds provide the deterministic limits of computational mathematics. A Turing complete system is capable of executing any algorithm, governed by formal transition tables mapping input alphabets to tapes."
            1 -> "Boolean algebra regulates logic gates (AND, OR, NOT) using high/low voltage offsets. In contemporary processors, millions of gates construct Arithmetic Logic Units (ALUs) to operate binary logic grids."
            2 -> "The Von Neumann architecture separates CPU registers from the main random-access memory array. This creates a critical transfer latency bottleneck known as the Bus Blockage, limiting maximum throughput."
            else -> "Silicon microprocessor trends operate on lithographic dimensions. Gate channels measuring sub-3nm face quantum tunneling restrictions, prompting industry migrations to chiplets and quantum parallel frameworks."
        }
        "CS201" -> when(id) {
            0 -> "A singly linked list maps memory segments together using individual address pointer references. Unlink actions require simple references swaps, but search calls scale at O(N) access time bounds."
            1 -> "Ring buffers map queue sequences inside solid pre-allocated array configurations. Index wrapping utilizes modulus indices (e.g. (tail + 1) % size) to guarantee O(1) enqueue and dequeue speeds."
            2 -> "Binary search trees segment lookup domains into left and right branches. On balanced configurations, insertion scales at O(log N). Deletions maintain parent pointers and evaluate child offsets."
            else -> "A stack operates as a strict LIFO (Last In First Out) structure, representing process scope calls. Dynamic memory allocations reside on the Heap array, while execution variables bind to stack indexes."
        }
        "SE302" -> when(id) {
            0 -> "Jetpack Compose operates through a declarative state compilation tree. If a mutable state triggers a value change, target recomposition boundaries intelligently rebuild only the affected branch segments."
            1 -> "The remember keyword binds memory outputs during compile schedules. When dynamic system configuration changes (such as rotation events), rememberSaveable preserves active values using Bundles."
            2 -> "Modern reactive streams use Coroutines and Flow APIs. Collecting flows inside the Compose UI requires using lifecycle-aware standard scopes (collectAsStateWithLifecycle) to avoid memory leak hazards."
            else -> "Offline persistence stores data locally using Room abstractions. When user initiates actions, repositories execute updates in background threads. Reactively, flow emission channels instantly refresh the UI."
        }
        else -> when(id) {
            0 -> "The human musculoskeletal system is organized into the axial division (skull, spine, rib cages) and appendicular frameworks. Structural bones articulate through joints lined with hyaline cartilage."
            1 -> "Pathology investigates changes to human cellular segments. Traumatic inputs or vascular blocks cause localized necrosis or cellular apoptosis, requiring inflammatory and healing cascades."
            2 -> "Clinical diagnostics evaluate cardinal metrics: body temperatures, systolic/diastolic blood pressure, pulse rate beats, and oxygen absorptions. Deviation indicates systemic homeostatic errors."
            else -> "Pharmacokinetics examines absorption, distribution, metabolism, and excretion rates (ADME). Oral tablets face hepatic first-pass reduction in livers before achieving systemic bioavailability."
        }
    }
}

private fun academicFormulaFor(resource: Resource, page: Int): String {
    val id = page % 3
    return when (resource.courseId) {
        "CS101" -> when(id) {
            0 -> "f(x) = ∑ (w_i * x_i) + bias"
            1 -> "L_gate = R_litho * (k1 / NA)"
            else -> "Bus_BW = Clock * Width (bytes/sec)"
        }
        "CS201" -> when(id) {
            0 -> "T(n) = T(n/2) + O(n) => O(n log n)"
            1 -> "Next_Tail = (Current_Tail + 1) % Array_Capacity"
            else -> "BST_Max_Height = O(h_root) [h = O(log k)]"
        }
        "SE302" -> when(id) {
            0 -> "val uiState by viewModel.flow.collectAsStateWithLifecycle()"
            1 -> "@Entity(tableName = \"resources\")"
            else -> "Modifier.windowInsetsPadding(WindowInsets.navigationBars)"
        }
        else -> when(id) {
            0 -> "C_bioavail = (AUC_oral / AUC_iv) * 100%"
            1 -> "BP = CO (Cardiac Output) * SVR (Resistance)"
            else -> "V_distr = Total_Dose / Plasma_Concentration"
        }
    }
}
