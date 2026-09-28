package com.videodownloader.app.data.local

import com.videodownloader.app.data.model.VideoDownloadEntity
import kotlinx.coroutines.flow.Flow

interface VideoDao {
    fun getAllDownloads(): Flow<List<VideoDownloadEntity>>
    fun getDownloadsByStatus(status: String): Flow<List<VideoDownloadEntity>>
    fun getDownloadById(id: Long): Flow<VideoDownloadEntity?>
    suspend fun getDownloadByIdOnce(id: Long): VideoDownloadEntity?
    suspend fun getDownloadByUrl(url: String): VideoDownloadEntity?
    suspend fun insert(video: VideoDownloadEntity): Long
    suspend fun update(video: VideoDownloadEntity)
    suspend fun updateProgress(id: Long, downloaded: Long, total: Long, status: String)
    suspend fun updateStatus(id: Long, status: String)
    suspend fun deleteById(id: Long)
    suspend fun clearAll()
}
