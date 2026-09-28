package com.videodownloader.app.ui.screens

import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videodownloader.app.data.model.DownloadTaskState
import com.videodownloader.app.data.model.VideoDownloadEntity
import com.videodownloader.app.ui.DownloadViewModel
import com.videodownloader.app.ui.NavTab
import com.videodownloader.app.ui.components.GlassIconButton
import com.videodownloader.app.ui.components.NovaAccentPurple
import com.videodownloader.app.ui.components.NovaIcons
import com.videodownloader.app.ui.theme.NeonCyan
import com.videodownloader.app.ui.theme.NeonEmerald
import com.videodownloader.app.ui.theme.NeonPink
import com.videodownloader.app.ui.theme.NeonViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * VRKA-inspired Frosted Cyber-Glass Downloads & Queue Screen:
 * - Ultra-deep AMOLED pitch-black background with subtle ambient purple atmosphere
 * - Translucent glass cards with specular top-rim highlight sheen (Canvas gradient)
 * - Animated sliding segmented control with glowing puck and monospace telemetry badges
 * - High-tech download progress bars with neon gradient, speed readouts, and status dots
 * - Aesthetic empty queue & history states with glowing circular glass emblem
 */
@Composable
fun DownloadsScreen(
    viewModel: DownloadViewModel,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val allDownloads by viewModel.downloads.collectAsState()
    val liveTasks by viewModel.liveTasks.collectAsState()

    var selectedSegment by remember { mutableStateOf(0) } // 0 = Active / Queue, 1 = Completed / History

    val activeDownloads = remember(allDownloads, liveTasks) {
        val active = allDownloads.filter {
            it.status == VideoDownloadEntity.STATUS_DOWNLOADING || it.status == VideoDownloadEntity.STATUS_PAUSED
        }.toMutableList()
        for (taskId in liveTasks.keys) {
            if (active.none { it.id == taskId }) {
                allDownloads.firstOrNull { it.id == taskId }?.let { active.add(0, it) }
            }
        }
        active
    }

    val completedDownloads = remember(allDownloads) {
        allDownloads.filter { it.status == VideoDownloadEntity.STATUS_COMPLETED }
    }

    androidx.compose.runtime.LaunchedEffect(activeDownloads.size, completedDownloads.size) {
        if (activeDownloads.isNotEmpty() && completedDownloads.isEmpty()) {
            selectedSegment = 0
        }
    }

    val freeSpace = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
            DownloadTaskState.formatBytes(bytesAvailable)
        } catch (e: Exception) {
            "46.2 GB"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F101A), // Subtle top cosmic violet glow
                        Color(0xFF07080D), // Deep obsidian
                        Color(0xFF030305)  // Pure AMOLED black
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top VRKA Glass Header & Segmented Pill Container
            item {
                VrkaTopGlassHeader(
                    activeCount = activeDownloads.size,
                    completedCount = completedDownloads.size,
                    freeSpace = freeSpace,
                    selectedSegment = selectedSegment,
                    onSegmentSelected = { selectedSegment = it }
                )
            }

            // Segment 0: Active / Queue
            if (selectedSegment == 0) {
                if (activeDownloads.isEmpty() && liveTasks.isEmpty()) {
                    item {
                        VrkaEmptyPlaceholder(
                            isQueue = true,
                            onGoHome = { viewModel.selectTab(NavTab.HOME) },
                            onGoBrowser = { viewModel.selectTab(NavTab.BROWSER) }
                        )
                    }
                } else {
                    items(activeDownloads, key = { it.id }) { video ->
                        val liveState = liveTasks[video.id]
                        VrkaActiveDownloadCard(
                            video = video,
                            liveState = liveState,
                            onPause = { viewModel.pauseDownload(video.id) },
                            onResume = { viewModel.resumeDownload(video.id) },
                            onCancel = { viewModel.cancelDownload(video.id) }
                        )
                    }
                }
            }

            // Segment 1: Completed / History
            if (selectedSegment == 1) {
                if (completedDownloads.isEmpty()) {
                    item {
                        VrkaEmptyPlaceholder(
                            isQueue = false,
                            onGoHome = { viewModel.selectTab(NavTab.HOME) },
                            onGoBrowser = { viewModel.selectTab(NavTab.BROWSER) }
                        )
                    }
                } else {
                    items(completedDownloads, key = { it.id }) { video ->
                        VrkaCompletedDownloadCard(
                            video = video,
                            onPlay = { viewModel.openInPlayer(video) },
                            onShare = { viewModel.shareVideo(context, video) },
                            onOpenExternal = { viewModel.openExternal(context, video) },
                            onDelete = { viewModel.deleteDownload(video) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

/**
 * VRKA-inspired Frosted Glass Top Header Card with Segmented Puck Switcher
 */
@Composable
private fun VrkaTopGlassHeader(
    activeCount: Int,
    completedCount: Int,
    freeSpace: String,
    selectedSegment: Int,
    onSegmentSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x35211D36), // Frosted glass amethyst top
                        Color(0x2012131F)  // Translucent dark obsidian bottom
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x55FFFFFF), // Crisp specular highlight rim
                        Color(0x15FFFFFF),
                        Color(0x30A855F7)  // Cyber purple lower rim
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        // Specular sheen overlay across top corner
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x28FFFFFF),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = 46.dp.toPx()
                ),
                cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title + Telemetry readout + Free Storage Glass Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0x40A855F7),
                                        Color(0x208B5CF6)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                Brush.linearGradient(listOf(Color(0x80C084FC), Color(0x30A855F7))),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NovaIcons.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Downloads",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp,
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (activeCount > 0) Color(0xFF06B6D4) else Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$activeCount active • $completedCount completed",
                                fontSize = 12.sp,
                                color = Color(0xFFC4B5FD),
                                fontWeight = FontWeight.Medium,
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // VRKA Storage Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x25FFFFFF))
                        .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = NovaIcons.Folder,
                            contentDescription = null,
                            tint = NovaAccentPurple,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$freeSpace Free",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0),
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                        )
                    }
                }
            }

            // VRKA Segmented Puck Switcher (Queue vs History)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x400A0B14))
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0x35FFFFFF),
                                Color(0x12FFFFFF)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        Triple("Queue", activeCount, NovaIcons.Download),
                        Triple("History", completedCount, NovaIcons.DownloadDone)
                    )

                    tabs.forEachIndexed { index, (label, count, icon) ->
                        val isSelected = selectedSegment == index
                        val bgBrush = if (isSelected) {
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFA855F7), // Neon Purple
                                    Color(0xFF7C3AED)  // Deep Royal Violet
                                )
                            )
                        } else {
                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                        val borderBrush = if (isSelected) {
                            Brush.linearGradient(
                                listOf(
                                    Color(0xE0FFFFFF),
                                    Color(0x60C084FC)
                                )
                            )
                        } else {
                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(bgBrush)
                                .border(1.2.dp, borderBrush, RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onSegmentSelected(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFF9CA3AF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else Color(0xFF9CA3AF),
                                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0x35FFFFFF) else Color(0x18FFFFFF))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF9CA3AF)
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

/**
 * VRKA High-Tech Glass Active Download Card with Specular Sheen & Neon Telemetry
 */
@Composable
private fun VrkaActiveDownloadCard(
    video: VideoDownloadEntity,
    liveState: DownloadTaskState?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val progress = liveState?.progress ?: video.progress
    val speed = liveState?.speedFormatted ?: if (video.isPaused) "Paused" else "Connecting..."
    val downloadedBytes = liveState?.downloadedBytes ?: video.downloadedBytes
    val totalBytes = if ((liveState?.totalBytes ?: 0L) > 0) liveState!!.totalBytes else video.fileSize

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "active_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x301E1C2E),
                        Color(0x1C11121C)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x45FFFFFF),
                        Color(0x12FFFFFF),
                        Color(0x28A855F7)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x1CFFFFFF), Color.Transparent),
                    startY = 0f,
                    endY = 36.dp.toPx()
                ),
                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title & Status Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x25A855F7))
                            .border(1.dp, Color(0x40C084FC), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (video.mimeType.contains("audio")) NovaIcons.MusicNote else NovaIcons.Movie,
                            contentDescription = null,
                            tint = NovaAccentPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title.ifBlank { "Downloading Media..." },
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (video.isPaused) Color(0xFFF59E0B) else Color(0xFF06B6D4))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (video.isPaused) "Paused" else "Downloading",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (video.isPaused) Color(0xFFF59E0B) else Color(0xFF06B6D4),
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "•  ${video.quality}",
                                fontSize = 11.sp,
                                color = Color(0xFFA1A1AA),
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                            )
                        }
                    }
                }

                // Action buttons: Pause/Resume + Cancel
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (video.isDownloading) {
                        GlassIconButton(
                            onClick = onPause,
                            icon = NovaIcons.Pause,
                            size = 32.dp,
                            tint = Color(0xFFFBBF24),
                            backgroundColor = Color(0x25FFFFFF)
                        )
                    } else {
                        GlassIconButton(
                            onClick = onResume,
                            icon = NovaIcons.Download,
                            size = 32.dp,
                            tint = Color.White,
                            backgroundColor = Color(0x25FFFFFF)
                        )
                    }
                    GlassIconButton(
                        onClick = onCancel,
                        icon = Icons.Default.Delete,
                        size = 32.dp,
                        tint = Color(0xFFF87171),
                        backgroundColor = Color(0x25FFFFFF)
                    )
                }
            }

            // High-Tech Linear Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x35000000))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFA855F7),
                                    Color(0xFF38BDF8)
                                )
                            )
                        )
                )
            }

            // Telemetry Row (Percent • Speed • Bytes)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                )

                Text(
                    text = speed,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (video.isPaused) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                )

                Text(
                    text = "${DownloadTaskState.formatBytes(downloadedBytes)} / ${DownloadTaskState.formatBytes(totalBytes)}",
                    fontSize = 11.sp,
                    color = Color(0xFFA1A1AA),
                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                )
            }
        }
    }
}

