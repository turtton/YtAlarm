package net.turtton.ytalarm.util

import kotlinx.serialization.Serializable
import net.turtton.ytalarm.util.serializer.VideoInformationSerializer

@Serializable(with = VideoInformationSerializer::class)
data class VideoInformation(
    val id: String,
    val title: String? = null,
    val url: String,
    val domain: String,
    val typeData: Type
) {
    sealed interface Type {
        data class Video(
            val fullTitle: String,
            val thumbnailUrl: String,
            val videoUrl: String
        ) : Type
        data class Playlist(
            val entries: List<VideoInformation>
        ) : Type
    }
}