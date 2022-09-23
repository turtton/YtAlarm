package net.turtton.ytalarm.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoInformation(
    val id: String,
    // available if type == video
    @SerialName("fulltitle")
    val fullTitle: String? = null,
    // available if type == video
    @SerialName("thumbnail")
    val thumbnailUrl: String? = null,
    @SerialName("webpage_url")
    val url: String,
    // available if type == video
    @SerialName("url")
    val videoUrl: String? = null,
    @SerialName("webpage_url_domain")
    val domain: String,
    // video or playlist
    @SerialName("_type")
    val type: String = "video",
    // available if type == playlist
    val entries: List<VideoInformation>? = null
)