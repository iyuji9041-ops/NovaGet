package com.videodownloader.app.data.model

data class VideoDownloadEntity(
    val id: Long = 0,
    val title: String,
    val url: String,
    val localPath: String,
    val fileSize: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: String = STATUS_DOWNLOADING,
    val mimeType: String = "video/mp4",
    val quality: String = "1080p",
    val durationMs: Long = 0L,
    val thumbnailUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (fileSize > 0) (downloadedBytes.toFloat() / fileSize.toFloat()).coerceIn(0f, 1f) else 0f

    val isCompleted: Boolean
        get() = status == STATUS_COMPLETED

    val isDownloading: Boolean
        get() = status == STATUS_DOWNLOADING

    val isPaused: Boolean
        get() = status == STATUS_PAUSED

    companion object {
        const val STATUS_DOWNLOADING = "DOWNLOADING"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
    }
}
