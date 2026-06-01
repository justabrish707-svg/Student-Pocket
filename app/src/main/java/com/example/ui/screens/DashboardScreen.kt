package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Department
import com.example.data.model.Resource
import com.example.ui.viewmodel.PocketViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PocketViewModel,
    onDepartmentClick: (Department) -> Unit,
    onResourceClick: (Resource) -> Unit
) {
    val departments by viewModel.departments.collectAsState()
    val offlineResources by viewModel.offlineResources.collectAsState()
    val favoriteResources by viewModel.favoriteResources.collectAsState()
    
    var selectedCollegeFilter by remember { mutableStateOf("All") }
    val colleges = remember(departments) {
        listOf("All") + departments.map { it.college ?: "Other" }.distinct().filter { it != "Other" }.sorted()
    }
    
    val isSimOffline by viewModel.isInSimulationOfflineMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    
    // Derived state: Recent reading stack (where user has turned pages)
    val courseResList by viewModel.courseResources.collectAsState()
    
    // Find resources that have been read (lastReadPage > 0)
    // We can fetch from database models, let's look for resources that have downloading status or lastReadPage
    // Since we seed the DB with basic slides, we can monitor all items in state or find them in offline items
    val recentReadingList = offlineResources.filter { it.lastReadPage > 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App header with integrated offline toggle
        TopAppBar(
            title = {
                Text(
                    text = "Student Pocket",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            ),
            actions = {
                // High contrast hardware simulation offline toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSimOffline) MaterialTheme.colorScheme.errorContainer 
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                        .clickable { viewModel.setSimulationOfflineMode(!isSimOffline) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isSimOffline) Icons.Default.WifiOff else Icons.Default.Wifi,
                        contentDescription = "Simulated Connectivity",
                        tint = if (isSimOffline) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSimOffline) "Simulated Offline" else "Online Mode",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isSimOffline) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Simulated Offline Warning Banner
            if (isSimOffline) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("offline_banner_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Disconnected State",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Offline Mode is Active",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Simulating zero network. You can only open indexed resources that are saved locally.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Top Search Bar Layout
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search Courses, Codes, or Notes...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search bar icon"
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search query"
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    
                    // Display search overlay list if active
                    AnimatedVisibility(
                        visible = searchQuery.isNotBlank(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .heightIn(max = 280.dp),
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 4.dp,
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            if (searchResults.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.FindInPage,
                                            contentDescription = "No results icon",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No academic resources found",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(searchResults) { resource ->
                                        ResourceSearchRow(
                                            resource = resource,
                                            onResourceClick = onResourceClick
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Offline Metrics Card (Startup Motivation)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Your Offline Vault",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Studying with local independence",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.FolderZip,
                                    contentDescription = "Vault Folder",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Downloded files counter
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${offlineResources.size}",
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Saved Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                                
                                // Favorites counter
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${favoriteResources.size}",
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "Bookmarks/Favs",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Academic Departments Grid Title
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Academic Departments",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.3.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    // Horizontal scrollable college/faculty chips
                    if (colleges.size > 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(colleges) { college ->
                                val isSelected = selectedCollegeFilter == college
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCollegeFilter = college },
                                    label = { Text(college, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("college_chip_$college")
                                )
                            }
                        }
                    }
                }
            }

            // Grid Layout for Departments
            if (departments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                }
            } else {
                val filteredDepts = if (selectedCollegeFilter == "All") {
                    departments
                } else {
                    departments.filter { (it.college ?: "Other") == selectedCollegeFilter }
                }

                if (filteredDepts.isEmpty()) {
                    item {
                        Text(
                            text = "No departments found for this selection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    val chunkedDepts = filteredDepts.chunked(2)
                    chunkedDepts.forEach { rowPairs ->
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowPairs.forEach { dept ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        DepartmentCard(
                                            department = dept,
                                            onTab = { onDepartmentClick(dept) }
                                        )
                                    }
                                }
                                if (rowPairs.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Continue Reading / Studying list
            if (recentReadingList.isNotEmpty()) {
                item {
                    Text(
                        text = "Continue Reading",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.3.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(recentReadingList) { resource ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResourceClick(resource) }
                            .testTag("recent_read_card_${resource.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Recent book icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = resource.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${resource.type} • Last read: Page ${resource.lastReadPage + 1} of ${resource.pageCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                // Reading progress line
                                val progressFraction = if (resource.pageCount > 1) {
                                    (resource.lastReadPage.toFloat() / (resource.pageCount - 1).toFloat()).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                LinearProgressIndicator(
                                    progress = progressFraction,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { onResourceClick(resource) }) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Resume reading button",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            } else {
                // Empty State Quick Study Tip
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "How to start instruction",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Column {
                                Text(
                                    text = "Ready to start?",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Open a department below, find academic resources, click download, and study cleanly offline.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DepartmentCard(
    department: Department,
    onTab: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(125.dp)
            .clickable { onTab() }
            .testTag("dept_card_${department.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        department.id.startsWith("AWTI") || department.id.startsWith("CNCS") -> Icons.Default.Science
                        department.id.startsWith("AMIT-CS") || department.id.startsWith("AMIT-SWE") || department.id.startsWith("AMIT-IT") -> Icons.Default.Terminal
                        department.id.startsWith("AMIT-EE") -> Icons.Default.Bolt
                        department.id.startsWith("AMIT-CE") || department.id.startsWith("SC-CE") -> Icons.Default.Construction
                        department.id.startsWith("AMIT-ME") || department.id.startsWith("SC-EM") || department.id.startsWith("SC-AE") || department.id.startsWith("SC-FE") -> Icons.Default.Engineering
                        department.id.startsWith("CMHS-MD") -> Icons.Default.LocalHospital
                        department.id.startsWith("CMHS") -> Icons.Default.Healing
                        department.id.startsWith("COAS") -> Icons.Default.Grass
                        department.id.startsWith("COBE-AF") || department.id.startsWith("SC-CA") -> Icons.Default.TrendingUp
                        department.id.startsWith("COBE-EC") || department.id.startsWith("SC-FEc") -> Icons.Default.ShowChart
                        department.id.startsWith("COBE") || department.id.startsWith("SC-LS") || department.id.startsWith("SC-BA") || department.id.startsWith("SC-MM") -> Icons.Default.School
                        department.id.startsWith("CSSH") || department.id.startsWith("SC-GO") || department.id.startsWith("SC-CO") || department.id.startsWith("SC-PR") -> Icons.Default.MenuBook
                        department.id.startsWith("SOL-LW") -> Icons.Default.Gavel
                        else -> Icons.Default.School
                    },
                    contentDescription = department.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column {
                Text(
                    text = department.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = department.code,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = department.college ?: "Other",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).padding(start = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ResourceSearchRow(
    resource: Resource,
    onResourceClick: (Resource) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onResourceClick(resource) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (resource.type) {
                "Lecture Notes" -> Icons.Default.Description
                "Handouts" -> Icons.Default.Note
                "Assignments" -> Icons.Default.Assignment
                "Lab Reports" -> Icons.Default. Science
                "Past Exams" -> Icons.Default.Quiz
                else -> Icons.Default.Article
            },
            contentDescription = resource.type,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = resource.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${resource.type} • Size: ${resource.fileSize}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        if (resource.isDownloaded) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Cached",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
