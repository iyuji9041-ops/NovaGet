package com.videodownloader.app.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.videodownloader.app.data.model.VideoDownloadEntity
import com.videodownloader.app.ui.components.GlassIconButton
import com.videodownloader.app.ui.components.NovaIcons
import com.videodownloader.app.ui.theme.NeonCyan
import com.videodownloader.app.ui.theme.TextPrimary
import com.videodownloader.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun PlayerScreen(
    video: VideoDownloadEntity,
    onClose: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableStateOf(0) }
    var currentPositionMs by remember { mutableStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }

    BackHandler {
        onClose()
    }

    // Auto-hide controls timer
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Position update polling
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            videoViewInstance?.let { vv ->
                currentPositionMs = vv.currentPosition
                if (vv.duration > 0) durationMs = vv.duration
            }
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewInstance?.stopPlayback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { controlsVisible = !controlsVisible }
    ) {
        // Native Android VideoView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    val file = File(video.localPath)
                    if (file.exists()) {
                        setVideoPath(file.absolutePath)
                    } else {
                        setVideoURI(Uri.parse(video.url))
                    }

                    setOnPreparedListener { mp ->
                        durationMs = mp.duration
                        mp.isLooping = true
                        start()
                        isPlaying = true
                    }

                    setOnCompletionListener {
                        isPlaying = false
                    }

                    videoViewInstance = this
                }
            }
        )

        // Overlay Controls
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        GlassIconButton(
                            onClick = onClose,
                            icon = Icons.Default.ArrowBack,
                            size = 36.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = video.title,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    GlassIconButton(
                        onClick = onShare,
                        icon = Icons.Default.Share,
                        size = 36.dp
                    )
                }

                // Center Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s
                    GlassIconButton(
                        onClick = {
                            videoViewInstance?.let { vv ->
                                val newPos = (vv.currentPosition - 10000).coerceAtLeast(0)
                                vv.seekTo(newPos)
                                currentPositionMs = newPos
                            }
                        },
                        icon = NovaIcons.FastRewind,
                        size = 46.dp
                    )

                    // Play/Pause Big Button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xE0FFFFFF), Color(0xB0FFFFFF))))
                            .border(1.dp, Color(0x60FFFFFF), CircleShape)
                            .clickable {
                                videoViewInstance?.let { vv ->
                                    if (vv.isPlaying) {
                                        vv.pause()
                                        isPlaying = false
                                    } else {
                                        vv.start()
                                        isPlaying = true
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) NovaIcons.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Fast Forward 10s
                    GlassIconButton(
                        onClick = {
                            videoViewInstance?.let { vv ->
                                val newPos = (vv.currentPosition + 10000).coerceAtMost(durationMs)
                                vv.seekTo(newPos)
                                currentPositionMs = newPos
                            }
                        },
                        icon = NovaIcons.FastForward,
                        size = 46.dp
                    )
                }

                // Bottom Seekbar & Time
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    val progressRatio = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
                    Slider(
                        value = progressRatio.coerceIn(0f, 1f),
                        onValueChange = { ratio ->
                            val targetMs = (ratio * durationMs.toFloat()).toInt()
                            videoViewInstance?.seekTo(targetMs)
                            currentPositionMs = targetMs
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0x40FFFFFF)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatTime(durationMs),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSecs = (ms / 1000).coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%02d:%02d".format(mins, secs)
}
