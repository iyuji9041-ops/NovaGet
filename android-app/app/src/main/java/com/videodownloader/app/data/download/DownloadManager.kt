package com.videodownloader.app.data.download

import android.content.Context
import android.os.Environment
import android.util.Log
import com.videodownloader.app.data.local.DownloadRepository
import com.videodownloader.app.data.model.DownloadTaskState
import com.videodownloader.app.data.model.StorageOption
import com.videodownloader.app.data.model.VideoDownloadEntity
import com.videodownloader.app.data.model.VideoFormat
import com.videodownloader.app.engine.ExtractionEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive

class DownloadManager(
    private val context: Context,
    private val repository: DownloadRepository,
    private val scope: CoroutineScope
) {
    private val tag = "DownloadManager"
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val _liveTasks = MutableStateFlow<Map<Long, DownloadTaskState>>(emptyMap())
    val liveTasks: StateFlow<Map<Long, DownloadTaskState>> = _liveTasks.asStateFlow()

    private var customDirectory: File? = null

    fun getStandardStorageOptions(): List<StorageOption> {
        val list = mutableListOf<StorageOption>()

        // Public Downloads
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (publicDownloads != null) {
            val f = File(publicDownloads, "NovaGet").apply { mkdirs() }
            list.add(StorageOption("downloads", "Downloads", "Downloads/NovaGet", f))
        }

        // App Scoped Downloads
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
            val f = File(it, "NovaGet").apply { mkdirs() }
            if (list.none { opt -> opt.id == "downloads" }) {
                list.add(StorageOption("downloads", "Downloads", "Downloads/NovaGet", f))
            }
        }

        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.let {
            val f = File(it, "NovaGet").apply { mkdirs() }
            list.add(StorageOption("movies", "Movies", "Movies/NovaGet", f))
        }

        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.let {
            val f = File(it, "NovaGet").apply { mkdirs() }
            list.add(StorageOption("music", "Music / Audio", "Music/NovaGet", f))
        }

        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let {
            val f = File(it, "NovaGet").apply { mkdirs() }
            list.add(StorageOption("documents", "Documents", "Documents/NovaGet", f))
        }

        val internalDir = File(context.filesDir, "videos").apply { mkdirs() }
        list.add(StorageOption("internal", "App Storage", "Internal/videos", internalDir))

        return list
    }

    fun setDownloadDirectory(dir: File) {
        dir.mkdirs()
        customDirectory = dir
    }

    fun getDownloadDirectory(): File {
        return customDirectory ?: run {
            val firstOption = getStandardStorageOptions().firstOrNull()
            firstOption?.file ?: File(context.filesDir, "videos").apply { mkdirs() }
        }
    }

    fun startDownload(
        video: VideoDownloadEntity,
        formatId: String = "best",
        trimStart: String? = null,
        trimEnd: String? = null,
        splitChapters: Boolean = false,
        sponsorBlock: Boolean = false,
        embedSubs: Boolean = false,
        embedArtwork: Boolean = true,
        customArgs: String? = null,
        downloadArchive: File? = null,
        turboSpeed: Boolean = true
    ) {
        if (activeJobs.containsKey(video.id)) return

        val job = scope.launch(Dispatchers.IO) {
            DownloadForegroundService.start(context, video.title)
            val targetFile = File(video.localPath)
            targetFile.parentFile?.mkdirs()

            _liveTasks.update {
                it + (video.id to DownloadTaskState(
                    id = video.id,
                    downloadedBytes = 0L,
                    totalBytes = video.fileSize,
                    progress = 0f,
                    speedText = if (turboSpeed) "5x Turbo Starting..." else "Starting...",
                    etaSeconds = 0L
                ))
            }

            try {
                repository.updateStatus(video.id, VideoDownloadEntity.STATUS_DOWNLOADING)

                val isPlatformUrl = ExtractionEngine.isYouTubeUrl(video.url) ||
                        ExtractionEngine.isInstagramUrl(video.url) ||
                        video.url.contains("tiktok.com", ignoreCase = true) ||
                        video.url.contains("twitter.com", ignoreCase = true) ||
                        video.url.contains("x.com", ignoreCase = true) ||
                        video.url.contains("facebook.com", ignoreCase = true) ||
                        video.url.contains("fb.watch", ignoreCase = true) ||
                        video.url.contains("reddit.com", ignoreCase = true) ||
                        video.url.contains("vimeo.com", ignoreCase = true)

                val isAudio = video.mimeType.contains("audio", ignoreCase = true) ||
                        video.quality.contains("audio", ignoreCase = true) ||
                        video.localPath.endsWith(".mp3", ignoreCase = true) ||
                        video.localPath.endsWith(".m4a", ignoreCase = true) ||
                        video.localPath.endsWith(".opus", ignoreCase = true) ||
                        video.localPath.endsWith(".wav", ignoreCase = true)

                val audioExt = when {
                    video.localPath.endsWith(".m4a", ignoreCase = true) -> "m4a"
                    video.localPath.endsWith(".opus", ignoreCase = true) -> "opus"
                    video.localPath.endsWith(".wav", ignoreCase = true) -> "wav"
                    else -> "mp3"
                }

                if (isPlatformUrl) {
                    val outputDir = targetFile.parentFile ?: getDownloadDirectory()
                    val fmt = VideoFormat(
                        formatId = formatId,
                        quality = video.quality,
                        ext = if (isAudio) audioExt else "mp4",
                        filesize = if (video.fileSize > 0) video.fileSize else null
                    )

                    var lastDbUpdate = 0L
                    var maxProgressFraction = 0f
                    ExtractionEngine.download(
                        url = video.url,
                        format = fmt,
                        type = if (isAudio) "audio" else "video",
                        outputDir = outputDir,
                        title = video.title,
                        fileName = targetFile.name,
                        trimStart = trimStart,
                        trimEnd = trimEnd,
                        splitChapters = splitChapters,
                        sponsorBlock = sponsorBlock,
                        embedSubs = embedSubs,
                        embedArtwork = embedArtwork,
                        customArgs = customArgs,
                        downloadArchive = downloadArchive,
                        turboSpeed = turboSpeed
                    ) { progress, etaInSeconds, line ->
                        val speed = ExtractionEngine.parseSpeed(line)
                        val rawFraction = (progress / 100f).coerceIn(0f, 1f)
                        if (rawFraction > maxProgressFraction) {
                            maxProgressFraction = rawFraction
                        }
                        val safeFraction = maxProgressFraction
                        val currentDownloaded = if (video.fileSize > 0) {
                            (video.fileSize * safeFraction).toLong()
                        } else 0L

                        _liveTasks.update {
                            it + (video.id to DownloadTaskState(
                                id = video.id,
                                downloadedBytes = currentDownloaded,
                                totalBytes = video.fileSize,
                                progress = safeFraction,
                                speedText = if (speed.isNotBlank()) speed else "Downloading...",
                                etaSeconds = etaInSeconds
                            ))
                        }

                        val now = System.currentTimeMillis()
                        if (now - lastDbUpdate >= 600) {
                            lastDbUpdate = now
                            scope.launch {
                                repository.updateProgress(video.id, currentDownloaded, video.fileSize, VideoDownloadEntity.STATUS_DOWNLOADING)
                            }
                            DownloadForegroundService.update(
                                context,
                                video.title,
                                (safeFraction * 100).toInt(),
                                speed
                            )
                        }
                    }
                } else {
                    // Direct stream download via HttpURLConnection
                    downloadDirectStream(video, targetFile, turboSpeed)
                }

                // Check actual output file
                val actualFile = findActualDownloadedFile(targetFile, video.title)
                val finalSize = if (actualFile.exists() && actualFile.length() > 0) actualFile.length() else video.fileSize

                repository.updateProgress(video.id, finalSize, finalSize, VideoDownloadEntity.STATUS_COMPLETED)
                repository.update(
                    video.copy(
                        localPath = actualFile.absolutePath,
                        fileSize = finalSize,
                        downloadedBytes = finalSize,
                        status = VideoDownloadEntity.STATUS_COMPLETED
                    )
                )

                _liveTasks.update { it - video.id }

            } catch (e: CancellationException) {
                repository.updateStatus(video.id, VideoDownloadEntity.STATUS_PAUSED)
                _liveTasks.update { it - video.id }
            } catch (e: Exception) {
                Log.e(tag, "Download failed for ${video.url}", e)
                repository.updateStatus(video.id, VideoDownloadEntity.STATUS_FAILED)
                _liveTasks.update {
                    val current = it[video.id]
                    if (current != null) {
                        it + (video.id to current.copy(error = e.localizedMessage ?: "Download failed"))
                    } else it
                }
            } finally {
                activeJobs.remove(video.id)
                if (activeJobs.isEmpty()) {
                    DownloadForegroundService.stop(context)
                }
            }
        }

        activeJobs[video.id] = job
    }

    private suspend fun downloadDirectStream(video: VideoDownloadEntity, targetFile: File, turboSpeed: Boolean) {
        val numThreads = if (turboSpeed) 4 else 2
        val rangeSupport = checkRangeSupport(video.url)
        val totalLength = if (rangeSupport.totalLength > 0) rangeSupport.totalLength else video.fileSize

        // 1. Multi-Thread Check: file > 2 MB and server supports Range / Partial Content
        if (rangeSupport.supportsRange && totalLength > 2 * 1024 * 1024) {
            downloadMultiThreadedDirectStream(video, targetFile, totalLength, numThreads)
        } else {
            // 4. Safe Fallback to single-threaded FileOutputStream
            downloadSingleThreadDirectStream(video, targetFile, totalLength)
        }
    }

    private data class RangeSupportResult(val supportsRange: Boolean, val totalLength: Long)

    private fun checkRangeSupport(videoUrl: String): RangeSupportResult {
        return try {
            val conn = URL(videoUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "NovaGet/2.4 (Android; Mobile)")
            conn.setRequestProperty("Range", "bytes=0-1")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.connect()

            val responseCode = conn.responseCode
            val isPartial = (responseCode == 206)
            val contentRange = conn.getHeaderField("Content-Range")
            val totalFromRange = contentRange?.substringAfterLast('/', "")?.trim()?.toLongOrNull() ?: -1L
            val totalLen = if (totalFromRange > 0) totalFromRange else conn.contentLengthLong

            conn.disconnect()
            RangeSupportResult(isPartial, totalLen)
        } catch (e: Exception) {
            Log.w(tag, "Range support check failed: ${e.message}")
            RangeSupportResult(false, -1L)
        }
    }

    private suspend fun downloadMultiThreadedDirectStream(
        video: VideoDownloadEntity,
        targetFile: File,
        totalLength: Long,
        numThreads: Int
    ) = coroutineScope {
        targetFile.parentFile?.mkdirs()

        // 2. Pre-allocate file size using RandomAccessFile in "rw" mode (never "rwd")
        RandomAccessFile(targetFile, "rw").use { raf ->
            if (raf.length() < totalLength) {
                raf.setLength(totalLength)
            }
        }

        // Track parts progress for Pause / Resume
        val partsFile = File(targetFile.parentFile, "${targetFile.name}.parts")
        val savedPartOffsets = loadPartsProgress(partsFile, targetFile, numThreads)

        val chunkSize = (totalLength + numThreads - 1) / numThreads
        val totalDownloaded = AtomicLong(savedPartOffsets.sum())
        val bytesSinceLastUpdate = AtomicLong(0L)
        val lastUpdateMs = AtomicLong(System.currentTimeMillis())

        val currentPartProgress = Array(numThreads) { i -> AtomicLong(savedPartOffsets[i]) }

        val chunkJobs = (0 until numThreads).map { threadIndex ->
            val chunkStart = threadIndex * chunkSize
            val chunkEnd = minOf(chunkStart + chunkSize - 1, totalLength - 1)
            val alreadyDownloaded = currentPartProgress[threadIndex].get()
            val threadStart = chunkStart + alreadyDownloaded

            async(Dispatchers.IO) {
                if (threadStart > chunkEnd) {
                    return@async
                }

                var conn: HttpURLConnection? = null
                var inputStream: InputStream? = null
                var raf: RandomAccessFile? = null

                try {
                    val url = URL(video.url)
                    conn = url.openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "NovaGet/2.4 (Android; Mobile)")
                    conn.setRequestProperty("Range", "bytes=$threadStart-$chunkEnd")
                    conn.connectTimeout = 15000
                    conn.readTimeout = 30000
                    conn.connect()

                    val responseCode = conn.responseCode
                    if (responseCode != 206 && responseCode != 200) {
                        throw IllegalStateException("Worker $threadIndex received HTTP $responseCode")
                    }

                    inputStream = conn.inputStream
                    raf = RandomAccessFile(targetFile, "rw")
                    raf.seek(threadStart)

                    // 3. Optimal 32 KB OS-page aligned buffer
                    val buffer = ByteArray(32 * 1024)
                    var bytesRead = 0
                    var currentPosition = threadStart

                    while (currentPosition <= chunkEnd && inputStream.read(buffer).also { bytesRead = it } != -1) {
                        coroutineContext.ensureActive()

                        val toWrite = minOf(bytesRead.toLong(), chunkEnd - currentPosition + 1).toInt()
                        raf.write(buffer, 0, toWrite)
                        currentPosition += toWrite

                        val downloadedNow = totalDownloaded.addAndGet(toWrite.toLong())
                        bytesSinceLastUpdate.addAndGet(toWrite.toLong())
                        currentPartProgress[threadIndex].addAndGet(toWrite.toLong())

                        // Thread-safe 500ms progress gate
                        val now = System.currentTimeMillis()
                        val last = lastUpdateMs.get()
                        if (now - last >= 500) {
                            if (lastUpdateMs.compareAndSet(last, now)) {
                                val delta = bytesSinceLastUpdate.getAndSet(0L)
                                val speed = if (now > last) (delta * 1000) / (now - last) else 0L
                                val speedStr = DownloadTaskState.formatSpeed(speed)
                                val eta = if (speed > 0 && totalLength > downloadedNow) (totalLength - downloadedNow) / speed else 0L
                                val progress = if (totalLength > 0) (downloadedNow.toFloat() / totalLength).coerceIn(0f, 1f) else 0f

                                _liveTasks.update {
                                    it + (video.id to DownloadTaskState(
                                        id = video.id,
                                        downloadedBytes = downloadedNow,
                                        totalBytes = totalLength,
                                        progress = progress,
                                        speedText = speedStr,
                                        etaSeconds = eta
                                    ))
                                }

                                scope.launch {
                                    repository.updateProgress(video.id, downloadedNow, totalLength, VideoDownloadEntity.STATUS_DOWNLOADING)
                                }

                                DownloadForegroundService.update(
                                    context,
                                    video.title,
                                    (progress * 100).toInt(),
                                    speedStr
                                )

                                savePartsProgress(partsFile, currentPartProgress)
                            }
                        }
                    }
                } finally {
                    try { raf?.close() } catch (ignored: Exception) {}
                    try { inputStream?.close() } catch (ignored: Exception) {}
                    conn?.disconnect()
                }
            }
        }

        try {
            chunkJobs.awaitAll()
            if (partsFile.exists()) {
                partsFile.delete()
            }
        } catch (e: Exception) {
            savePartsProgress(partsFile, currentPartProgress)
            throw e
        }
    }

    private fun loadPartsProgress(partsFile: File, targetFile: File, numThreads: Int): LongArray {
        val result = LongArray(numThreads) { 0L }
        try {
            if (partsFile.exists()) {
                if (!targetFile.exists()) {
                    partsFile.delete()
                    return result
                }
                val lines = partsFile.readLines()
                if (lines.size == numThreads) {
                    for (i in 0 until numThreads) {
                        result[i] = lines[i].trim().toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                    }
                } else {
                    partsFile.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to load parts progress: ${e.message}")
        }
        return result
    }

    private fun savePartsProgress(partsFile: File, progress: Array<AtomicLong>) {
        try {
            val content = progress.joinToString("\n") { it.get().toString() }
            partsFile.writeText(content)
        } catch (ignored: Exception) {}
    }

    private fun downloadSingleThreadDirectStream(video: VideoDownloadEntity, targetFile: File, expectedTotalLength: Long) {
        var downloaded = if (targetFile.exists()) targetFile.length() else 0L
        val url = URL(video.url)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "NovaGet/2.4 (Android; Mobile)")
        conn.connectTimeout = 15000
        conn.readTimeout = 30000

        if (downloaded > 0) {
            conn.setRequestProperty("Range", "bytes=$downloaded-")
        }

        conn.connect()
        val responseCode = conn.responseCode
        val isAppend = (responseCode == 206)

        if (!isAppend && responseCode !in 200..299) {
            conn.disconnect()
            throw IllegalStateException("Server returned HTTP $responseCode")
        }

        if (!isAppend) {
            downloaded = 0L
            targetFile.delete()
        }

        val totalLength = if (conn.contentLengthLong > 0) {
            if (isAppend) downloaded + conn.contentLengthLong else conn.contentLengthLong
        } else if (expectedTotalLength > 0) expectedTotalLength else video.fileSize

        val inputStream: InputStream = conn.inputStream
        val outputStream = FileOutputStream(targetFile, isAppend)
        val buffer = ByteArray(32 * 1024) // 32 KB page-aligned buffer
        var bytesRead = 0
        var lastUpdate = System.currentTimeMillis()
        var bytesSinceLastUpdate = 0L

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                bytesSinceLastUpdate += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastUpdate >= 500) {
                    val speed = (bytesSinceLastUpdate * 1000) / (now - lastUpdate)
                    val speedStr = DownloadTaskState.formatSpeed(speed)
                    val eta = if (speed > 0 && totalLength > downloaded) (totalLength - downloaded) / speed else 0L

                    _liveTasks.update {
                        it + (video.id to DownloadTaskState(
                            id = video.id,
                            downloadedBytes = downloaded,
                            totalBytes = totalLength,
                            progress = if (totalLength > 0) (downloaded.toFloat() / totalLength).coerceIn(0f, 1f) else 0f,
                            speedText = speedStr,
                            etaSeconds = eta
                        ))
                    }

                    scope.launch {
                        repository.updateProgress(video.id, downloaded, totalLength, VideoDownloadEntity.STATUS_DOWNLOADING)
                    }

                    DownloadForegroundService.update(
                        context,
                        video.title,
                        if (totalLength > 0) ((downloaded.toFloat() / totalLength) * 100).toInt() else 0,
                        speedStr
                    )

                    lastUpdate = now
                    bytesSinceLastUpdate = 0L
                }
            }
            outputStream.flush()
        } finally {
            try { outputStream.close() } catch (ignored: Exception) {}
            try { inputStream.close() } catch (ignored: Exception) {}
            conn.disconnect()
        }
    }

    private fun findActualDownloadedFile(expectedFile: File, title: String): File {
        if (expectedFile.exists() && expectedFile.length() > 0) return expectedFile
        val parent = expectedFile.parentFile ?: return expectedFile
        val baseName = expectedFile.nameWithoutExtension
        val matchedBase = parent.listFiles()?.firstOrNull { it.nameWithoutExtension == baseName && it.length() > 0 }
        if (matchedBase != null) return matchedBase
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(30)
        val matched = parent.listFiles()?.firstOrNull { it.name.startsWith(safeTitle) && it.length() > 0 }
        return matched ?: expectedFile
    }

    fun pauseDownload(id: Long) {
        val job = activeJobs[id]
        if (job != null) {
            job.cancel()
            activeJobs.remove(id)
            scope.launch {
                repository.updateStatus(id, VideoDownloadEntity.STATUS_PAUSED)
            }
            _liveTasks.update { it - id }
        }
    }

    fun resumeDownload(id: Long) {
        scope.launch(Dispatchers.IO) {
            val entity = repository.getDownloadByIdOnce(id)
            if (entity != null) {
                startDownload(entity)
            }
        }
    }

    fun cancelDownload(id: Long) {
        val job = activeJobs[id]
        job?.cancel()
        activeJobs.remove(id)
        _liveTasks.update { it - id }
        scope.launch(Dispatchers.IO) {
            val entity = repository.getDownloadByIdOnce(id)
            if (entity != null) {
                val f = File(entity.localPath)
                if (f.exists()) f.delete()
                repository.deleteById(id)
            }
        }
    }
}
