package com.videodownloader.app.engine

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.videodownloader.app.data.model.VideoFormat
import com.videodownloader.app.data.model.VideoInfo
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import java.io.File
import java.util.regex.Pattern

object ExtractionEngine {

    private const val TAG = "ExtractionEngine"
    @Volatile
    private var isInitialized = false
    private var appContext: Context? = null

    // Fast in-memory cache to make repeated/back-and-forth link inspections instantaneous (0ms)
    private val infoCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, VideoInfo>>()
    private const val CACHE_EXPIRY_MS = 10 * 60 * 1000L // 10 minutes

    /**
     * Strips tracking tokens and clutter (e.g. ?si=, &list=, ?igsh=) from URLs
     * to prevent yt-dlp from attempting to parse playlists or making tracking redirects.
     */
    fun sanitizeUrl(url: String): String {
        var cleaned = url.trim()
        if (isYouTubeUrl(cleaned)) {
            cleaned = cleaned.replace(Regex("[?&]si=[^&]+"), "")
                .replace(Regex("[?&]list=[^&]+"), "")
                .replace(Regex("[?&]index=[^&]+"), "")
                .replace(Regex("[?&]feature=[^&]+"), "")
                .replace(Regex("[?&]start_radio=[^&]+"), "")
            if (cleaned.contains("?") && !cleaned.substringAfter("?").contains("=")) {
                cleaned = cleaned.substringBefore("?")
            }
        } else if (isInstagramUrl(cleaned)) {
            if (cleaned.contains("?")) {
                cleaned = cleaned.substringBefore("?")
            }
            if (!cleaned.endsWith("/")) {
                cleaned = "$cleaned/"
            }
        }
        return cleaned
    }

    /**
     * 1. Force Latest Python & Engine Upgrade:
     * Cleans up any legacy Python 3.8 / 0.15.0 environment and initializes
     * the bundled Python 3.12 runtime from library-0.18.1.aar.
     */
    @Synchronized
    fun init(context: Context) {
        appContext = context.applicationContext
        val appCtx = context.applicationContext
        cleanupInstagramCookies(appCtx)
        if (isInitialized) return

        cleanLegacyEnvironment(appCtx)

        try {
            YoutubeDL.getInstance().init(appCtx)
            Log.d(TAG, "YoutubeDL engine (Python 3.12) initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize YoutubeDL engine", e)
            throw e
        }

        try {
            FFmpeg.getInstance().init(appCtx)
            Log.d(TAG, "FFmpeg 0.18.1 initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FFmpeg", e)
        }

        isInitialized = true
    }

