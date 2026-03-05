package net.turtton.ytalarm.datasource.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.turtton.ytalarm.kernel.dto.VideoInformation

/**
 * yt-dlp の JSON 出力を [VideoInformation] にデシリアライズするカスタム Serializer。
 *
 * yt-dlp は `_type` フィールドで "video" / "playlist" を区別する。
 * video の場合: fulltitle, thumbnail, url (videoUrl) を取り出す。
 * playlist の場合: entries (List<VideoInformation>) を取り出す。
 *
 * `entries` 内の要素も [VideoInformation] であるため、再帰的にこの Serializer を使う。
 */
object VideoInformationSerializer : KSerializer<VideoInformation> {
    override val descriptor: SerialDescriptor by lazy {
        VideoInformationSurrogate.serializer().descriptor
    }

    override fun deserialize(decoder: Decoder): VideoInformation {
        // JSON ベースのデシリアライズ。再帰的な entries 処理のために JsonDecoder を使用する。
        require(decoder is JsonDecoder) {
            "VideoInformationSerializer can only be used with JSON format"
        }
        val element = decoder.decodeJsonElement().jsonObject
        return decodeFromJsonObject(element, decoder)
    }

    private fun decodeFromJsonObject(obj: JsonObject, decoder: JsonDecoder): VideoInformation {
        val id = obj["id"]!!.jsonPrimitive.content
        val title = obj["title"]?.takeIf { it != JsonNull }?.jsonPrimitive?.content
        val url = obj["webpage_url"]!!.jsonPrimitive.content
        val domain = obj["webpage_url_domain"]!!.jsonPrimitive.content
        val type = obj["_type"]?.jsonPrimitive?.content ?: "video"

        val typeData: VideoInformation.Type = when (type) {
            "video" -> {
                val fullTitle = obj["fulltitle"]?.takeIf { it != JsonNull }?.jsonPrimitive?.content
                    ?: title
                    ?: id
                val thumbnailUrl =
                    obj["thumbnail"]?.takeIf { it != JsonNull }?.jsonPrimitive?.content ?: ""
                val videoUrl =
                    obj["url"]?.takeIf { it != JsonNull }?.jsonPrimitive?.content ?: url
                VideoInformation.Type.Video(
                    fullTitle = fullTitle,
                    thumbnailUrl = thumbnailUrl,
                    videoUrl = videoUrl
                )
            }

            "playlist" -> {
                val entriesJson: JsonArray =
                    obj["entries"]?.takeIf { it != JsonNull }?.jsonArray ?: JsonArray(emptyList())
                val entries = entriesJson.map { entryElement ->
                    decodeFromJsonObject(entryElement.jsonObject, decoder)
                }
                VideoInformation.Type.Playlist(entries = entries)
            }

            else -> error("Unknown _type value: $type")
        }

        return VideoInformation(
            id = id,
            title = title,
            url = url,
            domain = domain,
            typeData = typeData
        )
    }

    override fun serialize(encoder: Encoder, value: VideoInformation): Unit =
        throw UnsupportedOperationException(
            "VideoInformationSerializer does not support serialization"
        )

    @Serializable
    @Suppress("UnusedPrivateClass")
    private data class VideoInformationSurrogate(
        val id: String,
        val title: String? = null,
        @SerialName("webpage_url")
        val url: String,
        @SerialName("webpage_url_domain")
        val domain: String,
        @SerialName("_type")
        val type: String = "video",
        @SerialName("fulltitle")
        val fullTitle: String? = null,
        @SerialName("thumbnail")
        val thumbnailUrl: String? = null,
        @SerialName("url")
        val videoUrl: String? = null,
        val entries: List<VideoInformationSurrogate>? = null
    )
}