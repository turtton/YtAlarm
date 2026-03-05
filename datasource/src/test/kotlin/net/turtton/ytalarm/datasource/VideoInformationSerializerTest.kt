package net.turtton.ytalarm.datasource

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import net.turtton.ytalarm.datasource.serializer.VideoInformationSerializer
import net.turtton.ytalarm.kernel.dto.VideoInformation

class VideoInformationSerializerTest :
    FunSpec({
        val json = Json {
            ignoreUnknownKeys = true
            serializersModule = kotlinx.serialization.modules.SerializersModule {
                contextual(VideoInformation::class, VideoInformationSerializer)
            }
        }

        context("decode video type") {
            val rawVideoJson = """
            {
                "id": "abc",
                "title": "title",
                "webpage_url": "example.com/abc",
                "webpage_url_domain": "example.com",
                "_type": "video",
                "fulltitle": "fullTitle",
                "thumbnail": "thumbnail.example.com/abc",
                "url": "video.example.com/abc"
            }
            """.trimIndent()

            test("decodes video correctly") {
                val decoded = json.decodeFromString(VideoInformationSerializer, rawVideoJson)
                decoded.id shouldBe "abc"
                decoded.title shouldBe "title"
                decoded.url shouldBe "example.com/abc"
                decoded.domain shouldBe "example.com"
                val typeData = decoded.typeData as VideoInformation.Type.Video
                typeData.fullTitle shouldBe "fullTitle"
                typeData.thumbnailUrl shouldBe "thumbnail.example.com/abc"
                typeData.videoUrl shouldBe "video.example.com/abc"
            }

            test("uses id as fullTitle when fulltitle and title are null") {
                val rawJson = """
                {
                    "id": "fallback_id",
                    "webpage_url": "example.com/fallback",
                    "webpage_url_domain": "example.com",
                    "_type": "video",
                    "url": "video.example.com/fallback"
                }
                """.trimIndent()
                val decoded = json.decodeFromString(VideoInformationSerializer, rawJson)
                val typeData = decoded.typeData as VideoInformation.Type.Video
                typeData.fullTitle shouldBe "fallback_id"
            }

            test("uses title as fullTitle when fulltitle is null but title exists") {
                val rawJson = """
                {
                    "id": "some_id",
                    "title": "some title",
                    "webpage_url": "example.com/some",
                    "webpage_url_domain": "example.com",
                    "_type": "video",
                    "url": "video.example.com/some"
                }
                """.trimIndent()
                val decoded = json.decodeFromString(VideoInformationSerializer, rawJson)
                val typeData = decoded.typeData as VideoInformation.Type.Video
                typeData.fullTitle shouldBe "some title"
            }

            test("thumbnail defaults to empty string when missing") {
                val rawJson = """
                {
                    "id": "no_thumb",
                    "webpage_url": "example.com/no_thumb",
                    "webpage_url_domain": "example.com",
                    "_type": "video",
                    "url": "video.example.com/no_thumb"
                }
                """.trimIndent()
                val decoded = json.decodeFromString(VideoInformationSerializer, rawJson)
                val typeData = decoded.typeData as VideoInformation.Type.Video
                typeData.thumbnailUrl shouldBe ""
            }
        }

        context("decode playlist type") {
            val rawVideoJson = """
            {
                "id": "abc",
                "title": "title",
                "webpage_url": "example.com/abc",
                "webpage_url_domain": "example.com",
                "_type": "video",
                "fulltitle": "fullTitle",
                "thumbnail": "thumbnail.example.com/abc",
                "url": "video.example.com/abc"
            }
            """.trimIndent()

            test("decodes playlist with entries correctly") {
                val rawPlaylistJson = """
                {
                    "id": "playlist_id",
                    "title": "My Playlist",
                    "webpage_url": "example.com/playlist",
                    "webpage_url_domain": "example.com",
                    "_type": "playlist",
                    "entries": [$rawVideoJson]
                }
                """.trimIndent()
                val decoded = json.decodeFromString(VideoInformationSerializer, rawPlaylistJson)
                decoded.id shouldBe "playlist_id"
                decoded.title shouldBe "My Playlist"
                val typeData = decoded.typeData as VideoInformation.Type.Playlist
                typeData.entries.size shouldBe 1
                typeData.entries[0].id shouldBe "abc"
            }
        }
    })