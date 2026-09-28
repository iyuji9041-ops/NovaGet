package com.videodownloader.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videodownloader.app.engine.ExtractionEngine
import com.videodownloader.app.ui.DownloadViewModel
import com.videodownloader.app.ui.NavTab
import com.videodownloader.app.ui.components.NovaAccentPurple
import com.videodownloader.app.ui.components.NovaIcons
import com.videodownloader.app.ui.components.NovaPill
import com.videodownloader.app.ui.components.NovaPillBackgroundBrush
import com.videodownloader.app.ui.components.NovaPillBorderBrush
import androidx.compose.ui.window.Dialog
import java.io.File

/**
 * SettingsScreen designed exactly according to the user's provided XML layout:
 * - Card background: @drawable/dialog_bg with 24dp padding & 16dp margin
 * - Header: ⚙️ Settings (22sp, bold, #FFFFFF)
 * - CheckBoxes: 16sp, #E0E0E0 ("Keep screen ON while touch is locked.", etc.)
 * - Action buttons row (gravity="end"):
 *   - Cancel: transparent background, #9E9E9E, 40dp height, 16dp marginEnd
 *   - Save: @drawable/glass_background, #FFFFFF, 40dp height, 20dp paddingHorizontal
 */
@Composable
fun SettingsScreen(
    viewModel: DownloadViewModel,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val selectedStorageOption by viewModel.selectedStorageOption.collectAsState()
    val isUpdatingEngine by viewModel.isUpdatingEngine.collectAsState()
    val otaButtonLabel by viewModel.otaButtonLabel.collectAsState()
    val otaUpdateStatus by viewModel.otaUpdateStatus.collectAsState()
    val engineStatus by viewModel.engineStatus.collectAsState()
    val wifiOnly by viewModel.wifiOnly.collectAsState()
    val autoPlay by viewModel.autoPlayOnComplete.collectAsState()
    val turboSpeed by viewModel.turboSpeedEnabled.collectAsState()

    var tempWifiOnly by remember(wifiOnly) { mutableStateOf(wifiOnly) }
    var tempAutoPlay by remember(autoPlay) { mutableStateOf(autoPlay) }
    var tempTurboSpeed by remember(turboSpeed) { mutableStateOf(turboSpeed) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

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
            Toast.makeText(context, "Storage set to $folderName", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        // Main Dialog Card Container (layout_margin="16dp", background="@drawable/dialog_bg", padding="24dp")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF22232A),
                            Color(0xFF181920)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0x45FFFFFF),
                            Color(0x18FFFFFF)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TextView: ⚙️ Settings (textSize="22sp", textColor="#FFFFFF", textStyle="bold", layout_marginBottom="16dp")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ Settings",
                        fontSize = 22.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    NovaPill(
                        text = "NovaGet v2.4",
                        leadingIcon = NovaIcons.ElectricBolt,
                        iconTint = NovaAccentPurple
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // CheckBox: Wi-Fi Only (textSize="16sp", textColor="#E0E0E0")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { tempWifiOnly = !tempWifiOnly }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = tempWifiOnly,
                        onCheckedChange = { tempWifiOnly = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NovaAccentPurple,
                            uncheckedColor = Color(0xFF9E9E9E),
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download over Wi-Fi only",
                        fontSize = 16.sp,
                        color = Color(0xFFE0E0E0),
                        fontWeight = FontWeight.Medium
                    )
                }

                // CheckBox: Auto-play on complete (textSize="16sp", textColor="#E0E0E0")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { tempAutoPlay = !tempAutoPlay }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = tempAutoPlay,
                        onCheckedChange = { tempAutoPlay = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NovaAccentPurple,
                            uncheckedColor = Color(0xFF9E9E9E),
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auto-play video on complete",
                        fontSize = 16.sp,
                        color = Color(0xFFE0E0E0),
                        fontWeight = FontWeight.Medium
                    )
                }

                // CheckBox: 5x Turbo Speed Booster
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { tempTurboSpeed = !tempTurboSpeed }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = tempTurboSpeed,
                        onCheckedChange = { tempTurboSpeed = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NovaAccentPurple,
                            uncheckedColor = Color(0xFF9E9E9E),
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🚀 5x Download Speed Booster",
                                fontSize = 16.sp,
                                color = Color(0xFFE0E0E0),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x3010B981))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "TURBO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }
                        Text(
                            text = "Multi-threaded fragment downloading (8 parallel streams)",
                            fontSize = 12.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Instagram Private Downloads & Session Sync Card
                val isInstaLoggedIn = remember {
                    val cookies = ExtractionEngine.getInstagramCookies()
                    !cookies.isNullOrBlank() && (cookies.contains("sessionid") || cookies.contains("ds_user_id"))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x15FFFFFF))
                        .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NovaPillBackgroundBrush)
                                    .border(1.dp, NovaPillBorderBrush, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📸", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Instagram Private Downloads",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isInstaLoggedIn) "Synced & Active (Private Reels & Stories Unlocked)" else "Not logged in (Login in Browser to unlock)",
                                    color = if (isInstaLoggedIn) Color(0xFF34D399) else Color(0xFFE0E0E0),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isInstaLoggedIn) {
                                        Brush.linearGradient(listOf(Color(0x3510B981), Color(0x2010B981)))
                                    } else {
                                        NovaPillBackgroundBrush
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isInstaLoggedIn) {
                                        Brush.linearGradient(listOf(Color(0x8010B981), Color(0x5010B981)))
                                    } else {
                                        NovaPillBorderBrush
                                    },
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    viewModel.selectTab(NavTab.BROWSER)
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = if (isInstaLoggedIn) "Connected" else "Open Login",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Storage Location Row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x15FFFFFF))
                        .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NovaPillBackgroundBrush)
                                    .border(1.dp, NovaPillBorderBrush, RoundedCornerShape(10.dp)),
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
                                    text = "Storage Location",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = selectedStorageOption.file.absolutePath,
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NovaPillBackgroundBrush)
                                .border(1.dp, NovaPillBorderBrush, RoundedCornerShape(10.dp))
                                .clickable { folderPickerLauncher.launch(null) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Change",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Native Extraction Engine Row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x15FFFFFF))
                        .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NovaPillBackgroundBrush)
                                    .border(1.dp, NovaPillBorderBrush, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = NovaIcons.ElectricBolt,
                                    contentDescription = null,
                                    tint = NovaAccentPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Native Engine (CPU)",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = engineStatus ?: "yt-dlp (Python 3.12)",
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 12.sp
                                )
                                if (otaUpdateStatus != null) {
                                    val isStatusAlreadyUpdated = otaButtonLabel.contains("Already") || otaButtonLabel.contains("✓")
                                    Text(
                                        text = otaUpdateStatus ?: "",
                                        color = if (isStatusAlreadyUpdated) Color(0xFF00E676) else NovaAccentPurple,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val isAlreadyUpdated = otaButtonLabel.contains("Already") || otaButtonLabel.contains("✓")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NovaPillBackgroundBrush)
                                .border(
                                    1.dp,
                                    if (isAlreadyUpdated) androidx.compose.ui.graphics.SolidColor(Color(0xFF00E676)) else NovaPillBorderBrush,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable(enabled = !isUpdatingEngine) { viewModel.updateEngine(context) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            if (isUpdatingEngine) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = NovaAccentPurple,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isAlreadyUpdated) Icons.Default.Check else Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = if (isAlreadyUpdated) Color(0xFF00E676) else NovaAccentPurple,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isAlreadyUpdated) "Already updated" else "Update",
                                        color = if (isAlreadyUpdated) Color(0xFF00E676) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Privacy Policy Card Row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x15FFFFFF))
                        .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NovaPillBackgroundBrush)
                                    .border(1.dp, NovaPillBorderBrush, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🛡️", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Privacy Policy",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "100% On-Device • Zero Data Collection • No Tracking",
                                    color = Color(0xFF34D399),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NovaPillBackgroundBrush)
                                .border(1.dp, NovaPillBorderBrush, RoundedCornerShape(10.dp))
                                .clickable { showPrivacyPolicyDialog = true }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Read",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons Row: Cancel and Save (gravity="end", orientation="horizontal")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Button: Cancel (layout_height="40dp", layout_marginEnd="16dp", background="@android:color/transparent", textColor="#9E9E9E", text="Cancel")
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                tempWifiOnly = wifiOnly
                                tempAutoPlay = autoPlay
                                tempTurboSpeed = turboSpeed
                                Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF9E9E9E),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Button: Save (layout_height="40dp", background="@drawable/glass_background", textColor="#FFFFFF", paddingHorizontal="20dp", text="Save")
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x35FFFFFF),
                                        Color(0x18FFFFFF)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(0x50FFFFFF),
                                        Color(0x20FFFFFF)
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                viewModel.saveSettings(tempWifiOnly, tempAutoPlay, tempTurboSpeed)
                                Toast.makeText(context, "Settings Saved!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showPrivacyPolicyDialog) {
        Dialog(onDismissRequest = { showPrivacyPolicyDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E1F28))
                    .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛡️ Privacy Policy",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showPrivacyPolicyDialog = false }
                                .padding(4.dp)
                        ) {
                            Text("✕", fontSize = 16.sp, color = Color.White)
                        }
                    }

                    Text(
                        text = "NovaGet is designed with strict Zero-Knowledge & Zero-Telemetry architecture to protect user privacy.",
                        fontSize = 13.sp,
                        color = Color(0xFFD1D5DB)
                    )

                    PrivacyPolicySection(
                        title = "1. 100% On-Device Processing",
                        body = "NovaGet does not collect, transmit, proxy, or store any of your video/audio downloads or browsing history on external servers. All extraction (yt-dlp) and conversion (FFmpeg) runs 100% locally on your phone CPU."
                    )

                    PrivacyPolicySection(
                        title = "2. Zero Tracking & Telemetry",
                        body = "The app contains no third-party tracking SDKs, no advertising IDs, no device fingerprinting, and no background telemetry. Your activity remains private to you."
                    )

                    PrivacyPolicySection(
                        title = "3. Storage & Network Access",
                        body = "Storage access is used strictly to save media files to your selected directory. Network access is used solely to stream content directly from the media source URL you choose."
                    )

                    PrivacyPolicySection(
                        title = "4. Third-Party Websites & WebView",
                        body = "When using the built-in browser, you connect directly to third-party services (such as YouTube, Instagram). NovaGet blocks intrusive ad scripts and trackers, and does not log your credentials or session data."
                    )

                    PrivacyPolicySection(
                        title = "5. Legal Disclaimer & Fair Use",
                        body = "This software is provided for personal backup, offline research, and educational fair use only. Users are responsible for complying with the Terms of Service of respective content providers."
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NovaPillBackgroundBrush)
                            .border(1.dp, NovaPillBorderBrush, RoundedCornerShape(10.dp))
                            .clickable { showPrivacyPolicyDialog = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Understood",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyPolicySection(title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFA855F7)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = body,
            fontSize = 12.sp,
            color = Color(0xFF9CA3AF),
            lineHeight = 16.sp
        )
    }
}
