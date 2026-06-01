package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookOnline
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    
    val pages = listOf(
        OnboardingPageData(
            title = "AMU Student Pocket",
            description = "The ultimate academic companion for Arba Minch University students. Access course slide decks, notes, and handouts instantly.",
            icon = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                    // Modern styled canvas glow icon
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(Color(0x3360A5FA), Color.Transparent),
                                radius = size.minDimension / 1.5f
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "School Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }
        ),
        OnboardingPageData(
            title = "Strictly Offline-First",
            description = "Ethiopian campuses challenge connectivity. Save syllabus, lectures, assignments, and past exams locally from smis.amu.edu.et content so you can study anytime, offline.",
            icon = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(Color(0x332DD4BF), Color.Transparent),
                                radius = size.minDimension / 1.5f
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "Offline Ready Icon",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }
        ),
        OnboardingPageData(
            title = "Reachable for All Colleges",
            description = "From AMIT Computing & Civil Engineering to Medicine, Law, Agriculture, and Economics. No more messy Telegram groups, everything cataloged clearly.",
            icon = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(Color(0x33FBBF24), Color.Transparent),
                                radius = size.minDimension / 1.5f
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.BookOnline,
                        contentDescription = "Organized Resources",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(72.dp)
                    )
                }
            }
        )
    )

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // App branding top line
            Text(
                text = "STUDENT POCKET",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                ),
                modifier = Modifier.padding(top = 16.dp)
            )

            // Dynamic page transitions
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "onboarding_page_transition"
            ) { pageIdx ->
                val page = pages[pageIdx]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    page.icon()
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // Bottom action and dots bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Dot indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    pages.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (index == currentPage) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentPage) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                                )
                        )
                    }
                }

                // Call to Action
                Button(
                    onClick = {
                        if (currentPage < pages.lastIndex) {
                            currentPage++
                        } else {
                            onOnboardingComplete()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("onboarding_continue_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentPage == pages.lastIndex) "Get Started" else "Next",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Icon"
                        )
                    }
                }
            }
        }
    }
}

data class OnboardingPageData(
    val title: String,
    val description: String,
    val icon: @Composable () -> Unit
)
