package com.videodownloader.app.data.download

import android.util.Log
import com.videodownloader.app.data.model.AnalyzedVideoInfo
import com.videodownloader.app.data.model.VideoQualityOption
import com.videodownloader.app.engine.ExtractionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale

class VideoInfoAnalyzer {

    private val tag = "VideoInfoAnalyzer"

    suspend fun analyzeUrl(url: String): Result<AnalyzedVideoInfo> = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid HTTP or HTTPS URL"))
        }

        try {
            val pathWithoutQuery = cleanUrl.substringBefore('?').lowercase()
            val isDirectMediaFile = pathWithoutQuery.endsWith(".mp4") ||
                    pathWithoutQuery.endsWith(".m4v") ||
                    pathWithoutQuery.endsWith(".webm") ||
                    pathWithoutQuery.endsWith(".mp3") ||
                    pathWithoutQuery.endsWith(".m4a") ||
                    pathWithoutQuery.endsWith(".mov") ||
                    pathWithoutQuery.endsWith(".mkv")

            // Try extraction engine (yt-dlp with Python 3.12 runtime) first for platforms and video streams
            if (!isDirectMediaFile) {
                try {
                    val info = ExtractionEngine.extractInfo(cleanUrl)
                    val uri = URI(cleanUrl)
                    val host = uri.host?.replace("www.", "") ?: "Video Stream"

                    val durationParts = info.duration.split(":")
                    val durationSec = if (durationParts.size == 2) {
                        (durationParts[0].toLongOrNull() ?: 0L) * 60 + (durationParts[1].toLongOrNull() ?: 0L)
                    } else if (durationParts.size == 3) {
                        (durationParts[0].toLongOrNull() ?: 0L) * 3600 + (durationParts[1].toLongOrNull() ?: 0L) * 60 + (durationParts[2].toLongOrNull() ?: 0L)
                    } else 0L

                    val options = mutableListOf<VideoQualityOption>()
                    for (fmt in info.formats) {
                        val isAudio = fmt.isAudioOnly ||
                                fmt.ext.equals("mp3", ignoreCase = true) ||
                                fmt.ext.equals("m4a", ignoreCase = true) ||
                                fmt.ext.equals("opus", ignoreCase = true) ||
                                fmt.ext.equals("wav", ignoreCase = true) ||
                                fmt.quality.contains("Audio", ignoreCase = true)

                        val resolution = if (fmt.resolution.isNotBlank()) {
                            fmt.resolution
                        } else when {
                            fmt.quality.contains("4K") -> "3840x2160"
                            fmt.quality.contains("2K") -> "2560x1440"
                            fmt.quality.contains("1080p") -> "1920x1080"
                            fmt.quality.contains("720p") -> "1280x720"
                            fmt.quality.contains("480p") -> "854x480"
                            fmt.quality.contains("360p") -> "640x360"
                            fmt.quality.contains("240p") -> "426x240"
                            fmt.quality.contains("144p") -> "256x144"
                            fmt.quality.contains("320") -> "320 kbps"
                            fmt.quality.contains("256") -> "256 kbps"
                            fmt.quality.contains("160") -> "160 kbps"
                            fmt.quality.contains("192") -> "192 kbps"
                            isAudio -> "Audio Stream"
                            else -> fmt.quality
                        }

                        val codecStr = fmt.vcodec ?: fmt.acodec ?: if (isAudio) fmt.ext.uppercase() else "H.264"
                        val formatDesc = if (fmt.isSuggested) {
                            "MP4 • Best Quality (Video + Audio)"
                        } else if (isAudio) {
                            "${fmt.ext.uppercase()} • $codecStr Studio Audio"
                        } else {
                            val fpsLabel = if (fmt.fps != null && fmt.fps > 30) " ${fmt.fps}fps" else ""
                            "MP4 • $codecStr$fpsLabel"
                        }

                        options.add(
                            VideoQualityOption(
                                label = fmt.quality,
                                resolution = resolution,
                                format = formatDesc,
                                estimatedBytes = if (fmt.filesize != null && fmt.filesize > 0) fmt.filesize else 0L,
                                isAudioOnly = isAudio,
                                formatId = fmt.formatId,
                                fps = fmt.fps,
                                codec = codecStr,
                                ext = fmt.ext,
                                isSuggested = fmt.isSuggested,
                                tbr = fmt.tbr
                            )
                        )
                    }

                    val normalizedOptions = normalizeOptionSizes(options, durationSec)

                    return@withContext Result.success(
                        AnalyzedVideoInfo(
                            originalUrl = cleanUrl,
                            title = info.title,
                            host = host,
                            mimeType = if (normalizedOptions.firstOrNull()?.isAudioOnly == true) "audio/mpeg" else "video/mp4",
                            detectedSize = normalizedOptions.firstOrNull()?.estimatedBytes ?: 0L,
                            durationSeconds = durationSec,
                            thumbnailUrl = info.thumbnail.ifBlank { null },
                            uploader = info.uploader,
                            qualityOptions = normalizedOptions
                        )
                    )
                } catch (e: Exception) {
                    Log.w(tag, "ExtractionEngine extraction failed, attempting HTTP inspection: ${e.message}")
                }
            }

            // Fallback for direct media links or HTTP servers
            val uri = URI(cleanUrl)
            val host = uri.host?.replace("www.", "") ?: "Web Stream"
            val path = uri.path ?: ""
            val rawName = path.substringAfterLast('/', "video_download.mp4")
            val baseName = if (rawName.contains('.')) rawName.substringBeforeLast('.') else rawName
            val cleanTitle = baseName.replace(Regex("[-_+]"), " ")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                .ifBlank { "Stream_${System.currentTimeMillis() % 10000}" }

            var contentLength = 0L
            var contentType = "video/mp4"

            try {
                val conn = URL(cleanUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "HEAD"
                conn.setRequestProperty("User-Agent", "NovaGet/2.4 (Android; Mobile)")
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.connect()
                if (conn.responseCode in 200..399) {
                    contentLength = conn.contentLengthLong.coerceAtLeast(0L)
                    contentType = conn.contentType ?: "video/mp4"
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.d(tag, "HEAD check failed: ${e.message}")
            }

            if (contentLength <= 0L) {
                contentLength = 28L * 1024 * 1024
            }

            val fallbackQualities = listOf(
                VideoQualityOption(
                    label = "Suggested: 1080p (Full HD)",
                    resolution = "1920x1080",
                    format = "MP4 • Best Quality (Video + Audio)",
                    estimatedBytes = contentLength,
                    isAudioOnly = false,
                    formatId = "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best",
                    isSuggested = true,
                    codec = "H.264 + Audio"
                ),
                VideoQualityOption(
                    label = "4K (2160p Ultra HD)",
                    resolution = "3840x2160",
                    format = "MP4 • Ultra HD",
                    estimatedBytes = (contentLength * 2.2).toLong(),
                    isAudioOnly = false,
                    formatId = "bestvideo[height<=2160]+bestaudio/best[height<=2160]/best",
                    codec = "H.264"
                ),
                VideoQualityOption(
                    label = "1080p (Full HD)",
                    resolution = "1920x1080",
                    format = "MP4 • High Quality",
                    estimatedBytes = contentLength,
                    isAudioOnly = false,
                    formatId = "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best",
                    codec = "H.264"
                ),
                VideoQualityOption(
                    label = "720p (HD)",
                    resolution = "1280x720",
                    format = "MP4 • Standard",
                    estimatedBytes = (contentLength * 0.65).toLong(),
                    isAudioOnly = false,
                    formatId = "bestvideo[height<=720]+bestaudio/best[height<=720]/best"
                ),
                VideoQualityOption(
                    label = "480p (SD)",
                    resolution = "854x480",
                    format = "MP4 • Compact",
                    estimatedBytes = (contentLength * 0.38).toLong(),
                    isAudioOnly = false,
                    formatId = "bestvideo[height<=480]+bestaudio/best[height<=480]/best"
                ),
                VideoQualityOption(
                    label = "MP3 Audio (320 kbps HQ)",
                    resolution = "320 kbps",
                    format = "MP3 • Studio Quality",
                    estimatedBytes = 6L * 1024 * 1024,
                    isAudioOnly = true,
                    formatId = "bestaudio"
                ),
                VideoQualityOption(
                    label = "M4A Audio (Lossless/Original)",
                    resolution = "256 kbps",
                    format = "M4A • Native AAC",
                    estimatedBytes = 4L * 1024 * 1024,
                    isAudioOnly = true,
                    formatId = "bestaudio"
                ),
                VideoQualityOption(
                    label = "OPUS Audio (160 kbps Studio)",
                    resolution = "160 kbps",
                    format = "OPUS • Modern Clear",
                    estimatedBytes = 3L * 1024 * 1024,
                    isAudioOnly = true,
                    formatId = "bestaudio"
                )
            )

            Result.success(
                AnalyzedVideoInfo(
                    originalUrl = cleanUrl,
                    title = cleanTitle,
                    host = host,
                    mimeType = contentType,
                    detectedSize = contentLength,
                    durationSeconds = 180L,
                    thumbnailUrl = null,
                    qualityOptions = fallbackQualities
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to analyze URL: $cleanUrl", e)
            Result.failure(e)
        }
    }

    private fun normalizeOptionSizes(
        rawOptions: List<VideoQualityOption>,
        durationSec: Long
    ): List<VideoQualityOption> {
        if (rawOptions.isEmpty()) return rawOptions

        fun getResolutionFactor(height: Int): Double = when {
            height >= 2160 -> 3.2
            height >= 1440 -> 1.8
            height >= 1080 -> 1.0
            height >= 720  -> 0.55
            height >= 480  -> 0.30
            height >= 360  -> 0.18
            height >= 240  -> 0.10
            else           -> 0.06
        }

        fun parseHeight(label: String): Int = when {
            label.contains("4K", ignoreCase = true) || label.contains("2160", ignoreCase = true) -> 2160
            label.contains("2K", ignoreCase = true) || label.contains("1440", ignoreCase = true) -> 1440
            label.contains("1080", ignoreCase = true) -> 1080
            label.contains("720", ignoreCase = true) -> 720
            label.contains("480", ignoreCase = true) -> 480
            label.contains("360", ignoreCase = true) -> 360
            label.contains("240", ignoreCase = true) -> 240
            label.contains("144", ignoreCase = true) -> 144
            else -> 720
        }

        val videoOptions = rawOptions.filter { !it.isAudioOnly && !it.isSuggested }
        val audioOptions = rawOptions.filter { it.isAudioOnly }

        // Find anchor from video options with known non-zero filesize
        val anchor = videoOptions.firstOrNull { it.estimatedBytes > 0 && parseHeight(it.label) == 1080 }
            ?: videoOptions.firstOrNull { it.estimatedBytes > 0 && parseHeight(it.label) == 720 }
            ?: videoOptions.firstOrNull { it.estimatedBytes > 0 && parseHeight(it.label) == 360 }
            ?: videoOptions.firstOrNull { it.estimatedBytes > 0 }

        val effectiveDuration = if (durationSec > 0) durationSec else 60L

        val base1080Size: Double = if (anchor != null && anchor.estimatedBytes > 0) {
            val h = parseHeight(anchor.label)
            anchor.estimatedBytes.toDouble() / getResolutionFactor(h)
        } else {
            // Assume ~3.2 Mbps for 1080p
            (effectiveDuration * 400_000L).coerceAtLeast(2L * 1024 * 1024).toDouble()
        }

        // Proportional video sizes
        val adjustedVideoMap = mutableMapOf<String, Long>()
        for (opt in videoOptions) {
            val h = parseHeight(opt.label)
            val factor = getResolutionFactor(h)
            val calculated = (base1080Size * factor).toLong().coerceAtLeast(150_000L)
            val finalSize = if (opt == anchor) opt.estimatedBytes else calculated
            adjustedVideoMap[opt.label] = finalSize
        }

        // Monotonic check: Higher resolution must ALWAYS be larger than lower resolution
        val sortedHeights = listOf(2160, 1440, 1080, 720, 480, 360, 240, 144)
        for (i in 0 until sortedHeights.size - 1) {
            val hHigh = sortedHeights[i]
            val hLow = sortedHeights[i + 1]
            val optHigh = videoOptions.firstOrNull { parseHeight(it.label) == hHigh }
            val optLow = videoOptions.firstOrNull { parseHeight(it.label) == hLow }
            if (optHigh != null && optLow != null) {
                val sizeHigh = adjustedVideoMap[optHigh.label] ?: 0L
                val sizeLow = adjustedVideoMap[optLow.label] ?: 0L
                if (sizeLow >= sizeHigh && sizeHigh > 0) {
                    val ratio = getResolutionFactor(hLow) / getResolutionFactor(hHigh)
                    adjustedVideoMap[optLow.label] = (sizeHigh * ratio).toLong().coerceAtLeast(100_000L)
                }
            }
        }

        // Audio sizes
        val adjustedAudioMap = mutableMapOf<String, Long>()
        for (opt in audioOptions) {
            val abr = when {
                opt.label.contains("320") -> 40_000L
                opt.label.contains("256") -> 32_000L
                opt.label.contains("192") -> 24_000L
                opt.label.contains("160") -> 20_000L
                else -> 16_000L
            }
            val estimatedAudio = (effectiveDuration * abr).coerceIn(300_000L, 25L * 1024 * 1024)
            val finalAudioSize = if (opt.estimatedBytes in 100_000L..25_000_000L) opt.estimatedBytes else estimatedAudio
            adjustedAudioMap[opt.label] = finalAudioSize
        }

        return rawOptions.map { opt ->
            when {
                opt.isSuggested -> {
                    val targetVideoOpt = videoOptions.firstOrNull { parseHeight(it.label) == 1080 }
                        ?: videoOptions.firstOrNull()
                    val videoBytes = targetVideoOpt?.let { adjustedVideoMap[it.label] } ?: base1080Size.toLong()
                    val audioBytes = audioOptions.firstOrNull()?.let { adjustedAudioMap[it.label] } ?: (effectiveDuration * 20_000L)
                    opt.copy(estimatedBytes = videoBytes + audioBytes)
                }
                opt.isAudioOnly -> {
                    opt.copy(estimatedBytes = adjustedAudioMap[opt.label] ?: opt.estimatedBytes)
                }
                else -> {
                    opt.copy(estimatedBytes = adjustedVideoMap[opt.label] ?: opt.estimatedBytes)
                }
            }
        }
    }
}
