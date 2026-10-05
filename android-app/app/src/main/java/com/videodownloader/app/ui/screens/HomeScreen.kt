package com.videodownloader.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.videodownloader.app.ui.DownloadViewModel
import com.videodownloader.app.ui.components.DownloadSetupDialog
import com.videodownloader.app.ui.components.GlassBadge
import com.videodownloader.app.ui.components.GlassButton
import com.videodownloader.app.ui.components.GlassCard
import com.videodownloader.app.ui.components.GlassIconButton
import com.videodownloader.app.ui.components.GlassLinearProgressIndicator
import com.videodownloader.app.ui.components.GlassProgressBar
import com.videodownloader.app.ui.components.GlassTextField
import com.videodownloader.app.ui.components.NovaAccentPurple
import com.videodownloader.app.ui.components.NovaAccentPurpleSoft
import com.videodownloader.app.ui.components.NovaElevatedButtonBorderBrush
import com.videodownloader.app.ui.components.NovaElevatedButtonBrush
import com.videodownloader.app.ui.components.NovaIcons
import com.videodownloader.app.ui.components.NovaPill
import com.videodownloader.app.ui.components.NovaPillBackgroundBrush
import com.videodownloader.app.ui.components.NovaPillBorderBrush
import com.videodownloader.app.ui.theme.GlassAcrylicElevated
import com.videodownloader.app.ui.theme.GlassAcrylicHighlight
import com.videodownloader.app.ui.theme.GlassBorderCyan
import com.videodownloader.app.ui.theme.GlassBorderStart
import com.videodownloader.app.ui.theme.NeonAmber
import com.videodownloader.app.ui.theme.NeonCyan
import com.videodownloader.app.ui.theme.NeonEmerald
import com.videodownloader.app.ui.theme.NeonPink
import com.videodownloader.app.ui.theme.NeonViolet
import com.videodownloader.app.ui.theme.TextPrimary
import com.videodownloader.app.ui.theme.TextSecondary
import com.videodownloader.app.ui.theme.TextTertiary

