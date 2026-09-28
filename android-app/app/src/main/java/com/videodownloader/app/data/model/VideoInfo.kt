package com.videodownloader.app.data.model

import com.google.gson.annotations.SerializedName

data class VideoInfo(
    @SerializedName("title") val title: String,
    @SerializedName("thumbnail") val thumbnail: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("formats") val formats: List<VideoFormat>,
    @SerializedName("uploader") val uploader: String? = null
)

data class VideoFormat(
    @SerializedName("format_id") val formatId: String,
    @SerializedName("quality") val quality: String,
    @SerializedName("ext") val ext: String,
    @SerializedName("filesize") val filesize: Long?,
    @SerializedName("resolution") val resolution: String = "",
    @SerializedName("fps") val fps: Int? = null,
    @SerializedName("vcodec") val vcodec: String? = null,
    @SerializedName("acodec") val acodec: String? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("tbr") val tbr: Double? = null,
    @SerializedName("abr") val abr: Double? = null,
    @SerializedName("is_audio_only") val isAudioOnly: Boolean = false,
    @SerializedName("is_suggested") val isSuggested: Boolean = false
)
