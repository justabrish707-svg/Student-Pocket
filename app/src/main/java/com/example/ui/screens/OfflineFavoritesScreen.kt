package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Resource
import com.example.ui.viewmodel.PocketViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineFavoritesScreen(
    viewModel: PocketViewModel,
    onResourceClick: (Resource) -> Unit
) {
    val favoriteResources by viewModel.favoriteResources.collectAsState()
    val offlineResources by viewModel.offlineResources.collectAsState()
    val isSimOffline by viewModel.isInSimulationOfflineMode.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) } // 0 = Bookmarked (Starred), 1 = Saved Offline (Vault)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Upper screen header
        TopAppBar(
            title = {
                Text(
                    text = "Storage Vault",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        // Tab indicators to toggle favorites vs downloaded resources
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Starred (${favoriteResources.size})", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_starred_files")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.OfflinePin, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Offline (${offlineResources.size})", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_offline_files")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sliding animations for lists
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (selectedTab == 0) {
                // STARRED LIST
                if (favoriteResources.isEmpty()) {
                    EmptyStatePlaceholder(
                        icon = Icons.Default.StarOutline,
                        title = "No Starred Handouts Yet",
                        description = "Tap the star icon on any course material to compile your quick-reference list."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(favoriteResources) { resource ->
                            VaultResourceRow(
                                resource = resource,
                                onRowClick = { onResourceClick(resource) },
                                showFavoriteOption = true,
                                onFavoriteToggle = { viewModel.toggleFavorite(resource) },
                                showPurgeOption = false,
                                onPurge = {}
                            )
                        }
                    }
                }
            } else {
                // OFFLINE VAULT LIST
                if (offlineResources.isEmpty()) {
                    EmptyStatePlaceholder(
                        icon = Icons.Default.CloudQueue,
                        title = "Your Offline Vault is Empty",
                        description = "Once you download slides, books, or exams, they will accumulate securely in this offline vault and stay available."
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Helpful utility hint: free storage space
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = "Disk Space",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "To save disk space on your phone, you can selectively purge downloaded files. They can be re-downloaded later once you are connected.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(offlineResources) { resource ->
                                VaultResourceRow(
                                    resource = resource,
                                    onRowClick = { onResourceClick(resource) },
                                    showFavoriteOption = false,
                                    onFavoriteToggle = {},
                                    showPurgeOption = true,
                                    onPurge = {
                                        viewModel.purgeOfflineResource(resource)
                                        Toast.makeText(context, "Purged ${resource.title} from cache.", Toast.LENGTH_SHORT).show()
                                    }
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
fun EmptyStatePlaceholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun VaultResourceRow(
    resource: Resource,
    onRowClick: () -> Unit,
    showFavoriteOption: Boolean,
    onFavoriteToggle: () -> Unit,
    showPurgeOption: Boolean,
    onPurge: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRowClick() }
            .testTag("vault_item_${resource.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when(resource.type) {
                    "Lecture Notes" -> Icons.Default.Description
                    "Handouts" -> Icons.Default.Note
                    "Assignments" -> Icons.Default.Assignment
                    "Lab Reports" -> Icons.Default.Science
                    "Past Exams" -> Icons.Default.Quiz
                    else -> Icons.Default.Article
                },
                contentDescription = resource.type,
                tint = MaterialTheme.colorScheme.primary,
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            if (showFavoriteOption) {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Starred Icon indicator",
                        tint = Color(0xFFFBBF24)
                    )
                }
            }

            if (showPurgeOption) {
                IconButton(onClick = onPurge) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Purge file cache",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}
