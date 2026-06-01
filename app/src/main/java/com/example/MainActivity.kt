package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.model.Resource
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.CourseBrowserScreen
import com.example.ui.screens.PdfReaderScreen
import com.example.ui.screens.OfflineFavoritesScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PocketViewModel

class MainActivity : ComponentActivity() {

    // Instantiate State ViewModel using simple Factory pattern
    private val viewModel: PocketViewModel by viewModels {
        PocketViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppHost(viewModel)
            }
        }
    }
}

@Composable
fun MainAppHost(viewModel: PocketViewModel) {
    // Simple state flags for Onboarding and Tabbed Navigation
    var isOnboardingRequired by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(0) } // 0 = Explore Dashboard, 1 = Curriculum Syllabus, 2 = Storage Vault

    val activeResource by viewModel.activeResource.collectAsState()

    if (isOnboardingRequired) {
        OnboardingScreen(
            onOnboardingComplete = { isOnboardingRequired = false }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                // Persistent Tab bar at the bottom
                NavigationBar(
                    modifier = Modifier.testTag("app_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.GridView, contentDescription = "Home dashboard") },
                        label = { Text("Explore") },
                        modifier = Modifier.testTag("nav_item_explore")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.School, contentDescription = "Browse resources") },
                        label = { Text("Syllabus") },
                        modifier = Modifier.testTag("nav_item_syllabus")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.Storage, contentDescription = "Storage vault") },
                        label = { Text("Vault") },
                        modifier = Modifier.testTag("nav_item_vault")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Tab display content layout switching
                when (currentTab) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        onDepartmentClick = { dept ->
                            viewModel.selectDepartment(dept)
                            currentTab = 1 // Auto-navigate to Syllabus Tab
                        },
                        onResourceClick = { res ->
                            // Open PDF Reader
                            viewModel.openResourceReader(res)
                        }
                    )
                    1 -> CourseBrowserScreen(
                        viewModel = viewModel,
                        onBack = {
                            viewModel.selectDepartment(null)
                            currentTab = 0 // Go back to Home
                        },
                        onResourceClick = { res ->
                            viewModel.openResourceReader(res)
                        }
                    )
                    2 -> OfflineFavoritesScreen(
                        viewModel = viewModel,
                        onResourceClick = { res ->
                            viewModel.openResourceReader(res)
                        }
                    )
                }
            }
        }

        // Immersive PDF Fullscreen view overlays (covers Scaffold bottom bars)
        AnimatedVisibility(
            visible = activeResource != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PdfReaderScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.closeResourceReader() }
                )
            }
        }
    }
}
