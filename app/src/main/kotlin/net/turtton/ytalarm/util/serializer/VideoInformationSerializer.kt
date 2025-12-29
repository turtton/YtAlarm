package net.turtton.ytalarm.util.serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.turtton.ytalarm.util.VideoInformation

object VideoInformationSerializer : KSerializer<VideoInformation> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = buildClassSerialDescriptor("VideoInformation") {
        element<String>("id")
        element<String?>("title", isOptional = true)
        element<String>("url")
        element<String>("domain")
        element<String>("type")
        element<String?>("fullTitle", isOptional = true)
        element<String?>("thumbnailUrl", isOptional = true)
        element<String?>("videoUrl", isOptional = true)
        element(
            "entries",
            LazySerialDescriptor {
                ListSerializer(VideoInformation.serializer())
            },
            isOptional = true
        )
    }

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

    // Related: https://github.com/Kotlin/kotlinx.serialization/issues/1815#issuecomment-1006816146
    @ExperimentalSerializationApi
    private class LazySerialDescriptor(lazySerializer: () -> KSerializer<*>) : SerialDescriptor {
        private val serializer by lazy(lazySerializer)

        private val original: SerialDescriptor by lazy {
            serializer.descriptor
        }

        override val serialName: String
            get() = original.serialName
        override val kind: SerialKind
            get() = original.kind
        override val elementsCount: Int
            get() = original.elementsCount

        override fun getElementName(index: Int): String = original.getElementName(index)
        override fun getElementIndex(name: String): Int = original.getElementIndex(name)
        override fun getElementAnnotations(index: Int): List<Annotation> =
            original.getElementAnnotations(index)
        override fun getElementDescriptor(index: Int): SerialDescriptor =
            original.getElementDescriptor(index)
        override fun isElementOptional(index: Int): Boolean = original.isElementOptional(index)
    }
}