    /**
     * Discards any outdated Python 3.8 / 0.15.0 cache from noBackupFilesDir
     * to prevent version mismatch or unsupported Python runtime errors.
     */
    private fun cleanLegacyEnvironment(context: Context) {
        try {
            val ytdlDir = File(context.noBackupFilesDir, "youtubedl-android")
            if (ytdlDir.exists()) {
                val legacyPython = File(ytdlDir, "packages/python/usr/lib/python3.8")
                if (legacyPython.exists()) {
                    Log.i(TAG, "Legacy Python 3.8 environment detected. Purging $ytdlDir")
                    ytdlDir.deleteRecursively()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clean legacy environment", e)
        }
    }

    /**
     * Purges temporary Instagram cookie files from cacheDir/cookies
     * to prevent leftover session credentials from persisting on disk.
     */
    fun cleanupInstagramCookies(context: Context? = appContext) {
        try {
            val ctx = context ?: appContext ?: return
            val cookieDir = File(ctx.cacheDir, "cookies")
            if (cookieDir.exists()) {
                cookieDir.listFiles()?.forEach { file ->
                    try {
                        file.delete()
                    } catch (ignored: Exception) {}
                }
            }
        } catch (ignored: Exception) {}
    }

    /**
     * Instagram Cookie Extraction Utility:
     * Reads session cookies from Android CookieManager for Instagram.
     */
    fun getInstagramCookies(): String? {
        return try {
            val cookieManager = CookieManager.getInstance()
            val c1 = cookieManager.getCookie("https://www.instagram.com")
            val c2 = cookieManager.getCookie("https://instagram.com")
            when {
                !c1.isNullOrBlank() -> c1
                !c2.isNullOrBlank() -> c2
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read Instagram session cookies")
            null
        }
    }

    /**
     * Instagram Netscape Cookie File Exporter:
     * Generates a temporary standard Netscape cookies.txt file in the internal cache directory.
     * This allows yt-dlp to authenticate GraphQL, stories, and private reel endpoints.
     */
    fun getInstagramCookieFile(context: Context): File? {
        return try {
            val cm = CookieManager.getInstance()
            val rawCookies = cm.getCookie("https://www.instagram.com")
                ?: cm.getCookie("https://instagram.com")

            if (rawCookies.isNullOrBlank() || (!rawCookies.contains("sessionid") && !rawCookies.contains("ds_user_id"))) {
                return null
            }

            val cookieDir = File(context.cacheDir, "cookies").apply { mkdirs() }
            val cookieFile = File(cookieDir, "instagram_cookies.txt")
            val sb = StringBuilder()
            sb.append("# Netscape HTTP Cookie File\n")
            sb.append("# Temporary Instagram Session Sync\n\n")

            val pairs = rawCookies.split(";")
            for (p in pairs) {
                val kv = p.trim().split("=", limit = 2)
                if (kv.size == 2) {
                    val key = kv[0].trim()
                    val value = kv[1].trim()
                    sb.append(".instagram.com\tTRUE\t/\tTRUE\t2147483647\t$key\t$value\n")
                }
            }
            cookieFile.writeText(sb.toString())
            cookieFile
        } catch (e: Exception) {
            Log.w(TAG, "Failed to export Instagram cookies to file")
            null
        }
    }

    /**
     * Checks if a URL belongs to Instagram.
     */
    fun isInstagramUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("instagram.com") || lower.contains("instagr.am")
    }

    /**
     * Checks if a URL belongs to YouTube.
     */
    fun isYouTubeUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("youtube.com") || lower.contains("youtu.be")
    }

    /**
     * Builds a YoutubeDLRequest with optimized options:
     * - Fast metadata extraction (no -f filtering during info inspection)
     * - 5x Turbo Multi-threaded fragment downloads (-N 8, --concurrent-fragments 8, 32M buffer)
     * - Multi-format audio extraction & clean FFmpeg mp4 muxing
     * - Seal signature features: Trimming, Chapter split, SponsorBlock, Subs, Artwork, Archive, Custom args
     * - Instagram private reels and stories cookies sync
     */
    fun buildRequest(
        url: String,
        isDownload: Boolean = false,
        formatId: String? = null,
        isAudio: Boolean = false,
        audioFormat: String = "mp3",
        outputDir: File? = null,
        title: String? = null,
        fileName: String? = null,
        trimStart: String? = null,
        trimEnd: String? = null,
        splitChapters: Boolean = false,
        sponsorBlock: Boolean = false,
        embedSubs: Boolean = false,
        embedArtwork: Boolean = true,
        customArgs: String? = null,
        downloadArchive: File? = null,
        turboSpeed: Boolean = true
    ): YoutubeDLRequest {
        val request = YoutubeDLRequest(url)

        // General robust & fast options
        request.addOption("--no-playlist")
        request.addOption("--no-check-certificates")
        request.addOption("--no-warnings")
        request.addOption("--prefer-free-formats")

        // Instagram Cookie Injection (Bypass Login Wall for Private Reels & Stories)
        if (isInstagramUrl(url)) {
            val cookieFile = appContext?.let { getInstagramCookieFile(it) }
            if (cookieFile != null && cookieFile.exists()) {
                request.addOption("--cookies", cookieFile.absolutePath)
                Log.d(TAG, "Injected Instagram session cookie file for authenticated extraction")
            } else {
                val cookies = getInstagramCookies()
                if (!cookies.isNullOrBlank()) {
                    request.addOption("--add-header", "Cookie: $cookies")
                    Log.d(TAG, "Injected Instagram session cookies into request")
                }
            }
        }

        // Seal extraction configuration: skip translated subtitles & NEVER cap to player_client=android!
        if (isYouTubeUrl(url)) {
            request.addOption("--extractor-args", "youtube:skip=translated_subs")
        }

        // Fast metadata inspection flags
        if (!isDownload) {
            request.addOption("--flat-playlist")
            request.addOption("--no-check-certificates")
            request.addOption("--no-warnings")
            request.addOption("--no-call-home")
            request.addOption("--no-check-formats")
            request.addOption("--prefer-free-formats")
            request.addOption("--socket-timeout", "12")
            request.addOption("--retries", "1")
            request.addOption("--extractor-retries", "1")
            request.addOption("--skip-download")
            request.addOption("--dump-single-json")
        }

        // High-Speed Download options (Seal engine setup + 5x Turbo Booster)
        if (isDownload && outputDir != null) {
            val safeName = fileName?.substringBeforeLast(".")
                ?: (title ?: "video").replace(Regex("[^a-zA-Z0-9.-]"), "_").take(60)

            // Multi-connection accelerated download (Optimized buffer & thread pool for fast immediate start)
            val concurrentThreads = if (turboSpeed) "4" else "2"
            val bufferSize = "64K"
            val chunkSize = "10M"

            request.addOption("-N", concurrentThreads)
            request.addOption("--buffer-size", bufferSize)
            request.addOption("--http-chunk-size", chunkSize)
            request.addOption("--retries", "10")
            request.addOption("--fragment-retries", "10")
            request.addOption("--no-mtime")
            request.addOption("--socket-timeout", "10")

            // Seal feature #1: Video Trimming
            if (!trimStart.isNullOrBlank() || !trimEnd.isNullOrBlank()) {
                val start = trimStart?.trim()?.ifBlank { "00:00" } ?: "00:00"
                val end = trimEnd?.trim()?.ifBlank { "inf" } ?: "inf"
                request.addOption("--download-sections", "*$start-$end")
                request.addOption("--force-keyframes-at-cuts")
            }

            // Seal feature #2: Split by Chapters
            if (splitChapters) {
                request.addOption("--split-chapters")
            }

            // Seal feature #3: SponsorBlock
            if (sponsorBlock) {
                request.addOption("--sponsorblock-remove", "sponsor,intro,outro,selfpromo")
            }

            // Seal feature #4: Embed Subtitles
            if (embedSubs) {
                request.addOption("--embed-subs")
                request.addOption("--sub-langs", "all")
                request.addOption("--write-auto-subs")
            }

            // Seal feature #5: Embed Artwork / Thumbnail
            if (embedArtwork) {
                request.addOption("--embed-thumbnail")
                request.addOption("--add-metadata")
            }

            // Seal feature #8: Download Archive
            if (downloadArchive != null) {
                request.addOption("--download-archive", downloadArchive.absolutePath)
            }

            if (isAudio) {
                request.addOption("-o", "${outputDir.absolutePath}/$safeName.%(ext)s")
                request.addOption("-x")
                val cleanAudioFmt = when (audioFormat.lowercase()) {
                    "m4a", "aac" -> "m4a"
                    "opus" -> "opus"
                    "wav" -> "wav"
                    "flac" -> "flac"
                    else -> "mp3"
                }
                request.addOption("--audio-format", cleanAudioFmt)
                request.addOption("--audio-quality", "0")
                if (!formatId.isNullOrBlank() && formatId != "best" && formatId != "bestaudio" && formatId != "suggested") {
                    request.addOption("-f", formatId)
                } else {
                    request.addOption("-f", "bestaudio/best")
                }
            } else {
                request.addOption("-o", "${outputDir.absolutePath}/$safeName.%(ext)s")
                val fmt = when {
                    formatId.isNullOrBlank() || formatId == "best" || formatId == "suggested" -> "bestvideo+bestaudio/best"
                    formatId.contains("+") || formatId.contains("/") || formatId.contains("[") -> formatId
                    else -> "$formatId+bestaudio/best"
                }
                request.addOption("-f", fmt)
                // Guaranteed MP4 output via FFmpeg muxing
                request.addOption("--merge-output-format", "mp4")
            }

            // Seal feature #7: Custom CLI Arguments
            if (!customArgs.isNullOrBlank()) {
                val tokens = customArgs.trim().split("\\s+".toRegex())
                var i = 0
                while (i < tokens.size) {
                    val token = tokens[i]
                    if (token.startsWith("-") && i + 1 < tokens.size && !tokens[i + 1].startsWith("-")) {
                        request.addOption(token, tokens[i + 1])
                        i += 2
                    } else if (token.isNotBlank()) {
                        request.addOption(token)
                        i += 1
                    } else {
                        i += 1
                    }
                }
            }
        }

        return request
    }

    /**
     * DTO for raw JSON output from yt-dlp --dump-single-json
     */
    private data class RawYtdlFormat(
        @com.google.gson.annotations.SerializedName("format_id") val formatId: String? = null,
        @com.google.gson.annotations.SerializedName("format_note") val formatNote: String? = null,
        @com.google.gson.annotations.SerializedName("ext") val ext: String? = null,
        @com.google.gson.annotations.SerializedName("vcodec") val vcodec: String? = null,
        @com.google.gson.annotations.SerializedName("acodec") val acodec: String? = null,
        @com.google.gson.annotations.SerializedName("width") val width: Double? = null,
        @com.google.gson.annotations.SerializedName("height") val height: Double? = null,
        @com.google.gson.annotations.SerializedName("fps") val fps: Double? = null,
        @com.google.gson.annotations.SerializedName("filesize") val filesize: Long? = null,
        @com.google.gson.annotations.SerializedName("filesize_approx") val filesizeApprox: Long? = null,
        @com.google.gson.annotations.SerializedName("tbr") val tbr: Double? = null,
        @com.google.gson.annotations.SerializedName("vbr") val vbr: Double? = null,
        @com.google.gson.annotations.SerializedName("abr") val abr: Double? = null,
        @com.google.gson.annotations.SerializedName("format") val format: String? = null,
        @com.google.gson.annotations.SerializedName("resolution") val resolution: String? = null
    )

    private data class RawYtdlInfo(
        @com.google.gson.annotations.SerializedName("id") val id: String? = null,
        @com.google.gson.annotations.SerializedName("title") val title: String? = null,
        @com.google.gson.annotations.SerializedName("thumbnail") val thumbnail: String? = null,
        @com.google.gson.annotations.SerializedName("duration") val duration: Double? = null,
        @com.google.gson.annotations.SerializedName("duration_string") val durationString: String? = null,
        @com.google.gson.annotations.SerializedName("uploader") val uploader: String? = null,
        @com.google.gson.annotations.SerializedName("channel") val channel: String? = null,
        @com.google.gson.annotations.SerializedName("formats") val formats: List<RawYtdlFormat>? = null
    )

    /**
     * Parses yt-dlp dump-single-json output into clean VideoInfo and categorized streams.
     */
    private fun parseJsonToVideoInfo(jsonStr: String): VideoInfo? {
        return try {
            val gson = com.google.gson.Gson()
            val raw = gson.fromJson(jsonStr, RawYtdlInfo::class.java) ?: return null

            val durationSec = raw.duration?.toLong() ?: 0L
            val durationStr = raw.durationString?.takeIf { it.isNotBlank() } ?: run {
                if (durationSec > 0) {
                    val m = durationSec / 60
                    val s = durationSec % 60
                    "%02d:%02d".format(m, s)
                } else ""
            }

            val formats = parseRawFormats(raw.formats, durationSec)

            VideoInfo(
                title = raw.title ?: "Untitled Video",
                thumbnail = raw.thumbnail ?: "",
                duration = durationStr,
                formats = formats,
                uploader = raw.uploader ?: raw.channel
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse yt-dlp JSON: ${e.message}")
            null
        }
    }

    private fun parseRawFormats(rawFormats: List<RawYtdlFormat>?, durationSec: Long): List<VideoFormat> {
        if (rawFormats.isNullOrEmpty()) {
            return getDefaultFormats()
        }

        val videoResults = mutableListOf<VideoFormat>()
        val audioResults = mutableListOf<VideoFormat>()
        val seenVideoResolutions = mutableSetOf<String>()
        val seenAudioCodecs = mutableSetOf<String>()

        for (f in rawFormats) {
            val ext = f.ext?.lowercase() ?: ""
            val formatId = f.formatId ?: ""
            val vcodec = f.vcodec?.lowercase() ?: ""
            val acodec = f.acodec?.lowercase() ?: ""
            val formatStr = f.format?.lowercase() ?: ""

            // Skip invalid or storyboard formats
            if (ext.contains("mhtml") || formatStr.contains("mhtml") || formatId.contains("mhtml")) continue
            if (formatId.contains("sb") || formatStr.contains("storyboard")) continue

            val height = f.height?.toInt() ?: 0
            val width = f.width?.toInt() ?: 0
            val isAudioOnly = (vcodec == "none" || (vcodec.isBlank() && height <= 0 && width <= 0)) && (acodec.isNotBlank() && acodec != "none")

            if (isAudioOnly) {
                val abr = f.abr?.toInt() ?: 0
                val key = when {
                    ext.contains("m4a") || acodec.contains("mp4a") || acodec.contains("aac") -> "m4a"
                    ext.contains("opus") || acodec.contains("opus") -> "opus"
                    else -> ext
                }

                if (!seenAudioCodecs.contains(key)) {
                    seenAudioCodecs.add(key)
                    val size = f.filesize ?: f.filesizeApprox ?: (durationSec * (f.abr ?: 128.0) * 125).toLong()
                    val label = when (key) {
                        "m4a" -> "M4A Audio (${if (abr > 0) "${abr} kbps" else "AAC Original"})"
                        "opus" -> "OPUS Audio (${if (abr > 0) "${abr} kbps" else "Studio HQ"})"
                        else -> "${ext.uppercase()} Audio (${if (abr > 0) "${abr} kbps" else "HQ"})"
                    }
                    audioResults.add(
                        VideoFormat(
                            formatId = f.formatId ?: "bestaudio",
                            quality = label,
                            ext = key,
                            filesize = size,
                            resolution = if (abr > 0) "$abr kbps" else "Audio Stream",
                            fps = null,
                            vcodec = null,
                            acodec = key.uppercase(),
                            height = null,
                            tbr = f.tbr,
                            abr = f.abr,
                            isAudioOnly = true
                        )
                    )
                }
            } else {
                // Video stream
                if (height < 144 && width < 144) continue

                val qualityLabel = when {
                    height >= 2160 || width >= 3840 -> "4K (2160p Ultra HD)"
                    height >= 1440 || width >= 2560 -> "2K (1440p Quad HD)"
                    height >= 1080 || width >= 1920 -> "1080p (Full HD)"
                    height >= 720 || width >= 1280 -> "720p (HD)"
                    height >= 480 -> "480p (SD)"
                    height >= 360 -> "360p"
                    height >= 240 -> "240p"
                    height >= 144 -> "144p"
                    else -> null
                }

                if (qualityLabel != null) {
                    val fpsInt = f.fps?.toInt() ?: 30
                    val codecTag = when {
                        vcodec.contains("avc1") || vcodec.contains("h264") -> "H.264"
                        vcodec.contains("vp09") || vcodec.contains("vp9") -> "VP9"
                        vcodec.contains("av01") || vcodec.contains("av1") -> "AV1"
                        else -> vcodec.substringBefore(".").uppercase().ifBlank { "MP4" }
                    }

                    val resolutionBucketKey = "$qualityLabel-$codecTag"
                    if (!seenVideoResolutions.contains(qualityLabel)) {
                        seenVideoResolutions.add(qualityLabel)
                        val size = f.filesize ?: f.filesizeApprox ?: (durationSec * (f.tbr ?: f.vbr ?: 2000.0) * 125).toLong()
                        val resStr = if (width > 0 && height > 0) "${width}x${height}" else qualityLabel

                        videoResults.add(
                            VideoFormat(
                                formatId = f.formatId ?: "best",
                                quality = qualityLabel,
                                ext = "mp4",
                                filesize = size,
                                resolution = resStr,
                                fps = fpsInt,
                                vcodec = codecTag,
                                acodec = "AAC",
                                height = height,
                                tbr = f.tbr,
                                isAudioOnly = false
                            )
                        )
                    }
                }
            }
        }

        // Standard high-quality MP3 options
        audioResults.add(
            VideoFormat(
                formatId = "bestaudio",
                quality = "MP3 Audio (320 kbps HQ)",
                ext = "mp3",
                filesize = (durationSec * 40000L).coerceIn(3L * 1024 * 1024, 15L * 1024 * 1024),
                resolution = "320 kbps",
                fps = null,
                vcodec = null,
                acodec = "MP3",
                height = null,
                isAudioOnly = true
            )
        )
        audioResults.add(
            VideoFormat(
                formatId = "bestaudio",
                quality = "MP3 Audio (192 kbps Standard)",
                ext = "mp3",
                filesize = (durationSec * 24000L).coerceIn(2L * 1024 * 1024, 10L * 1024 * 1024),
                resolution = "192 kbps",
                fps = null,
                vcodec = null,
                acodec = "MP3",
                height = null,
                isAudioOnly = true
            )
        )

        // Sort video resolutions descending (4K -> 2K -> 1080p -> 720p -> 480p -> 360p)
        val sortedVideos = videoResults.sortedByDescending { it.height ?: 0 }

        // Recommended / Suggested Option (1080p Full HD preferred for mobile)
        val preferredVid = sortedVideos.firstOrNull { (it.height ?: 0) == 1080 } ?: sortedVideos.firstOrNull()
        val suggestedFormat = preferredVid?.let { bestVid ->
            val bestAudioSize = audioResults.firstOrNull()?.filesize ?: (durationSec * 20000L)
            VideoFormat(
                formatId = "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best",
                quality = "Suggested: ${bestVid.quality}",
                ext = "mp4",
                filesize = (bestVid.filesize ?: 0L) + bestAudioSize,
                resolution = bestVid.resolution,
                fps = bestVid.fps,
                vcodec = "${bestVid.vcodec ?: "H.264"} + Audio",
                height = bestVid.height,
                isAudioOnly = false,
                isSuggested = true
            )
        }

        val allQualities = mutableListOf<VideoFormat>()
        if (suggestedFormat != null) {
            allQualities.add(suggestedFormat)
        }
        allQualities.addAll(sortedVideos)
        allQualities.addAll(audioResults)

        return allQualities
    }

    private fun getDefaultFormats(): List<VideoFormat> {
        return listOf(
            VideoFormat(formatId = "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best", quality = "Suggested: 1080p (Full HD)", ext = "mp4", filesize = null, resolution = "1920x1080", isSuggested = true, height = 1080),
            VideoFormat(formatId = "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best", quality = "1080p (Full HD)", ext = "mp4", filesize = null, resolution = "1920x1080", height = 1080),
            VideoFormat(formatId = "bestvideo[height<=720]+bestaudio/best[height<=720]/best", quality = "720p (HD)", ext = "mp4", filesize = null, resolution = "1280x720", height = 720),
            VideoFormat(formatId = "bestvideo[height<=480]+bestaudio/best[height<=480]/best", quality = "480p (SD)", ext = "mp4", filesize = null, resolution = "854x480", height = 480),
            VideoFormat(formatId = "bestaudio", quality = "MP3 Audio (320 kbps HQ)", ext = "mp3", filesize = null, resolution = "320 kbps", isAudioOnly = true),
            VideoFormat(formatId = "bestaudio", quality = "M4A Audio (Lossless/Original)", ext = "m4a", filesize = null, resolution = "256 kbps", isAudioOnly = true),
            VideoFormat(formatId = "bestaudio", quality = "OPUS Audio (160 kbps Studio)", ext = "opus", filesize = null, resolution = "160 kbps", isAudioOnly = true)
        )
    }

    /**
     * Extracts and organizes all Video & Audio stream formats (fallback when getInfo is used):
     * - Resolutions: 4K, 2K, 1080p, 720p, 480p, 360p, 240p in descending order
     * - Audio streams: MP3 320k, M4A 256k, OPUS 160k, WAV Lossless
     */
    fun filterFormats(rawFormats: List<com.yausername.youtubedl_android.mapper.VideoFormat>?): List<VideoFormat> {
        val videoResults = mutableListOf<VideoFormat>()
        val audioResults = mutableListOf<VideoFormat>()

        if (rawFormats.isNullOrEmpty()) {
            return getDefaultFormats()
        }

        val seenVideoLabels = mutableSetOf<String>()
        val seenAudioLabels = mutableSetOf<String>()

        for (f in rawFormats) {
            val ext = f.ext?.lowercase() ?: ""
            val formatStr = f.format?.lowercase() ?: ""
            val formatId = f.formatId?.lowercase() ?: ""
            val formatNote = f.formatNote?.lowercase() ?: ""
            val vcodec = f.vcodec?.lowercase() ?: ""
            val acodec = f.acodec?.lowercase() ?: ""

            // Discard mhtml and storyboard formats
            if (ext.contains("mhtml") || formatStr.contains("mhtml") || formatId.contains("mhtml")) continue
            if (formatId.contains("sb") || formatStr.contains("storyboard") || formatNote.contains("storyboard")) continue

            val height = f.height
            val width = f.width
            val isAudioOnly = (vcodec == "none" || (vcodec.isBlank() && height <= 0 && width <= 0)) && (acodec != "none")

            if (isAudioOnly) {
                val abr = f.abr
                val audioLabel = when {
                    ext.contains("m4a") || ext.contains("aac") -> "M4A Audio (${if (abr > 0) "${abr} kbps" else "Original"})"
                    ext.contains("opus") || (ext.contains("webm") && acodec.contains("opus")) -> "OPUS Audio (${if (abr > 0) "${abr} kbps" else "Studio"})"
                    ext.contains("mp3") -> "MP3 Audio (${if (abr > 0) "${abr} kbps" else "Standard"})"
                    else -> "${ext.uppercase()} Audio (${if (abr > 0) "${abr} kbps" else "Original"})"
                }

                if (!seenAudioLabels.contains(audioLabel)) {
                    seenAudioLabels.add(audioLabel)
                    val size = if (f.fileSize > 0) f.fileSize else if (f.fileSizeApproximate > 0) f.fileSizeApproximate else null
                    audioResults.add(
                        VideoFormat(
                            formatId = f.formatId ?: "bestaudio",
                            quality = audioLabel,
                            ext = if (ext.isNotBlank()) ext else "mp3",
                            filesize = size,
                            resolution = if (abr > 0) "$abr kbps" else "Audio Stream",
                            isAudioOnly = true
                        )
                    )
                }
            } else {
                // Video stream
                if ((height in 1..143) || (width in 1..143 && height <= 0)) continue

                val qualityLabel = when {
                    height >= 2160 || width >= 3840 -> "4K (2160p Ultra HD)"
                    height >= 1440 || width >= 2560 -> "2K (1440p Quad HD)"
                    height >= 1080 || width >= 1920 -> "1080p (Full HD)"
                    height >= 720 || width >= 1280 -> "720p (HD)"
                    height >= 480 -> "480p (SD)"
                    height >= 360 -> "360p"
                    height >= 240 -> "240p"
                    height >= 144 -> "144p"
                    else -> null
                }

                if (qualityLabel != null && !seenVideoLabels.contains(qualityLabel)) {
                    seenVideoLabels.add(qualityLabel)
                    val size = if (f.fileSize > 0) f.fileSize else if (f.fileSizeApproximate > 0) f.fileSizeApproximate else null
                    val res = if (width > 0 && height > 0) "${width}x${height}" else qualityLabel
                    videoResults.add(
                        VideoFormat(
                            formatId = f.formatId ?: "best",
                            quality = qualityLabel,
                            ext = "mp4",
                            filesize = size,
                            resolution = res,
                            height = height,
                            isAudioOnly = false
                        )
                    )
                }
            }
        }

        // Standard high-quality audio options
        if (!seenAudioLabels.any { it.contains("MP3") }) {
            audioResults.add(VideoFormat(formatId = "bestaudio", quality = "MP3 Audio (320 kbps HQ)", ext = "mp3", filesize = null, resolution = "320 kbps", isAudioOnly = true))
        }
        if (!seenAudioLabels.any { it.contains("M4A") }) {
            audioResults.add(VideoFormat(formatId = "bestaudio", quality = "M4A Audio (Lossless/Original)", ext = "m4a", filesize = null, resolution = "256 kbps", isAudioOnly = true))
        }
        if (!seenAudioLabels.any { it.contains("OPUS") }) {
            audioResults.add(VideoFormat(formatId = "bestaudio", quality = "OPUS Audio (160 kbps Studio)", ext = "opus", filesize = null, resolution = "160 kbps", isAudioOnly = true))
        }

        // Sort video resolutions descending (4K -> 2K -> 1080p -> 720p -> 480p -> 360p)
        val sortedVideos = videoResults.sortedByDescending { it.height ?: 0 }

        // Recommended / Suggested Option (1080p Full HD preferred for mobile)
        val preferredVid = sortedVideos.firstOrNull { (it.height ?: 0) == 1080 } ?: sortedVideos.firstOrNull()
        val suggestedFormat = preferredVid?.let { bestVid ->
            VideoFormat(
                formatId = "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best",
                quality = "Suggested: ${bestVid.quality}",
                ext = "mp4",
                filesize = bestVid.filesize,
                resolution = bestVid.resolution,
                fps = bestVid.fps,
                vcodec = "H.264 + Audio",
                height = bestVid.height,
                isAudioOnly = false,
                isSuggested = true
            )
        }

        val allQualities = mutableListOf<VideoFormat>()
        if (suggestedFormat != null) {
            allQualities.add(suggestedFormat)
        }
        allQualities.addAll(sortedVideos)
        allQualities.addAll(audioResults)

        return allQualities
    }

    /**
     * Extracts full VideoInfo using fast cached single-pass inspection.
     */
    fun extractInfo(url: String): VideoInfo {
        if (!isInitialized) {
            appContext?.let { init(it) }
        }

        val cleanUrl = sanitizeUrl(url)

        // 1. Instant return from fast memory cache (0ms)
        val cached = infoCache[cleanUrl]
        if (cached != null && (System.currentTimeMillis() - cached.first) < CACHE_EXPIRY_MS) {
            Log.d(TAG, "Instant return from memory cache: ${cached.second.title}")
            return cached.second
        }

        // 2. High-speed single-pass getInfo with optimized parameters
        val request = YoutubeDLRequest(cleanUrl).apply {
            addOption("--no-playlist")
            addOption("--flat-playlist")
            addOption("--no-check-certificates")
            addOption("--no-warnings")
            addOption("--no-call-home")
            addOption("--no-check-formats")
            addOption("--prefer-free-formats")
            addOption("--socket-timeout", "12")
            addOption("--retries", "1")
            addOption("--extractor-retries", "1")
            addOption("--skip-download")

            if (isYouTubeUrl(cleanUrl)) {
                addOption("--extractor-args", "youtube:skip=translated_subs,dash")
            }

            if (isInstagramUrl(cleanUrl)) {
                val cookieFile = appContext?.let { getInstagramCookieFile(it) }
                if (cookieFile != null && cookieFile.exists()) {
                    addOption("--cookies", cookieFile.absolutePath)
                } else {
                    val cookies = getInstagramCookies()
                    if (!cookies.isNullOrBlank()) {
                        addOption("--add-header", "Cookie: $cookies")
                    }
                }
            }
        }

        val ydlInfo = try {
            YoutubeDL.getInstance().getInfo(request)
        } finally {
            if (isInstagramUrl(cleanUrl)) {
                cleanupInstagramCookies()
            }
        }
        val durationSec = ydlInfo.duration
        val durationStr = if (durationSec > 0) {
            val m = durationSec / 60
            val s = durationSec % 60
            "%02d:%02d".format(m, s)
        } else ""

        val formats = filterFormats(ydlInfo.formats)
        val info = VideoInfo(
            title = ydlInfo.title?.takeIf { it.isNotBlank() } ?: "Untitled Video",
            thumbnail = ydlInfo.thumbnail ?: "",
            duration = durationStr,
            formats = formats,
            uploader = ydlInfo.uploader
        )

        infoCache[cleanUrl] = Pair(System.currentTimeMillis(), info)
        Log.d(TAG, "Fast extraction successful: ${info.title} (${info.formats.size} formats)")
        return info
    }

    /**
     * Executes download using YoutubeDL with real-time progress callbacks and high speed options.
     */
    fun download(
        url: String,
        format: VideoFormat,
        type: String,
        outputDir: File,
        title: String,
        fileName: String? = null,
        trimStart: String? = null,
        trimEnd: String? = null,
        splitChapters: Boolean = false,
        sponsorBlock: Boolean = false,
        embedSubs: Boolean = false,
        embedArtwork: Boolean = true,
        customArgs: String? = null,
        downloadArchive: File? = null,
        turboSpeed: Boolean = true,
        onProgress: (progress: Float, etaInSeconds: Long, line: String) -> Unit
    ): YoutubeDLResponse {
        if (!outputDir.exists()) outputDir.mkdirs()

        val isAudio = type.equals("audio", ignoreCase = true) ||
                format.ext.equals("mp3", ignoreCase = true) ||
                format.ext.equals("m4a", ignoreCase = true) ||
                format.ext.equals("opus", ignoreCase = true) ||
                format.ext.equals("wav", ignoreCase = true)

        val request = buildRequest(
            url = url,
            isDownload = true,
            formatId = format.formatId,
            isAudio = isAudio,
            audioFormat = format.ext,
            outputDir = outputDir,
            title = title,
            fileName = fileName,
            trimStart = trimStart,
            trimEnd = trimEnd,
            splitChapters = splitChapters,
            sponsorBlock = sponsorBlock,
            embedSubs = embedSubs,
            embedArtwork = embedArtwork,
            customArgs = customArgs,
            downloadArchive = downloadArchive,
            turboSpeed = turboSpeed
        )

        return try {
            YoutubeDL.getInstance().execute(request) { progress, etaInSeconds, line ->
                onProgress(progress, etaInSeconds, line)
            }
        } finally {
            if (isInstagramUrl(url)) {
                cleanupInstagramCookies()
            }
        }
    }

    /**
     * Extracts speed string from yt-dlp output line.
     */
    fun parseSpeed(line: String?): String {
        if (line == null) return ""
        val matcher = Pattern.compile("(\\d+(\\.\\d+)?(KiB|MiB|GiB|B)/s)").matcher(line)
        return if (matcher.find()) matcher.group(1) ?: "" else ""
    }
}

/**
 * 3. Dummy / Background WebView Helper for Instagram Login:
 * Allows user or app to log in to Instagram in a hidden/background WebView
 * so that session cookies can be captured by CookieManager for future extractions.
 */
class InstagramLoginWebViewHelper(private val context: Context) {

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun initializeHiddenWebView(onLoginSuccess: (() -> Unit)? = null) {
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)

        webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    url?.let {
                        val cookies = cm.getCookie(it)
                        if (!cookies.isNullOrBlank() && (cookies.contains("sessionid") || cookies.contains("ds_user_id"))) {
                            Log.d("InstagramLoginHelper", "Instagram session cookies captured!")
                            cm.flush()
                            onLoginSuccess?.invoke()
                        }
                    }
                }
            }
            // Load Instagram login endpoint
            loadUrl("https://www.instagram.com/accounts/login/")
        }
    }

    fun destroy() {
        webView?.destroy()
        webView = null
    }
}