/**
 * VRKA Glass Completed / History Card with Specular Highlights & Monospace Chips
 */
@Composable
private fun VrkaCompletedDownloadCard(
    video: VideoDownloadEntity,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onOpenExternal: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isAudio = video.mimeType.contains("audio")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x281F1C30),
                        Color(0x1810111A)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x40FFFFFF),
                        Color(0x12FFFFFF),
                        Color(0x25A855F7)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onPlay() }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x18FFFFFF), Color.Transparent),
                    startY = 0f,
                    endY = 36.dp.toPx()
                ),
                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Media badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0x35A855F7),
                                Color(0x158B5CF6)
                            )
                        )
                    )
                    .border(1.dp, Color(0x40C084FC), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAudio) NovaIcons.MusicNote else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isAudio) NeonPink else NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                val dateStr = remember(video.timestamp) {
                    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(video.timestamp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VrkaMiniChip(text = video.quality.ifBlank { if (isAudio) "MP3" else "MP4" })
                    VrkaMiniChip(text = DownloadTaskState.formatBytes(video.fileSize))
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                    )
                }
            }

            // Quick Actions: Play Pill + More Dropdown
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x20A855F7))
                        .border(1.dp, Color(0x40A855F7), RoundedCornerShape(10.dp))
                        .clickable { onPlay() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Play",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box {
                    GlassIconButton(
                        onClick = { showMenu = true },
                        icon = Icons.Default.MoreVert,
                        size = 32.dp,
                        backgroundColor = Color.Transparent
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Play in App") },
                            onClick = {
                                showMenu = false
                                onPlay()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonCyan)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share File") },
                            onClick = {
                                showMenu = false
                                onShare()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null, tint = NeonViolet)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open in External Player") },
                            onClick = {
                                showMenu = false
                                onOpenExternal()
                            },
                            leadingIcon = {
                                Icon(NovaIcons.OpenInNew, contentDescription = null, tint = NeonEmerald)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = NeonPink) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = NeonPink)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VrkaMiniChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x22FFFFFF))
            .border(0.8.dp, Color(0x25FFFFFF), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFD4D4D8),
            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
        )
    }
}

