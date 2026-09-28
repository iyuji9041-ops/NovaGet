package com.videodownloader.app.data.model

data class VideoQualityOption(
    val label: String,
    val resolution: String,
    val format: String,
    val estimatedBytes: Long,
    val isAudioOnly: Boolean = false,
    val formatId: String = "best",
    val fps: Int? = null,
    val codec: String = "",
    val ext: String = "mp4",
    val isSuggested: Boolean = false,
    val tbr: Double? = null
) {
    val sizeFormatted: String
        get() = DownloadTaskState.formatBytes(estimatedBytes)
}

data class AnalyzedVideoInfo(
    val originalUrl: String,
    val title: String,
    val host: String,
    val mimeType: String,
    val detectedSize: Long,
    val durationSeconds: Long = 180L,
    val thumbnailUrl: String? = null,
    val uploader: String? = null,
    val qualityOptions: List<VideoQualityOption>
)
