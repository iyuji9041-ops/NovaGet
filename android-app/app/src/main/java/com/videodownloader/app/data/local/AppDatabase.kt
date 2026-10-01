package com.videodownloader.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.videodownloader.app.data.model.VideoDownloadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppDatabase private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), VideoDao {

    private val dbScope = CoroutineScope(Dispatchers.IO)
    private val _downloadsFlow = MutableStateFlow<List<VideoDownloadEntity>>(emptyList())

    init {
        refreshFlow()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_URL TEXT NOT NULL,
                $COL_LOCAL_PATH TEXT NOT NULL,
                $COL_FILE_SIZE INTEGER NOT NULL,
                $COL_DOWNLOADED_BYTES INTEGER NOT NULL,
                $COL_STATUS TEXT NOT NULL,
                $COL_MIME_TYPE TEXT NOT NULL,
                $COL_QUALITY TEXT NOT NULL,
                $COL_DURATION_MS INTEGER NOT NULL,
                $COL_THUMBNAIL_URL TEXT,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Safe non-destructive migration: preserves user's download records across app updates
        onCreate(db)
    }

    private fun refreshFlow() {
        dbScope.launch {
            val list = queryAllFromDb()
            _downloadsFlow.value = list
        }
    }

    private fun queryAllFromDb(): List<VideoDownloadEntity> {
        val list = mutableListOf<VideoDownloadEntity>()
        val db = readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COL_TIMESTAMP DESC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToEntity(c))
            }
        }
        return list
    }

    private fun cursorToEntity(c: Cursor): VideoDownloadEntity {
        return VideoDownloadEntity(
            id = c.getLong(c.getColumnIndexOrThrow(COL_ID)),
            title = c.getString(c.getColumnIndexOrThrow(COL_TITLE)),
            url = c.getString(c.getColumnIndexOrThrow(COL_URL)),
            localPath = c.getString(c.getColumnIndexOrThrow(COL_LOCAL_PATH)),
            fileSize = c.getLong(c.getColumnIndexOrThrow(COL_FILE_SIZE)),
            downloadedBytes = c.getLong(c.getColumnIndexOrThrow(COL_DOWNLOADED_BYTES)),
            status = c.getString(c.getColumnIndexOrThrow(COL_STATUS)),
            mimeType = c.getString(c.getColumnIndexOrThrow(COL_MIME_TYPE)),
            quality = c.getString(c.getColumnIndexOrThrow(COL_QUALITY)),
            durationMs = c.getLong(c.getColumnIndexOrThrow(COL_DURATION_MS)),
            thumbnailUrl = c.getString(c.getColumnIndexOrThrow(COL_THUMBNAIL_URL)),
            timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP))
        )
    }

    override fun getAllDownloads(): Flow<List<VideoDownloadEntity>> = _downloadsFlow.asStateFlow()

    override fun getDownloadsByStatus(status: String): Flow<List<VideoDownloadEntity>> {
        return _downloadsFlow.map { list -> list.filter { it.status == status } }
    }

    override fun getDownloadById(id: Long): Flow<VideoDownloadEntity?> {
        return _downloadsFlow.map { list -> list.find { it.id == id } }
    }

    override suspend fun getDownloadByIdOnce(id: Long): VideoDownloadEntity? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME WHERE $COL_ID = ? LIMIT 1", arrayOf(id.toString()))
        cursor.use { c ->
            if (c.moveToFirst()) cursorToEntity(c) else null
        }
    }

    override suspend fun getDownloadByUrl(url: String): VideoDownloadEntity? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME WHERE $COL_URL = ? LIMIT 1", arrayOf(url))
        cursor.use { c ->
            if (c.moveToFirst()) cursorToEntity(c) else null
        }
    }

    override suspend fun insert(video: VideoDownloadEntity): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, video.title)
            put(COL_URL, video.url)
            put(COL_LOCAL_PATH, video.localPath)
            put(COL_FILE_SIZE, video.fileSize)
            put(COL_DOWNLOADED_BYTES, video.downloadedBytes)
            put(COL_STATUS, video.status)
            put(COL_MIME_TYPE, video.mimeType)
            put(COL_QUALITY, video.quality)
            put(COL_DURATION_MS, video.durationMs)
            put(COL_THUMBNAIL_URL, video.thumbnailUrl)
            put(COL_TIMESTAMP, video.timestamp)
        }
        val id = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshFlow()
        id
    }

    override suspend fun update(video: VideoDownloadEntity) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, video.title)
            put(COL_URL, video.url)
            put(COL_LOCAL_PATH, video.localPath)
            put(COL_FILE_SIZE, video.fileSize)
            put(COL_DOWNLOADED_BYTES, video.downloadedBytes)
            put(COL_STATUS, video.status)
            put(COL_MIME_TYPE, video.mimeType)
            put(COL_QUALITY, video.quality)
            put(COL_DURATION_MS, video.durationMs)
            put(COL_THUMBNAIL_URL, video.thumbnailUrl)
            put(COL_TIMESTAMP, video.timestamp)
        }
        db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(video.id.toString()))
        refreshFlow()
    }

    override suspend fun updateProgress(id: Long, downloaded: Long, total: Long, status: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_DOWNLOADED_BYTES, downloaded)
            if (total > 0) put(COL_FILE_SIZE, total)
            put(COL_STATUS, status)
        }
        db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(id.toString()))
        refreshFlow()
    }

    override suspend fun updateStatus(id: Long, status: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_STATUS, status)
        }
        db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(id.toString()))
        refreshFlow()
    }

    override suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(id.toString()))
        refreshFlow()
    }

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_NAME, null, null)
        refreshFlow()
    }

    fun videoDao(): VideoDao = this

    companion object {
        private const val DATABASE_NAME = "zaswix_database.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "video_downloads"

        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_URL = "url"
        private const val COL_LOCAL_PATH = "localPath"
        private const val COL_FILE_SIZE = "fileSize"
        private const val COL_DOWNLOADED_BYTES = "downloadedBytes"
        private const val COL_STATUS = "status"
        private const val COL_MIME_TYPE = "mimeType"
        private const val COL_QUALITY = "quality"
        private const val COL_DURATION_MS = "durationMs"
        private const val COL_THUMBNAIL_URL = "thumbnailUrl"
        private const val COL_TIMESTAMP = "timestamp"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
