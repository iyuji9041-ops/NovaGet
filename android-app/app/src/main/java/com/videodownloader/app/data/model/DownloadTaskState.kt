package com.videodownloader.app.data.model

data class DownloadTaskState(
    val id: Long,
    val speedBytesPerSec: Long = 0L,
    val speedText: String = "",
    val etaSeconds: Long = 0L,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val progress: Float = 0f,
    val isPaused: Boolean = false,
    val error: String? = null
) {
    val speedFormatted: String
        get() = if (speedText.isNotBlank()) speedText else formatSpeed(speedBytesPerSec)

    companion object {
        fun formatSpeed(bytesPerSec: Long): String {
            return when {
                bytesPerSec >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB/s", bytesPerSec.toFloat() / (1024 * 1024))
                bytesPerSec >= 1024 -> String.format(java.util.Locale.US, "%.0f KB/s", bytesPerSec.toFloat() / 1024)
                bytesPerSec > 0 -> "$bytesPerSec B/s"
                else -> "-- KB/s"
            }
        }

        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 * 1024 -> String.format(java.util.Locale.US, "%.2f GB", bytes.toFloat() / (1024 * 1024 * 1024))
                bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes.toFloat() / (1024 * 1024))
                bytes >= 1024 -> String.format(java.util.Locale.US, "%.0f KB", bytes.toFloat() / 1024)
                bytes > 0 -> "$bytes B"
                else -> "0 B"
            }
        }

        fun formatEta(seconds: Long): String {
            if (seconds <= 0 || seconds > 86400) return "--"
            val mins = seconds / 60
            val secs = seconds % 60
            return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
        }
    }
}
