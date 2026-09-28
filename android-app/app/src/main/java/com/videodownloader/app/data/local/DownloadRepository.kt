package com.videodownloader.app.data.local

import com.videodownloader.app.data.model.VideoDownloadEntity
import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val videoDao: VideoDao) {

    val allDownloads: Flow<List<VideoDownloadEntity>> = videoDao.getAllDownloads()

    fun getDownloadsByStatus(status: String): Flow<List<VideoDownloadEntity>> = videoDao.getDownloadsByStatus(status)

    fun getDownloadById(id: Long): Flow<VideoDownloadEntity?> = videoDao.getDownloadById(id)

    suspend fun getDownloadByIdOnce(id: Long): VideoDownloadEntity? = videoDao.getDownloadByIdOnce(id)

    suspend fun getDownloadByUrl(url: String): VideoDownloadEntity? = videoDao.getDownloadByUrl(url)

    suspend fun insert(video: VideoDownloadEntity): Long = videoDao.insert(video)

    suspend fun update(video: VideoDownloadEntity) = videoDao.update(video)

    suspend fun updateProgress(id: Long, downloaded: Long, total: Long, status: String) {
        videoDao.updateProgress(id, downloaded, total, status)
    }

    suspend fun updateStatus(id: Long, status: String) {
        videoDao.updateStatus(id, status)
    }

    suspend fun deleteById(id: Long) {
        videoDao.deleteById(id)
    }

    suspend fun clearAll() {
        videoDao.clearAll()
    }
}
