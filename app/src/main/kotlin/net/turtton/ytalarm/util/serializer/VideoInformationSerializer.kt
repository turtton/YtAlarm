package net.turtton.ytalarm.util.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.turtton.ytalarm.util.VideoInformation

object VideoInformationSerializer : KSerializer<VideoInformation> {
    override val descriptor = VideoInformationSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): VideoInformation {
        val surrogate = decoder.decodeSerializableValue(VideoInformationSurrogate.serializer())
        val type = when (surrogate.type) {
            "video" -> VideoInformation.Type.Video(
                surrogate.fullTitle!!,
                surrogate.thumbnailUrl!!,
                surrogate.videoUrl!!
            )
            "playlist" -> VideoInformation.Type.Playlist(
                surrogate.entries!!
            )
            else -> error("Unknown type name ${surrogate.type}")
        }
        return VideoInformation(
            surrogate.id,
            surrogate.title,
            surrogate.url,
            surrogate.domain,
            type
        )
    }

    override fun serialize(encoder: Encoder, value: VideoInformation) {
        error("Not implemented")
    }

    @Serializable
    private data class VideoInformationSurrogate(
        val id: String,
        val title: String? = null,
        @SerialName("webpage_url")
        val url: String,
        @SerialName("webpage_url_domain")
        val domain: String,
        // video or playlist
        @SerialName("_type")
        val type: String = "video",
        /* available if type == video */
        @SerialName("fulltitle")
        val fullTitle: String? = null,
        @SerialName("thumbnail")
        val thumbnailUrl: String? = null,
        @SerialName("url")
        val videoUrl: String? = null,
        /* end */
        /* available if type == playlist */
        val entries: List<VideoInformation>? = null
        /* end */
    )
}