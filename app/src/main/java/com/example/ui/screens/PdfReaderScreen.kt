package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
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
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import com.example.data.model.Bookmark
import com.example.data.model.Resource
import com.example.ui.viewmodel.PocketViewModel
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

val pdfRenderMutex = Mutex()

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

    val isLocalPdf = remember(resource.id) {
        resource.localPath != null && resource.localPath.endsWith(".pdf", ignoreCase = true)
    }
    val isLocalPptx = remember(resource.id) {
        resource.localPath != null && (resource.localPath.endsWith(".pptx", ignoreCase = true) || resource.localPath.endsWith(".ppt", ignoreCase = true))
    }

    var pdfBitmap by remember(resource.id) { mutableStateOf<Bitmap?>(null) }
    var pdfPageCount by remember(resource.id) { mutableStateOf<Int?>(null) }
    var pptxSlides by remember(resource.id) { mutableStateOf<List<String>>(emptyList()) }

    // Extract PPTX slides if local pptx
    LaunchedEffect(resource.id, resource.localPath) {
        if (isLocalPptx && resource.localPath != null) {
            pptxSlides = extractPptxText(resource.localPath)
        }
    }

    var currentPage by remember(resource.id) { mutableStateOf(resource.lastReadPage) }
    var isContinuousScroll by remember(resource.id) { mutableStateOf(false) }

    // Render active PDF page bitmap
    LaunchedEffect(resource.id, resource.localPath, currentPage) {
        if (isLocalPdf && resource.localPath != null) {
            withContext(Dispatchers.IO) {
                pdfRenderMutex.withLock {
                    try {
                        val file = File(resource.localPath)
                        if (file.exists() && file.length() > 0) {
                            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                            val renderer = PdfRenderer(pfd)
                            pdfPageCount = renderer.pageCount
                            val pIndex = currentPage.coerceIn(0, renderer.pageCount - 1)
                            val page = renderer.openPage(pIndex)
                            
                            val displayWidth = 1080
                            val scaleFactor = displayWidth.toFloat() / page.width.toFloat()
                            val renderHeight = (page.height * scaleFactor).toInt().coerceAtLeast(1)
                            
                            val bitmap = Bitmap.createBitmap(displayWidth, renderHeight, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            pdfBitmap = bitmap
                            page.close()
                            renderer.close()
                            pfd.close()
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    val totalPageCount = when {
        isLocalPdf -> pdfPageCount ?: resource.pageCount
        isLocalPptx -> if (pptxSlides.isNotEmpty()) pptxSlides.size else resource.pageCount
        else -> resource.pageCount
    }

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
                            text = "Page ${currentPage + 1} of $totalPageCount",
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
                    // Toggle reading layout method
                    IconButton(
                        onClick = { isContinuousScroll = !isContinuousScroll },
                        modifier = Modifier.testTag("toggle_reading_mode")
                    ) {
                        Icon(
                            imageVector = if (isContinuousScroll) Icons.Default.MenuBook else Icons.Default.SwapVert,
                            contentDescription = if (isContinuousScroll) "Switch to Page View" else "Switch to Continuous Scroll",
                            tint = if (isContinuousScroll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Open visual PowerPoint or PDF in system app
                    if (resource.localPath != null) {
                        IconButton(
                            onClick = { openFileExternally(context, resource.localPath) },
                            modifier = Modifier.testTag("open_external_app_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "View original file as uploaded",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

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
                    val safeCurrentPage = currentPage.coerceIn(0, maxOf(0, totalPageCount - 1))
                    val maxRangeValue = maxOf(1f, (totalPageCount - 1).toFloat())
                    val safeSteps = maxOf(0, totalPageCount - 2)

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
                            text = "Page ${safeCurrentPage + 1} of $totalPageCount",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "Next",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (safeCurrentPage < totalPageCount - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.clickable(enabled = safeCurrentPage < totalPageCount - 1) { currentPage++ }
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
                    if (isContinuousScroll) {
                        val lazyListState = rememberLazyListState()

                        // Keep current page synced with user's scroll location
                        LaunchedEffect(lazyListState.firstVisibleItemIndex) {
                            if (lazyListState.isScrollInProgress) {
                                currentPage = lazyListState.firstVisibleItemIndex
                            }
                        }

                        // Scroll list container when currentPage updates (e.g. bookmarks or slider scrubs)
                        LaunchedEffect(currentPage) {
                            if (!lazyListState.isScrollInProgress) {
                                lazyListState.animateScrollToItem(currentPage)
                            }
                        }

                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = zoomScale,
                                    scaleY = zoomScale
                                )
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, _, zoom, _ ->
                                        zoomScale = (zoomScale * zoom).coerceIn(1.0f, 2.5f)
                                    }
                                }
                                .testTag("scrolling_reader_container"),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            items(totalPageCount) { pageIndex ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    if (isLocalPdf && resource.localPath != null) {
                                        PdfPageItem(
                                            filePath = resource.localPath,
                                            pageIndex = pageIndex
                                        )
                                    } else if (isLocalPptx) {
                                        PptxSlideItem(
                                            resource = resource,
                                            currentPage = pageIndex,
                                            pptxSlides = pptxSlides
                                        )
                                    } else {
                                        SimulatedPageItem(
                                            resource = resource,
                                            currentPage = pageIndex
                                        )
                                    }
                                }
                            }
                        }
                    } else {
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
                            if (isLocalPdf && pdfBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = pdfBitmap!!.asImageBitmap(),
                                        contentDescription = "PDF Page View idx $currentPage",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else if (isLocalPptx) {
                                Column(
                                    modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "PPTX PRESENTATION SLIDE",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFE65100)
                                        )
                                        Text(
                                            text = "Ref: #${resource.id}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = "Slide ${currentPage + 1}: ${if (pptxSlides.isNotEmpty()) "Extracted Slide Notes" else "Presentation Review"}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = Color(0xFFE65100)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        val slideText = if (pptxSlides.isNotEmpty() && currentPage < pptxSlides.size) {
                                            pptxSlides[currentPage]
                                        } else {
                                            "Welcome to the computer science 2nd-year lecture slide: ${resource.title}. \n\nNo embedded text was found on this slide. Use standard bookmarks below to add study reflections and write notes directly on it."
                                        }

                                        Text(
                                            text = slideText,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Serif,
                                                lineHeight = 22.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        if (pptxSlides.isEmpty()) {
                                            Spacer(modifier = Modifier.height(20.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFFFF3E0))
                                                    .padding(12.dp)
                                            ) {
                                                Text(
                                                    text = "STUDY FOCUS:\n• Re-read this PowerPoint slide summary\n• Create lecture highlights with bookmarks\n• View offline whenever studying for midterms!",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFFE65100)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "STUDENT POCKET CACHE DECK • SLIDE ${currentPage + 1} • COPYRIGHT AUTHORIZED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            textAlign = TextAlign.Center,
                                            fontSize = 8.sp,
                                            letterSpacing = 1.sp
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            } else {
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
        "CS-Y2S2-04" -> when (resource.id) {
            "RES_MATH2052_01" -> when (page) {
                0 -> "Group Assignment Cover Page"
                1 -> "Part A: Planar Graphs - Q1"
                2 -> "Part A: Planar Graphs - Q2 & Q3"
                3 -> "Part B: Graph Coloring - C5 and W5"
                4 -> "Part B: Graph Coloring - Welsh-Powell"
                5 -> "Part C: Eulerian Circuit Conditions"
                6 -> "Part C: Hamiltonian but NOT Eulerian Graph"
                7 -> "Part C: Dirac's Theorem & C++ Adjacency Code"
                8 -> "Part C: C++ Adjacency Code (Continued)"
                else -> "Part C: Eulerian Adjacency Verification"
            }
            else -> when(id) {
                0 -> "Planar Graphs Properties & Euler bounds"
                1 -> "Bipartite Graph Chromatic Numbers"
                2 -> "Welsh-Powell Greedy Local Coloring"
                3 -> "Eulerian Trails and Fleury's Algorithm"
                else -> "Hamiltonian Cycles and Dirac's Bounds"
            }
        }
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
        "CS-Y2S2-04" -> when (resource.id) {
            "RES_MATH2052_01" -> when (page) {
                0 -> """
                    ARBA MINCH UNIVERSITY
                    COLLEGE OF NATURAL SCIENCES
                    DEPARTMENT OF MATHEMATICS
                    
                    Discrete Mathematics Group Assignment
                    Section: A
                    
                    Group Members:
                    1. Abrham Sisay ----------------- NSR/1931/17
                    2. Abraham Admasu ------------ NSR/056/17
                    3. Fikreab Negasi ---------------- NSR/711/17
                    4. Rekik Abebaw ---------------- NSR/1416/17
                    5. Yeabsira Elias ---------------- NSR/2041/17
                    6. Biruk Melese ----------------- NSR/412/17
                    7. Biruk Niguse ------------------ NSR/1962/17
                    8. Sisay Abreham --------------- NSR/2072/17
                    9. Hanan Hassen ---------------- NSR/1997/17
                    10. Bereket Berhanu ------------ NSR/300/17
                """.trimIndent()
                1 -> """
                    Part A: Planar Graphs
                    1. A connected planar simple graph has 20 vertices, each of degree 3. How many regions does the graph divide the plane into? (Use Euler’s Formula: r = e - v + 2).
                    
                    Step 1: Use the Handshaking Lemma
                    The Handshaking Lemma states: Sum of degrees = 2e
                    Each of the 20 vertices has degree 3.
                    Therefore: 20 * 3 = 2e => 60 = 2e => e = 30 edges.
                    
                    Step 2: Use Euler's Formula
                    r = e - v + 2
                    Substitute the values: r = 30 - 20 + 2 = 12.
                    
                    Final Answer: The graph divides the plane into 12 regions.
                """.trimIndent()
                2 -> """
                    2. Prove that the complete graph K5 is non-planar by showing it violates the edge-vertex inequality for planar graphs.
                    For a simple connected planar graph: e <= 3v - 6
                    For the complete graph K5:
                    • Number of vertices v = 5
                    • Number of edges e = 10
                    Now calculate: 3v - 6 = 3(5) - 6 = 9
                    Compare: e = 10 and 10 > 9.
                    Since the number of edges is greater than the maximum allowed for a planar graph, K5 violates the planar graph inequality.
                    Final Answer: K5 is non-planar because it violates e <= 3v - 6.
                    
                    3. In a connected planar graph with 10 edges and 6 regions, determine the number of vertices.
                    Use Euler's Formula: v - e + r = 2
                    Given: e = 10, r = 6
                    Substitute: v - 10 + 6 = 2 => v - 4 = 2 => v = 6.
                    Final Answer: The graph has 6 vertices.
                """.trimIndent()
                3 -> """
                    Part B: Graph Coloring
                    1. Find the chromatic number χ(G) for a Cycle graph C5 and a Wheel graph W5. Explain the difference.
                    
                    • Cycle Graph C5:
                    A cycle graph with an odd number of vertices requires 3 colors.
                    Therefore: χ(C5) = 3
                    
                    • Wheel Graph W5:
                    A wheel graph consists of one cycle graph and one center vertex connected to all outer vertices.
                    The outer cycle C5 already needs 3 colors. The center vertex is connected to every outer vertex, so it requires a new color.
                    Therefore: χ(W5) = 4
                    
                    Difference: The wheel graph requires one additional color because the center vertex is adjacent to all other vertices.
                """.trimIndent()
                4 -> """
                    2. Define a Bipartite graph in terms of its chromatic number. Why is χ(G) = 2 for all bipartite graphs?
                    A bipartite graph is a graph whose vertices can be divided into two disjoint sets such that no adjacent vertices belong to the same set.
                    In terms of chromatic number: χ(G) = 2.
                    Reason: Vertices in set 1 are colored with Color 1, set 2 with Color 2. Adjacent vertices are always in different sets.
                    
                    3. Use the Welsh-Powell algorithm logic to manually color a graph with vertices {A, B, C, D, E} and edges {AB, BC, CD, DE, EA, AC}. Minimum colors?
                    • Step 1: Find degrees: deg(A)=3, deg(C)=3, deg(B)=2, deg(D)=2, deg(E)=2.
                    • Step 2: Sort vertices decreasing: A, C, B, D, E.
                    • Step 3: Assign Color 1 -> A, D (D is not adjacent to A).
                    • Step 4: Assign Color 2 -> C, E (E is not adjacent to C).
                    • Step 5: Assign Color 3 -> B (adjacent to both A and C).
                    Final Answer: Minimum colors = 3.
                """.trimIndent()
                5 -> """
                    Part C: Eulerian and Hamiltonian Graphs
                    1. A graph has degrees {2, 2, 4, 4, 2}. Does it contain an Euler Circuit? Justify.
                    A connected graph contains an Euler Circuit if every vertex has an even degree.
                    Given degrees: 2, 2, 4, 4, 2.
                    All vertices have even degree. Therefore, yes, the graph satisfies the Euler Circuit condition.
                    
                    2. Draw a graph that is Hamiltonian but NOT Eulerian. Explain why it fails Eulerian.
                    Consider a square with one diagonal:
                    • Vertices: A, B, C, D.
                    • Edges: AB, BC, CD, DA, AC.
                    Hamiltonian Property:
                    The cycle A -> B -> C -> D -> A visits every vertex exactly once and returns to the starting point. Thus, it is Hamiltonian.
                """.trimIndent()
                6 -> """
                    Eulerian Condition verification for Square with AC Diagonal (A, B, C, D):
                    Vertex degrees:
                    deg(A) = 3
                    deg(B) = 2
                    deg(C) = 3
                    deg(D) = 2
                    
                    Vertices A and C have odd degree.
                    A graph is Eulerian only if all vertices have even degree.
                    Final Answer: The graph is Hamiltonian but not Eulerian because not all vertices have even degree (specifically, A and C have odd degree of 3).
                """.trimIndent()
                7 -> """
                    3. Explain Dirac’s Theorem condition for a graph to be Hamiltonian. If a graph has 5 vertices, what is the minimum degree each vertex must have to guarantee a Hamilton Cycle?
                    
                    Dirac's Theorem: If a simple graph has n vertices (where n >= 3) and every vertex has degree at least n/2, then the graph is Hamiltonian.
                    Given: n = 5.
                    Compute: n / 2 = 5 / 2 = 2.5.
                    Since degree must be an integer, Minimum degree >= 3.
                    Final Answer: Each vertex must have degree at least 3 to guarantee a Hamilton Cycle.
                    
                    4. Programming Task: Write a C++ program to represent a graph using an Adjacency Matrix and determine if the graph is Eulerian.
                    
                    C++ Code Snippet:
                    #include <iostream>
                    using namespace std;
                    
                    int main() {
                        int n, e;
                        int graph[10][10] = {0};
                        int u, v;
                """.trimIndent()
                8 -> """
                        // Input number of vertices and edges
                        cout << "Enter number of vertices: "; cin >> n;
                        cout << "Enter number of edges: "; cin >> e;
                        
                        // Input edges
                        cout << "Enter edges (example: 0 1):" << endl;
                        for(int i = 0; i < e; i++) {
                            cout << "Edge " << i + 1 << ": ";
                            cin >> u >> v;
                            graph[u][v] = 1;
                            graph[v][u] = 1; // Undirected graph
                        }
                        
                        // Display adjacency matrix
                        cout << "\nAdjacency Matrix:" << endl;
                        for(int i = 0; i < n; i++) {
                            for(int j = 0; j < n; j++) {
                                cout << graph[i][j] << " ";
                            }
                            cout << endl;
                        }
                """.trimIndent()
                9 -> """
                        // Calculate degree of each vertex and verify Eulerian circuit
                        int oddCount = 0;
                        for(int i = 0; i < n; i++) {
                            int degree = 0;
                            for(int j = 0; j < n; j++) {
                                degree += graph[i][j];
                            }
                            cout << "Vertex " << i << " degree = " << degree << endl;
                            if(degree % 2 != 0) {
                                oddCount++;
                            }
                        }
                        
                        // Determine Eulerian type
                        if(oddCount == 0) {
                            cout << "The graph has an Euler Circuit." << endl;
                        } else if(oddCount == 2) {
                            cout << "The graph has an Euler Path." << endl;
                        } else {
                            cout << "The graph is NOT Eulerian." << endl;
                        }
                        return 0;
                    }
                """.trimIndent()
                else -> "The program checks whether the graph is Eulerian using the adjacency matrix degree checks. Success!"
            }
            else -> when (page % 4) {
                0 -> "Planar simple graph region calculations are performed under Euler's mapping bounds of the Euclidean plane."
                1 -> "Greedy coloring utilizing Welsh-Powell coordinates the vertex allocations by degree size bounds."
                2 -> "Bipartite structures partition elements seamlessly into two discrete sets with zero adjacent cross interactions."
                else -> "Euler circuits require a single contiguous cycle where all vertex elements maintain even edge degree metrics."
            }
        }
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
        "CS-Y2S2-04" -> when (id) {
            0 -> "r = e - v + 2  [Euler's Formula]"
            1 -> "e <= 3v - 6  [Planar Edge-Vertex Bound]"
            else -> "deg(v) >= n / 2  [Dirac's Hamilton Condition]"
        }
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

private fun extractPptxText(filePath: String): List<String> {
    val slides = mutableListOf<String>()
    try {
        val file = File(filePath)
        if (file.exists()) {
            val zipFile = ZipFile(file)
            val entries = zipFile.entries()
            val slideEntries = mutableListOf<ZipEntry>()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.startsWith("ppt/slides/slide") && entry.name.endsWith(".xml")) {
                    slideEntries.add(entry)
                }
            }
            // Sort slide entries numerically
            slideEntries.sortBy { entry ->
                val numStr = entry.name.substringAfter("ppt/slides/slide").substringBefore(".xml")
                numStr.toIntOrNull() ?: 999
            }
            
            for (entry in slideEntries) {
                val xmlContent = zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
                // Match text inside <a:t> elements
                val regex = Regex("<a:t>(.*?)</a:t>")
                val matches = regex.findAll(xmlContent).map { it.groupValues[1] }.toList()
                if (matches.isNotEmpty()) {
                    val parsedText = matches.joinToString(" ")
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&quot;", "\"")
                        .replace("&apos;", "'")
                        .trim()
                    slides.add(parsedText)
                }
            }
        }
    } catch (e: Throwable) {
        e.printStackTrace()
    }
    return slides
}

fun openFileExternally(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        if (file.exists()) {
            val authority = "com.aistudio.studentpocket.rxbywq.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val extension = file.extension.lowercase()
            val mimeType = when (extension) {
                "pdf" -> "application/pdf"
                "ppt" -> "application/vnd.ms-powerpoint"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                else -> "*/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Local file not found.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Throwable) {
        Toast.makeText(context, "No supportive viewer found. Please download a PDF/PPTX reader from Play Store to view native files.", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}

@Composable
fun PdfPageItem(
    filePath: String,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    var pageBitmap by remember(filePath, pageIndex) { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(filePath, pageIndex) {
        withContext(Dispatchers.IO) {
            pdfRenderMutex.withLock {
                try {
                    val file = File(filePath)
                    if (file.exists() && file.length() > 0) {
                        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(pfd)
                        if (pageIndex < renderer.pageCount) {
                            val page = renderer.openPage(pageIndex)
                            val displayWidth = 1080
                            val scaleFactor = displayWidth.toFloat() / page.width.toFloat()
                            val renderHeight = (page.height * scaleFactor).toInt().coerceAtLeast(1)
                            
                            val bitmap = Bitmap.createBitmap(displayWidth, renderHeight, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            pageBitmap = bitmap
                            page.close()
                        }
                        renderer.close()
                        pfd.close()
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 700.dp)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (pageBitmap != null) {
            Image(
                bitmap = pageBitmap!!.asImageBitmap(),
                contentDescription = "PDF Page $pageIndex",
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@Composable
fun PptxSlideItem(
    resource: Resource,
    currentPage: Int,
    pptxSlides: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PPTX PRESENTATION SLIDE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFE65100)
            )
            Text(
                text = "Ref: #${resource.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "Slide ${currentPage + 1}: ${if (pptxSlides.isNotEmpty()) "Extracted Slide Notes" else "Presentation Review"}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = Color(0xFFE65100)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val slideText = if (pptxSlides.isNotEmpty() && currentPage < pptxSlides.size) {
                pptxSlides[currentPage]
            } else {
                "Welcome to the computer science 2nd-year lecture slide: ${resource.title}. \n\nNo embedded text was found on this slide. Use standard bookmarks below to add study reflections and write notes directly on it."
            }

            Text(
                text = slideText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Serif,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )

            if (pptxSlides.isEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "STUDY FOCUS:\n• Re-read this PowerPoint slide summary\n• Create lecture highlights with bookmarks\n• View offline whenever studying for midterms!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFE65100)
                    )
                }
            }
        }

        Text(
            text = "STUDENT POCKET CACHE DECK • SLIDE ${currentPage + 1} • COPYRIGHT AUTHORIZED",
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

@Composable
fun SimulatedPageItem(
    resource: Resource,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
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

