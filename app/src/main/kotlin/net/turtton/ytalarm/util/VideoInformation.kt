package net.turtton.ytalarm.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoInformation(
    val id: String,
    @SerialName("fulltitle")
    val fullTitle: String,
    @SerialName("thumbnail")
    val thumbnailUrl: String,
    @SerialName("webpage_url")
    val url: String,
    @SerialName("url")
    val videoUrl: String,
    @SerialName("webpage_url_domain")
    val domain: String
)