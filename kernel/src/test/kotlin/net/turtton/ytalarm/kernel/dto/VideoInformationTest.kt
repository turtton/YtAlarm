package net.turtton.ytalarm.kernel.dto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class VideoInformationTest :
    FunSpec({
        test("VideoInformation holds video type data") {
            val videoType = VideoInformation.Type.Video(
                fullTitle = "Test Video",
                thumbnailUrl = "http://example.com/thumb.jpg",
                videoUrl = "http://example.com/video.mp4"
            )
            val info = VideoInformation(
                id = "abc123",
                title = "Test Video",
                url = "http://example.com/video",
                domain = "example.com",
                typeData = videoType
            )
            info.id shouldBe "abc123"
            info.domain shouldBe "example.com"
            info.typeData.shouldBeInstanceOf<VideoInformation.Type.Video>()
        }

        test("VideoInformation holds playlist type data") {
            val entries = listOf(
                VideoInformation(
                    id = "vid1",
                    url = "http://example.com/vid1",
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video(
                        "Title 1",
                        "http://thumb1.jpg",
                        "http://stream1.m3u8"
                    )
                )
            )
            val playlistType = VideoInformation.Type.Playlist(entries)
            val info = VideoInformation(
                id = "playlist1",
                title = "My Playlist",
                url = "http://example.com/playlist",
                domain = "example.com",
                typeData = playlistType
            )
            val playlistTypeData =
                info.typeData.shouldBeInstanceOf<VideoInformation.Type.Playlist>()
            playlistTypeData.entries.size shouldBe 1
        }
    })