@Composable
fun HomeScreen(
    viewModel: DownloadViewModel,
    contentPadding: PaddingValues
) {
    val urlInput by viewModel.urlInput.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analyzedInfo by viewModel.analyzedInfo.collectAsState()
    val analysisError by viewModel.analysisError.collectAsState()
    val selectedQuality by viewModel.selectedQuality.collectAsState()
    val liveTasks by viewModel.liveTasks.collectAsState()
    val activeDownloads = liveTasks.values.toList()
    val isUpdatingEngine by viewModel.isUpdatingEngine.collectAsState()
    val otaButtonLabel by viewModel.otaButtonLabel.collectAsState()
    val otaUpdateStatus by viewModel.otaUpdateStatus.collectAsState()
    val context = LocalContext.current

    DownloadSetupDialog(viewModel = viewModel)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Header Hero
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Stylish NovaGet Frosted Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0x28FFFFFF),
                                        Color(0x12FFFFFF)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(0x60FFFFFF),
                                        Color(0x20FFFFFF)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = NovaIcons.ElectricBolt,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Zaswix",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    // OTA Update Frosted Pill (replaces bulky engine version string)
                    val isAlreadyUpdated = otaButtonLabel.contains("Already") || otaButtonLabel.contains("✓")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.horizontalGradient(
                                    if (isAlreadyUpdated) {
                                        listOf(Color(0x3300E676), Color(0x1800E676))
                                    } else {
                                        listOf(Color(0x28FFFFFF), Color(0x12FFFFFF))
                                    }
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    if (isAlreadyUpdated) {
                                        listOf(Color(0x8000E676), Color(0x3000E676))
                                    } else {
                                        listOf(Color(0x60FFFFFF), Color(0x20FFFFFF))
                                    }
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable(enabled = !isUpdatingEngine) {
                                viewModel.updateEngine(context)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isUpdatingEngine) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    color = NovaAccentPurple,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isAlreadyUpdated) Icons.Default.Check else Icons.Default.Refresh,
                                    contentDescription = "OTA Update",
                                    tint = if (isAlreadyUpdated) Color(0xFF00E676) else NovaAccentPurple,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = otaButtonLabel,
                                color = if (isAlreadyUpdated) Color(0xFF00E676) else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ultra-Fast Video Downloader",
                    fontSize = 20.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Download 4K, 1080p, 720p & Studio Audio directly on Phone CPU",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Search & Paste Box (ChatGPT Stadium Capsule Style from IMG_20260925_093355.jpg)
        item {
            com.videodownloader.app.ui.components.StadiumCapsuleSearchBar(
                value = urlInput,
                onValueChange = { viewModel.onUrlInputChanged(it) },
                onAnalyzeClick = { viewModel.analyzeUrl() },
                onPasteClick = { viewModel.pasteFromClipboard() },
                placeholder = "Paste video link to download (YouTube, Insta, X...)",
                isLoading = isAnalyzing,
                onClearClick = { viewModel.clearUrl() }
            )
        }

        // Platform Quick Pills
        item {
            Column {
                Text(
                    text = "Supported Platforms",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    letterSpacing = 0.8.sp,
                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                )
                Spacer(modifier = Modifier.height(8.dp))
                val platforms = listOf(
                    Triple("YouTube", Color(0xFFEF4444), "https://m.youtube.com"),
                    Triple("Instagram", Color(0xFFEC4899), "https://www.instagram.com"),
                    Triple("Facebook", Color(0xFF3B82F6), "https://m.facebook.com"),
                    Triple("TikTok", Color(0xFFA855F7), "https://www.tiktok.com"),
                    Triple("Twitter / X", Color(0xFFF59E0B), "https://x.com"),
                    Triple("Direct MP4", Color(0xFF10B981), "")
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(platforms) { (name, color, url) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x18FFFFFF))
                                .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(12.dp))
                                .clickable(enabled = url.isNotBlank()) {
                                    if (url.isNotBlank()) {
                                        viewModel.openPlatformInBrowser(url)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = name,
                                color = color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Analyzing State Indicator
        item {
            AnimatedVisibility(visible = isAnalyzing) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderBrush = Brush.linearGradient(listOf(Color(0x60FFFFFF), Color(0x20FFFFFF)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = NovaIcons.Speed,
                                    contentDescription = null,
                                    tint = NovaAccentPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Analyzing Video Metadata...",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            NovaPill(
                                text = "Python 3.12 Engine",
                                leadingIcon = NovaIcons.ElectricBolt,
                                iconTint = NovaAccentPurple
                            )
                        }
                        GlassLinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = "Extracting video resolutions (4K/1080p/720p) & studio audio streams",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Error State Card
        item {
            AnimatedVisibility(visible = analysisError != null && !isAnalyzing) {
                analysisError?.let { err ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderBrush = Brush.horizontalGradient(listOf(NeonPink, NeonAmber)),
                        acrylicColor = Color(0x22F43F5E)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NeonPink.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = NeonPink,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = err,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Analysis Result Card (Displays ALL available qualities)
        item {
            AnimatedVisibility(visible = analyzedInfo != null && !isAnalyzing) {
                analyzedInfo?.let { info ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderBrush = NovaPillBorderBrush,
                        acrylicColor = GlassAcrylicHighlight
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Title & Host
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    NovaPill(
                                        text = info.host.uppercase(),
                                        leadingIcon = NovaIcons.Speed,
                                        iconTint = NovaAccentPurple
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = info.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (info.durationSeconds > 0) {
                                    val m = info.durationSeconds / 60
                                    val s = info.durationSeconds % 60
                                    val timeStr = "%02d:%02d".format(m, s)
                                    NovaPill(
                                        text = timeStr,
                                        leadingIcon = NovaIcons.Speed,
                                        iconTint = NovaAccentPurple
                                    )
                                }
                            }

                            // Category Filter Tabs (Seal Style)
                            var homeCategory by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("ALL") }
                            val homeCategories = listOf("ALL" to "All Streams", "VIDEO" to "Video Only", "AUDIO" to "Audio Only")

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                homeCategories.forEach { (catId, catLabel) ->
                                    val isTabSelected = homeCategory == catId
                                    val tabBg = if (isTabSelected) NovaPillBackgroundBrush else Brush.linearGradient(listOf(Color(0x12FFFFFF), Color(0x0AFFFFFF)))
                                    val tabBorder = if (isTabSelected) NovaPillBorderBrush else Brush.linearGradient(listOf(Color(0x20FFFFFF), Color(0x10FFFFFF)))
                                    val tabTextColor = if (isTabSelected) Color.White else TextSecondary

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(tabBg)
                                            .border(1.dp, tabBorder, RoundedCornerShape(12.dp))
                                            .clickable { homeCategory = catId }
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

                            // Full Quality & Audio List
                            val filteredHomeQualities = info.qualityOptions.filter {
                                when (homeCategory) {
                                    "VIDEO" -> !it.isAudioOnly
                                    "AUDIO" -> it.isAudioOnly
                                    else -> true
                                }
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Stream Qualities (${filteredHomeQualities.size})",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextTertiary,
                                        letterSpacing = 0.8.sp,
                                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                                    )
                                    Text(
                                        text = "Tap to select",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    filteredHomeQualities.forEach { quality ->
                                        val isSelected = selectedQuality?.label == quality.label
                                        val isSuggested = quality.isSuggested
                                        val shape = RoundedCornerShape(14.dp)
                                        val bg = when {
                                            isSelected -> Brush.horizontalGradient(listOf(Color(0x358B5CF6), Color(0x20A855F7), Color(0x15FFFFFF)))
                                            isSuggested -> Brush.horizontalGradient(listOf(Color(0x188B5CF6), Color(0x10FFFFFF)))
                                            else -> Brush.linearGradient(listOf(Color(0x12FFFFFF), Color(0x0AFFFFFF)))
                                        }
                                        val border = when {
                                            isSelected -> Brush.linearGradient(listOf(Color(0xA0FFFFFF), Color(0x80A855F7), Color(0x40FFFFFF)))
                                            isSuggested -> Brush.linearGradient(listOf(Color(0x60A855F7), Color(0x30FFFFFF)))
                                            else -> Brush.linearGradient(listOf(Color(0x25FFFFFF), Color(0x10FFFFFF)))
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(shape)
                                                .background(bg)
                                                .border(1.dp, border, shape)
                                                .clickable { viewModel.selectQuality(quality) }
                                                .padding(horizontal = 14.dp, vertical = 11.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isSuggested) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0x30A855F7))
                                                        .border(0.8.dp, Color(0x80A855F7), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Recommended",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFFD8B4FE),
                                                        letterSpacing = 0.5.sp,
                                                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
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
                                                        Text(
                                                            text = quality.label,
                                                            color = if (isSelected) Color.White else TextPrimary,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )

                                                        Spacer(modifier = Modifier.height(2.dp))

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

                            // Download Action
                            GlassButton(
                                onClick = { viewModel.openDownloadDialog() },
                                modifier = Modifier.fillMaxWidth(),
                                brush = NovaElevatedButtonBrush,
                                borderBrush = NovaElevatedButtonBorderBrush
                            ) {
                                Icon(
                                    imageVector = NovaIcons.Download,
                                    contentDescription = null,
                                    tint = NovaAccentPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Download & Select File Path",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Downloads Quick View
        if (activeDownloads.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Downloads (${activeDownloads.size})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary,
                            letterSpacing = 0.8.sp,
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                        )
                        Text(
                            text = "View All",
                            fontSize = 12.sp,
                            color = NovaAccentPurpleSoft,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { viewModel.selectTab(com.videodownloader.app.ui.NavTab.DOWNLOADS) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    activeDownloads.take(2).forEach { task ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            acrylicColor = GlassAcrylicElevated
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Downloading stream...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${(task.progress * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NovaAccentPurpleSoft
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                GlassProgressBar(progress = task.progress, height = 6.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = task.speedFormatted,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    if (task.etaSeconds > 0) {
                                        Text(
                                            text = "ETA: ${task.etaSeconds}s",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
