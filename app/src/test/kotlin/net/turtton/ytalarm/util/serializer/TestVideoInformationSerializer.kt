package net.turtton.ytalarm.util.serializer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.turtton.ytalarm.util.VideoInformation

@Suppress("UNUSED")
class TestVideoInformationSerializer :
    FunSpec({
        val json = Json { ignoreUnknownKeys = true }
        context("decode") {
            val videoData = VideoInformation(
                "abc",
                "title",
                "example.com/abc",
                "example.com",
                VideoInformation.Type.Video(
                    "fullTitle",
                    "thumbnail.example.com/abc",
                    "video.example.com/abc"
                )
            )
            val rowVideoData = """
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
            test("video") {
                val decoded = json.decodeFromString<VideoInformation>(rowVideoData)
                decoded shouldBe videoData
            }
            test("playlist") {
                val exampleData = """
                {
                    "id": "abc",
                    "title": "title",
                    "webpage_url": "example.com/abc",
                    "webpage_url_domain": "example.com",
                    "_type": "playlist",
                    "entries": [$rowVideoData]
                }
                """.trimIndent()
                val expected = VideoInformation(
                    "abc",
                    "title",
                    "example.com/abc",
                    "example.com",
                    VideoInformation.Type.Playlist(listOf(videoData))
                )

                val decoded = json.decodeFromString<VideoInformation>(exampleData)
                decoded shouldBe expected
            }
        }
    })