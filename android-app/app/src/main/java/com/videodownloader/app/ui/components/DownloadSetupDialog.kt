package com.videodownloader.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.videodownloader.app.data.model.StorageOption
import com.videodownloader.app.ui.DownloadViewModel
import com.videodownloader.app.ui.theme.GlassAcrylicModal
import com.videodownloader.app.ui.theme.GlassBorderCyan
import com.videodownloader.app.ui.theme.GlassBorderStart
import com.videodownloader.app.ui.theme.M3SurfaceContainer
import com.videodownloader.app.ui.theme.NeonCyan
import com.videodownloader.app.ui.theme.NeonPink
import com.videodownloader.app.ui.theme.NeonViolet
import com.videodownloader.app.ui.theme.TextPrimary
import com.videodownloader.app.ui.theme.TextSecondary
import com.videodownloader.app.ui.theme.TextTertiary
import java.io.File

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun DownloadSetupDialog(
    viewModel: DownloadViewModel
) {
    val context = LocalContext.current
    val showDialog by viewModel.showDownloadDialog.collectAsState()
    val analyzedInfo by viewModel.analyzedInfo.collectAsState()
    val selectedQuality by viewModel.selectedQuality.collectAsState()
    val selectedStorageOption by viewModel.selectedStorageOption.collectAsState()
    val pendingFileName by viewModel.pendingFileName.collectAsState()

    // Seal Features States
    val isTrimEnabled by viewModel.isTrimEnabled.collectAsState()
    val trimStart by viewModel.trimStart.collectAsState()
    val trimEnd by viewModel.trimEnd.collectAsState()
    val isSplitChaptersEnabled by viewModel.isSplitChaptersEnabled.collectAsState()
    val isSponsorBlockEnabled by viewModel.isSponsorBlockEnabled.collectAsState()
    val isEmbedSubsEnabled by viewModel.isEmbedSubsEnabled.collectAsState()
    val isEmbedArtworkEnabled by viewModel.isEmbedArtworkEnabled.collectAsState()
    val turboSpeedEnabled by viewModel.turboSpeedEnabled.collectAsState()
    val customCliArgs by viewModel.customCliArgs.collectAsState()

    var showSealFeatures by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (ignored: Exception) {}

            val pathSegments = uri.path?.split(":") ?: emptyList()
            val folderName = if (pathSegments.size > 1) pathSegments.last() else "CustomFolder"
            val externalDir = context.getExternalFilesDir(null)
            val customDir = File(externalDir ?: context.filesDir, folderName).apply { mkdirs() }
            viewModel.setCustomStorageDirectory(customDir, folderName)
        }
    }

    if (showDialog && analyzedInfo != null) {
        val info = analyzedInfo!!

        Dialog(
            onDismissRequest = { viewModel.dismissDownloadDialog() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        width = 1.2.dp,
                        brush = NovaPillBorderBrush,
                        shape = RoundedCornerShape(26.dp)
                    ),
                color = GlassAcrylicModal,
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    // Fixed Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NovaPillBackgroundBrush)
                                    .border(1.dp, NovaPillBorderBrush, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = NovaIcons.Download,
                                    contentDescription = null,
                                    tint = NovaAccentPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Download Stream",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = info.host,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    if (turboSpeedEnabled) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0x3010B981))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "⚡ 5x TURBO",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF34D399)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        GlassIconButton(
                            onClick = { viewModel.dismissDownloadDialog() },
                            icon = Icons.Default.Close,
                            size = 32.dp
                        )
                    }

                    // Middle Scrollable Content
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Video Title Preview
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        borderBrush = NovaPillBorderBrush,
                        acrylicColor = Color(0x18FFFFFF)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = info.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Category Filter Tabs (Seal Style)
                    var selectedCategory by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("ALL") }
                    val categories = listOf("ALL" to "All Streams", "VIDEO" to "Video Only", "AUDIO" to "Audio Only")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { (catId, catLabel) ->
                            val isTabSelected = selectedCategory == catId
                            val tabBg = if (isTabSelected) NovaPillBackgroundBrush else Brush.linearGradient(listOf(Color(0x12FFFFFF), Color(0x0AFFFFFF)))
                            val tabBorder = if (isTabSelected) NovaPillBorderBrush else Brush.linearGradient(listOf(Color(0x20FFFFFF), Color(0x10FFFFFF)))
                            val tabTextColor = if (isTabSelected) Color.White else TextSecondary

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tabBg)
                                    .border(1.dp, tabBorder, RoundedCornerShape(12.dp))
                                    .clickable { selectedCategory = catId }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = catLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = tabTextColor
                                )
                            }
                        }
                    }

                    // Quality Selection Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SELECT STREAM QUALITY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary,
                            letterSpacing = 1.sp
                        )
                        val filteredCount = info.qualityOptions.filter {
                            when (selectedCategory) {
                                "VIDEO" -> !it.isAudioOnly
                                "AUDIO" -> it.isAudioOnly
                                else -> true
                            }
                        }.size
                        Text(
                            text = "$filteredCount available",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    // Filtered Quality Items
                    val filteredQualities = info.qualityOptions.filter {
                        when (selectedCategory) {
                            "VIDEO" -> !it.isAudioOnly
                            "AUDIO" -> it.isAudioOnly
                            else -> true
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredQualities.forEach { quality ->
                            val isSelected = selectedQuality?.label == quality.label
                            val isSuggested = quality.isSuggested
                            val border = when {
                                isSelected -> Brush.linearGradient(listOf(Color(0xA0FFFFFF), Color(0x80A855F7), Color(0x40FFFFFF)))
                                isSuggested -> Brush.linearGradient(listOf(Color(0x60A855F7), Color(0x30FFFFFF)))
                                else -> Brush.linearGradient(listOf(Color(0x25FFFFFF), Color(0x10FFFFFF)))
                            }
                            val bg = when {
                                isSelected -> Brush.horizontalGradient(listOf(Color(0x358B5CF6), Color(0x20A855F7), Color(0x15FFFFFF)))
                                isSuggested -> Brush.horizontalGradient(listOf(Color(0x188B5CF6), Color(0x10FFFFFF)))
                                else -> Brush.linearGradient(listOf(Color(0x15FFFFFF), Color(0x0AFFFFFF)))
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bg)
                                    .border(1.dp, border, RoundedCornerShape(14.dp))
                                    .clickable { viewModel.selectQuality(quality) }
                                    .padding(horizontal = 14.dp, vertical = 11.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (isSuggested) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x30A855F7))
                                                    .border(0.8.dp, Color(0x80A855F7), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "RECOMMENDED",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFFD8B4FE),
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .background(if (isSelected) Color(0x30A855F7) else Color(0x15FFFFFF))
                                                    .border(1.dp, if (isSelected) Color(0x80A855F7) else Color(0x25FFFFFF), RoundedCornerShape(9.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (quality.isAudioOnly) NovaIcons.MusicNote else NovaIcons.Movie,
                                                    contentDescription = null,
                                                    tint = if (isSelected) NovaAccentPurple else TextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = quality.label,
                                                        color = if (isSelected) Color.White else TextPrimary,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(2.dp))

                                                // Badges row (Resolution, FPS, Codec)
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (quality.codec.isNotBlank()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0x20FFFFFF))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = quality.codec,
                                                                fontSize = 9.sp,
                                                                color = TextSecondary,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                    }

                                                    if (quality.fps != null && quality.fps > 30) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0x20A855F7))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = "${quality.fps}fps",
                                                                fontSize = 9.sp,
                                                                color = Color(0xFFD8B4FE),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        text = if (quality.isAudioOnly) quality.ext.uppercase() else "MP4",
                                                        fontSize = 10.sp,
                                                        color = TextTertiary
                                                    )
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = quality.sizeFormatted,
                                                color = if (isSelected) Color.White else TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0x40A855F7)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = NovaAccentPurple,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Save Location Selection - Single clean option with folder picker
                    Column {
                        Text(
                            text = "DESTINATION STORAGE PATH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x18FFFFFF))
                                .border(1.dp, NovaPillBorderBrush, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NovaPillBackgroundBrush)
                                            .border(1.dp, NovaPillBorderBrush, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = NovaIcons.Folder,
                                            contentDescription = null,
                                            tint = NovaAccentPurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedStorageOption.name,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = selectedStorageOption.file.absolutePath,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NovaPillBackgroundBrush)
                                        .border(1.dp, NovaPillBorderBrush, RoundedCornerShape(8.dp))
                                        .clickable { folderPickerLauncher.launch(null) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Change",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // File Name Edit
                    Column {
                        Text(
                            text = "FILE NAME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        GlassTextField(
                            value = pendingFileName,
                            onValueChange = { viewModel.updatePendingFileName(it) },
                            placeholder = "Enter file name",
                            leadingIcon = NovaIcons.Save
                        )
                    }

                    // Seal Signature Power Tools (Collapsible / Expandable Panel)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x15FFFFFF))
                            .border(1.dp, NovaPillBorderBrush, RoundedCornerShape(14.dp))
                            .clickable { showSealFeatures = !showSealFeatures }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = NovaIcons.ElectricBolt,
                                    contentDescription = null,
                                    tint = NovaAccentPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SEAL ADVANCED POWER TOOLS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Icon(
                                imageVector = if (showSealFeatures) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showSealFeatures,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x10FFFFFF))
                                .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Trimming
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Video Trimming (Clip Section)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Download only a specific range (--download-sections)", color = TextSecondary, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = isTrimEnabled,
                                    onCheckedChange = { viewModel.isTrimEnabled.value = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NovaAccentPurple)
                                )
                            }

                            if (isTrimEnabled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        GlassTextField(
                                            value = trimStart,
                                            onValueChange = { viewModel.trimStart.value = it },
                                            placeholder = "Start: 00:00"
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        GlassTextField(
                                            value = trimEnd,
                                            onValueChange = { viewModel.trimEnd.value = it },
                                            placeholder = "End: 01:30"
                                        )
                                    }
                                }
                            }

                            // 2. Chapters Split
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Split Video by Chapters", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Creates individual files for each chapter", color = TextSecondary, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = isSplitChaptersEnabled,
                                    onCheckedChange = { viewModel.isSplitChaptersEnabled.value = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NovaAccentPurple)
                                )
                            }

                            // 3. SponsorBlock
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SponsorBlock (Auto-Remove Ads)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Cut out sponsor segments automatically", color = TextSecondary, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = isSponsorBlockEnabled,
                                    onCheckedChange = { viewModel.isSponsorBlockEnabled.value = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NovaAccentPurple)
                                )
                            }

                            // 4. Subtitles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Embed Subtitles", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Embed auto-generated & multi-lang subs", color = TextSecondary, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = isEmbedSubsEnabled,
                                    onCheckedChange = { viewModel.isEmbedSubsEnabled.value = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NovaAccentPurple)
                                )
                            }

                            // 5. Artwork & Metadata
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Embed Artwork & Tags", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Embed cover thumbnail & ID3 metadata into file", color = TextSecondary, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = isEmbedArtworkEnabled,
                                    onCheckedChange = { viewModel.isEmbedArtworkEnabled.value = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NovaAccentPurple)
                                )
                            }

                            // 6. Custom CLI Arguments
                            Column {
                                Text("Custom yt-dlp Arguments", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                GlassTextField(
                                    value = customCliArgs,
                                    onValueChange = { viewModel.customCliArgs.value = it },
                                    placeholder = "--geo-bypass --rate-limit 5M"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fixed Action Buttons
            Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassButton(
                            onClick = { viewModel.dismissDownloadDialog() },
                            modifier = Modifier.weight(1f),
                            brush = NovaPillBackgroundBrush,
                            borderBrush = NovaPillBorderBrush,
                            contentColor = TextSecondary
                        ) {
                            Text("Cancel", fontSize = 14.sp)
                        }

                        GlassButton(
                            onClick = { viewModel.confirmAndStartDownload() },
                            modifier = Modifier.weight(1.5f),
                            brush = NovaElevatedButtonBrush,
                            borderBrush = NovaElevatedButtonBorderBrush
                        ) {
                            Icon(
                                imageVector = NovaIcons.Download,
                                contentDescription = null,
                                tint = NovaAccentPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Download", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
