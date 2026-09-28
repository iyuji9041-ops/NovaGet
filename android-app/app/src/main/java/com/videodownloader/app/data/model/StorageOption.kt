package com.videodownloader.app.data.model

import java.io.File

data class StorageOption(
    val id: String,
    val name: String,
    val relativePath: String,
    val file: File
)
