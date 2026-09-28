package com.videodownloader.app.data.model

data class DownloadTask(
    val id: String,
    val title: String,
    val url: String,
    val formatId: String,
    val type: String,
    var status: DownloadStatus,
    var progress: Int,
    var speed: String,
    var eta: String,
    val timestamp: Long
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}