/**
 * VRKA-inspired Empty State with Atmospheric Glass Halo & Action Buttons
 */
@Composable
private fun VrkaEmptyPlaceholder(
    isQueue: Boolean,
    onGoHome: () -> Unit,
    onGoBrowser: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x251F1C32),
                        Color(0x1210111A)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x40FFFFFF),
                        Color(0x10FFFFFF),
                        Color(0x25A855F7)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Ambient Emblem
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0x50A855F7),
                                Color(0x158B5CF6),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        1.2.dp,
                        Brush.linearGradient(listOf(Color(0x90C084FC), Color(0x30A855F7))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isQueue) NovaIcons.Download else NovaIcons.DownloadDone,
                    contentDescription = null,
                    tint = NovaAccentPurple,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isQueue) "Queue is empty" else "No download history",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isQueue)
                    "Media you enqueue will appear here while downloading."
                else
                    "Completed and archived downloads will be listed here.",
                fontSize = 13.sp,
                color = Color(0xFFA1A1AA),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Pills
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFA855F7),
                                    Color(0xFF7C3AED)
                                )
                            )
                        )
                        .border(1.dp, Color(0xC0FFFFFF), RoundedCornerShape(14.dp))
                        .clickable { onGoHome() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = NovaIcons.ContentPaste,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Paste Link",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(14.dp))
                        .clickable { onGoBrowser() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = NovaIcons.Language,
                            contentDescription = null,
                            tint = NovaAccentPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Browse Web",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE2E8F0),
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                        )
                    }
                }
            }
        }
    }
}
