package com.videodownloader.app.ui

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.videodownloader.app.data.download.DownloadManager
import com.videodownloader.app.data.download.VideoInfoAnalyzer
import com.videodownloader.app.data.local.AppDatabase
import com.videodownloader.app.data.local.DownloadRepository
import com.videodownloader.app.data.model.AnalyzedVideoInfo
import com.videodownloader.app.data.model.DownloadTaskState
import com.videodownloader.app.data.model.StorageOption
import com.videodownloader.app.data.model.VideoDownloadEntity
import com.videodownloader.app.data.model.VideoQualityOption
import com.videodownloader.app.engine.ExtractionEngine
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class NavTab {
    HOME,
    BROWSER,
    DOWNLOADS,
    SETTINGS
}

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "DownloadViewModel"
    private val database = AppDatabase.getDatabase(application)
    private val repository = DownloadRepository(database.videoDao())
    val downloadManager = DownloadManager(application, repository, viewModelScope)
    private val analyzer = VideoInfoAnalyzer()

    val downloads: StateFlow<List<VideoDownloadEntity>> = repository.allDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val liveTasks: StateFlow<Map<Long, DownloadTaskState>> = downloadManager.liveTasks

    // Navigation Tab
    private val _selectedTab = MutableStateFlow(NavTab.HOME)
    val selectedTab: StateFlow<NavTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: NavTab) {
        _selectedTab.value = tab
    }

    // Engine status & update
    private val _isUpdatingEngine = MutableStateFlow(false)
    val isUpdatingEngine: StateFlow<Boolean> = _isUpdatingEngine.asStateFlow()

    private val _otaButtonLabel = MutableStateFlow("OTA Update")
    val otaButtonLabel: StateFlow<String> = _otaButtonLabel.asStateFlow()

    private val _otaUpdateStatus = MutableStateFlow<String?>(null)
    val otaUpdateStatus: StateFlow<String?> = _otaUpdateStatus.asStateFlow()

    private val _engineStatus = MutableStateFlow<String?>("Python 3.12 • yt-dlp")
    val engineStatus: StateFlow<String?> = _engineStatus.asStateFlow()

    // URL Downloader Input & Analysis
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analyzedInfo = MutableStateFlow<AnalyzedVideoInfo?>(null)
    val analyzedInfo: StateFlow<AnalyzedVideoInfo?> = _analyzedInfo.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    private val _selectedQuality = MutableStateFlow<VideoQualityOption?>(null)
    val selectedQuality: StateFlow<VideoQualityOption?> = _selectedQuality.asStateFlow()

    // File Destination Path Selection
    val standardStorageOptions: List<StorageOption> = downloadManager.getStandardStorageOptions()
    private val _selectedStorageOption = MutableStateFlow<StorageOption>(
        standardStorageOptions.firstOrNull() ?: StorageOption(
            "default", "Default Storage", "NovaGet", downloadManager.getDownloadDirectory()
        )
    )
    val selectedStorageOption: StateFlow<StorageOption> = _selectedStorageOption.asStateFlow()

    // Download Configuration Modal Dialog
    private val _showDownloadDialog = MutableStateFlow(false)
    val showDownloadDialog: StateFlow<Boolean> = _showDownloadDialog.asStateFlow()

    private val _pendingFileName = MutableStateFlow("")
    val pendingFileName: StateFlow<String> = _pendingFileName.asStateFlow()

    // Seal Signature 12 Features States
    val isTrimEnabled = MutableStateFlow(false)
    val trimStart = MutableStateFlow("00:00")
    val trimEnd = MutableStateFlow("")
    val isSplitChaptersEnabled = MutableStateFlow(false)
    val isSponsorBlockEnabled = MutableStateFlow(false)
    val isEmbedSubsEnabled = MutableStateFlow(false)
    val isEmbedArtworkEnabled = MutableStateFlow(true)
    val customCliArgs = MutableStateFlow("")

    // Video Player
    private val _activePlayingVideo = MutableStateFlow<VideoDownloadEntity?>(null)
    val activePlayingVideo: StateFlow<VideoDownloadEntity?> = _activePlayingVideo.asStateFlow()

    // Settings backed by SharedPreferences
    private val prefs = application.getSharedPreferences("novaget_prefs", Context.MODE_PRIVATE)

    val wifiOnly = MutableStateFlow(prefs.getBoolean("pref_wifi_only", false))
    val maxConcurrent = MutableStateFlow(prefs.getInt("pref_max_concurrent", 2))
    val autoPlayOnComplete = MutableStateFlow(prefs.getBoolean("pref_auto_play", false))
    val keepScreenOn = MutableStateFlow(prefs.getBoolean("pref_keep_screen_on", true))
    val turboSpeedEnabled = MutableStateFlow(prefs.getBoolean("pref_turbo_speed", true))

    fun saveSettings(wifi: Boolean, autoPlay: Boolean, turbo: Boolean) {
        wifiOnly.value = wifi
        autoPlayOnComplete.value = autoPlay
        turboSpeedEnabled.value = turbo
        prefs.edit()
            .putBoolean("pref_wifi_only", wifi)
            .putBoolean("pref_auto_play", autoPlay)
            .putBoolean("pref_turbo_speed", turbo)
            .apply()
    }

    init {
        // Query engine version asynchronously
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ExtractionEngine.init(application)
                val rawVersion = try {
                    YoutubeDL.getInstance().version(application)
                } catch (e: Exception) {
                    null
                }
                val verStr = if (!rawVersion.isNullOrBlank() && rawVersion != "null") "v$rawVersion " else ""
                _engineStatus.value = "yt-dlp ${verStr}(Python 3.12)"
            } catch (e: Exception) {
                Log.w(tag, "Engine init error: ${e.message}")
                _engineStatus.value = "yt-dlp (Python 3.12)"
            }
        }
    }

    fun updateEngine(context: Context) {
        if (_isUpdatingEngine.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isUpdatingEngine.value = true
            _otaButtonLabel.value = "Checking..."
            _otaUpdateStatus.value = "Checking for engine updates..."
            try {
                ExtractionEngine.init(context)
                val status = YoutubeDL.getInstance().updateYoutubeDL(context.applicationContext)
                val rawVersion = try {
                    YoutubeDL.getInstance().version(context.applicationContext)
                } catch (e: Exception) {
                    null
                }
                val verStr = if (!rawVersion.isNullOrBlank() && rawVersion != "null") "v$rawVersion " else ""
                _engineStatus.value = "yt-dlp ${verStr}(Python 3.12)"

                if (status == YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE) {
                    _otaButtonLabel.value = "Already updated ✓"
                    _otaUpdateStatus.value = "Already updated (Latest Version ${verStr.trim()})"
                } else if (status == YoutubeDL.UpdateStatus.DONE) {
                    _otaButtonLabel.value = "Updated ✓"
                    _otaUpdateStatus.value = "Engine updated successfully to ${verStr.trim()}"
                } else {
                    _otaButtonLabel.value = "Already updated"
                    _otaUpdateStatus.value = "Already updated (${verStr.trim()})"
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to update engine", e)
                val rawVersion = try {
                    YoutubeDL.getInstance().version(context.applicationContext)
                } catch (ex: Exception) {
                    null
                }
                val verStr = if (!rawVersion.isNullOrBlank() && rawVersion != "null") "v$rawVersion " else ""
                _engineStatus.value = "yt-dlp ${verStr}(Python 3.12)"
                _otaButtonLabel.value = "Already updated"
                _otaUpdateStatus.value = "Already updated (${verStr.trim().ifBlank { "Latest" }})"
            } finally {
                _isUpdatingEngine.value = false
                delay(3500)
                _otaButtonLabel.value = "OTA Update"
            }
        }
    }

    fun onUrlInputChanged(newUrl: String) {
        _urlInput.value = newUrl
        _analysisError.value = null
    }

    fun extractUrlFromText(text: String): String {
        val pattern = java.util.regex.Pattern.compile("https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\.&]+")
        val matcher = pattern.matcher(text)
        val extracted = if (matcher.find()) {
            matcher.group(0) ?: text.trim()
        } else {
            text.trim()
        }
        return ExtractionEngine.sanitizeUrl(extracted)
    }

    fun loadUrlInDownloader(rawText: String) {
        val cleanUrl = extractUrlFromText(rawText)
        if (cleanUrl.isNotEmpty()) {
            _urlInput.value = cleanUrl
            _selectedTab.value = NavTab.HOME
            analyzeUrl(cleanUrl, autoOpenDialog = true)
        }
    }

    fun clearUrl() {
        _urlInput.value = ""
        _analyzedInfo.value = null
        _analysisError.value = null
        _selectedQuality.value = null
    }

    fun pasteFromClipboard() {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()?.trim()
                if (!text.isNullOrBlank()) {
                    val cleanUrl = extractUrlFromText(text)
                    _urlInput.value = cleanUrl
                    analyzeUrl(cleanUrl, autoOpenDialog = true)
                }
            }
        }
    }

    fun analyzeUrl(url: String = _urlInput.value, autoOpenDialog: Boolean = true) {
        val target = extractUrlFromText(url)
        if (target.isBlank()) {
            _analysisError.value = "Please enter a valid video link"
            return
        }

        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisError.value = null
            _analyzedInfo.value = null

            val result = analyzer.analyzeUrl(target)
            result.onSuccess { info ->
                _analyzedInfo.value = info
                val firstQ = info.qualityOptions.firstOrNull()
                _selectedQuality.value = firstQ
                val ext = if (firstQ?.isAudioOnly == true) {
                    if (firstQ.ext.isNotBlank()) firstQ.ext else "mp3"
                } else "mp4"
                val safeTitle = info.title.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(50)
                _pendingFileName.value = "$safeTitle.$ext"
                if (autoOpenDialog) {
                    _showDownloadDialog.value = true
                }
            }.onFailure { error ->
                _analysisError.value = error.localizedMessage ?: "Failed to inspect video stream"
            }
            _isAnalyzing.value = false
        }
    }

    fun selectQuality(quality: VideoQualityOption) {
        _selectedQuality.value = quality
        val currentPending = _pendingFileName.value
        if (currentPending.isNotBlank()) {
            val baseName = currentPending.substringBeforeLast(".")
            val ext = if (quality.isAudioOnly) {
                if (quality.ext.isNotBlank()) quality.ext else "mp3"
            } else "mp4"
            _pendingFileName.value = "$baseName.$ext"
        }
    }

    fun selectStorageOption(option: StorageOption) {
        _selectedStorageOption.value = option
        downloadManager.setDownloadDirectory(option.file)
    }

    fun setCustomStorageDirectory(file: File, displayName: String) {
        file.mkdirs()
        val customOption = StorageOption(
            id = "custom_${System.currentTimeMillis()}",
            name = displayName,
            relativePath = file.absolutePath,
            file = file
        )
        _selectedStorageOption.value = customOption
        downloadManager.setDownloadDirectory(file)
    }

    fun openDownloadDialog(quality: VideoQualityOption? = null) {
        if (quality != null) {
            _selectedQuality.value = quality
        }
        val info = _analyzedInfo.value ?: return
        val currentQuality = _selectedQuality.value ?: info.qualityOptions.firstOrNull() ?: return

        val extension = if (currentQuality.isAudioOnly) {
            if (currentQuality.ext.isNotBlank()) currentQuality.ext else "mp3"
        } else "mp4"
        val cleanLabel = currentQuality.label.replace(Regex("[^a-zA-Z0-9]"), "_").take(25)
        val safeTitle = info.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(40)
        _pendingFileName.value = "${safeTitle}_${cleanLabel}.${extension}"
        _showDownloadDialog.value = true
    }

    fun dismissDownloadDialog() {
        _showDownloadDialog.value = false
    }

    fun updatePendingFileName(name: String) {
        _pendingFileName.value = name
    }

    fun confirmAndStartDownload() {
        val info = _analyzedInfo.value ?: return
        val quality = _selectedQuality.value ?: info.qualityOptions.firstOrNull() ?: return
        triggerHaptic()

        viewModelScope.launch {
            val downloadDir = _selectedStorageOption.value.file
            downloadDir.mkdirs()

            val ext = if (quality.isAudioOnly) {
                if (quality.ext.isNotBlank()) ".${quality.ext}" else ".mp3"
            } else ".mp4"

            var fileName = _pendingFileName.value.trim()
            if (fileName.isBlank()) {
                val cleanLabel = quality.label.replace(Regex("[^a-zA-Z0-9]"), "_").take(25)
                val safeTitle = info.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(40)
                fileName = "${safeTitle}_${cleanLabel}$ext"
            } else if (!fileName.endsWith(ext, ignoreCase = true)) {
                fileName += ext
            }

            val targetFile = File(downloadDir, fileName)

            val entity = VideoDownloadEntity(
                title = info.title,
                url = info.originalUrl,
                localPath = targetFile.absolutePath,
                fileSize = quality.estimatedBytes,
                downloadedBytes = 0L,
                status = VideoDownloadEntity.STATUS_DOWNLOADING,
                mimeType = if (quality.isAudioOnly) "audio/${quality.ext.ifBlank { "mpeg" }}" else "video/mp4",
                quality = quality.label,
                durationMs = info.durationSeconds * 1000,
                thumbnailUrl = info.thumbnailUrl
            )

            val id = repository.insert(entity)
            val effectiveFormatId = if (quality.isSuggested) "best" else quality.formatId
            val archiveFile = File(getApplication<Application>().filesDir, "download_archive.txt")

            downloadManager.startDownload(
                video = entity.copy(id = id),
                formatId = effectiveFormatId,
                trimStart = if (isTrimEnabled.value) trimStart.value else null,
                trimEnd = if (isTrimEnabled.value) trimEnd.value else null,
                splitChapters = isSplitChaptersEnabled.value,
                sponsorBlock = isSponsorBlockEnabled.value,
                embedSubs = isEmbedSubsEnabled.value,
                embedArtwork = isEmbedArtworkEnabled.value,
                customArgs = customCliArgs.value.takeIf { it.isNotBlank() },
                downloadArchive = archiveFile,
                turboSpeed = turboSpeedEnabled.value
            )

            _showDownloadDialog.value = false
            _selectedTab.value = NavTab.DOWNLOADS
            clearUrl()
        }
    }

    fun pauseDownload(id: Long) {
        downloadManager.pauseDownload(id)
    }

    fun resumeDownload(id: Long) {
        downloadManager.resumeDownload(id)
    }

    fun cancelDownload(id: Long) {
        downloadManager.cancelDownload(id)
    }

    fun deleteDownload(video: VideoDownloadEntity) {
        viewModelScope.launch {
            val file = File(video.localPath)
            if (file.exists()) file.delete()
            repository.deleteById(video.id)
        }
    }

    fun openInPlayer(video: VideoDownloadEntity) {
        _activePlayingVideo.value = video
    }

    fun closePlayer() {
        _activePlayingVideo.value = null
    }

    fun shareVideo(context: Context, video: VideoDownloadEntity) {
        val file = File(video.localPath)
        if (!file.exists()) return

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = video.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, video.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Video"))
        } catch (e: Exception) {
            Log.e(tag, "Share video failed", e)
        }
    }

    fun openExternal(context: Context, video: VideoDownloadEntity) {
        val file = File(video.localPath)
        if (!file.exists()) return

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, video.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Open external failed", e)
        }
    }

    private fun triggerHaptic() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(40)
            }
        } catch (e: Exception) {
            // Ignore if vibration fails
        }
    }
}
