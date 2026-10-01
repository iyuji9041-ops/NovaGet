package com.videodownloader.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videodownloader.app.ui.components.GlassBackground
import com.videodownloader.app.ui.components.NovaIcons
import com.videodownloader.app.ui.screens.BrowserScreen
import com.videodownloader.app.ui.screens.DownloadsScreen
import com.videodownloader.app.ui.screens.HomeScreen
import com.videodownloader.app.ui.screens.PlayerScreen
import com.videodownloader.app.ui.screens.SettingsScreen
import com.videodownloader.app.ui.theme.GlassAcrylicModal
import com.videodownloader.app.ui.theme.GlassBorderCyan
import com.videodownloader.app.ui.theme.GlassBorderStart
import com.videodownloader.app.ui.theme.NeonCyan
import com.videodownloader.app.ui.theme.NeonPink
import com.videodownloader.app.ui.theme.NeonViolet
import com.videodownloader.app.ui.theme.TextPrimary
import com.videodownloader.app.ui.theme.TextSecondary
import com.videodownloader.app.ui.theme.TextTertiary

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DownloadViewModel
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val activePlayingVideo by viewModel.activePlayingVideo.collectAsState()
    val liveTasks by viewModel.liveTasks.collectAsState()
    val activeCount = liveTasks.size

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                // Floating Glassmorphic Bottom Navigation Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(GlassAcrylicModal)
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        GlassBorderCyan.copy(alpha = 0.5f),
                                        GlassBorderStart.copy(alpha = 0.3f),
                                        NeonViolet.copy(alpha = 0.4f)
                                    )
                                ),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NavBarItem(
                                label = "Home",
                                selectedIcon = Icons.Default.Home,
                                unselectedIcon = Icons.Default.Home,
                                isSelected = selectedTab == NavTab.HOME,
                                onClick = { viewModel.selectTab(NavTab.HOME) }
                            )

                            NavBarItem(
                                label = "Browser",
                                selectedIcon = NovaIcons.Language,
                                unselectedIcon = NovaIcons.Language,
                                isSelected = selectedTab == NavTab.BROWSER,
                                onClick = { viewModel.selectTab(NavTab.BROWSER) }
                            )

                            NavBarItem(
                                label = "Downloads",
                                selectedIcon = NovaIcons.Download,
                                unselectedIcon = NovaIcons.Download,
                                isSelected = selectedTab == NavTab.DOWNLOADS,
                                badgeCount = activeCount,
                                onClick = { viewModel.selectTab(NavTab.DOWNLOADS) }
                            )

                            NavBarItem(
                                label = "Settings",
                                selectedIcon = Icons.Default.Settings,
                                unselectedIcon = Icons.Default.Settings,
                                isSelected = selectedTab == NavTab.SETTINGS,
                                onClick = { viewModel.selectTab(NavTab.SETTINGS) }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Persistent Tab View Container for Zero-Lag, Instant Tab Switching
                // Keeps screen states alive, eliminating heavy WebView and database recomposition freeze
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (selectedTab == NavTab.HOME) Modifier else Modifier.size(0.dp).alpha(0f))
                ) {
                    HomeScreen(viewModel = viewModel, contentPadding = paddingValues)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (selectedTab == NavTab.BROWSER) Modifier else Modifier.size(0.dp).alpha(0f))
                ) {
                    BrowserScreen(viewModel = viewModel, contentPadding = paddingValues)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (selectedTab == NavTab.DOWNLOADS) Modifier else Modifier.size(0.dp).alpha(0f))
                ) {
                    DownloadsScreen(viewModel = viewModel, contentPadding = paddingValues)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (selectedTab == NavTab.SETTINGS) Modifier else Modifier.size(0.dp).alpha(0f))
                ) {
                    SettingsScreen(viewModel = viewModel, contentPadding = paddingValues)
                }
            }
        }

        // Fullscreen In-App Video Player Overlay
        activePlayingVideo?.let { video ->
            PlayerScreen(
                video = video,
                onClose = { viewModel.closePlayer() },
                onShare = { viewModel.shareVideo(context, video) }
            )
        }
    }
}

@Composable
private fun NavBarItem(
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isSelected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box {
                Icon(
                    imageVector = if (isSelected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    tint = if (isSelected) NeonCyan else TextTertiary,
                    modifier = Modifier.size(22.dp)
                )

                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(NeonPink),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            color = TextPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                color = if (isSelected) NeonCyan else TextSecondary,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
