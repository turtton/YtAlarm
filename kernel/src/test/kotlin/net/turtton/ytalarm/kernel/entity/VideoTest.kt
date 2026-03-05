package net.turtton.ytalarm.kernel.entity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class VideoTest :
    FunSpec({
        test("Video default values are set correctly") {
            val video = Video(videoId = "abc123", state = Video.State.Importing)
            video.id shouldBe 0L
            video.title shouldBe "No title"
            video.thumbnailUrl shouldBe ""
            video.videoUrl shouldBe ""
            video.domain shouldBe ""
        }

        test("State.Importing isUpdating returns true") {
            Video.State.Importing.isUpdating() shouldBe true
        }

        test("State.Downloading isUpdating returns true") {
            Video.State.Downloading.isUpdating() shouldBe true
        }

        test("State.Information isUpdating returns false") {
            Video.State.Information().isUpdating() shouldBe false
        }

        test("State.Downloaded isUpdating returns false") {
            Video.State.Downloaded("internal/path", 1024L, true).isUpdating() shouldBe false
        }

        test("State.Downloaded accepts Long fileSize") {
            val bigFileSize = Int.MAX_VALUE.toLong() + 1L
            val state = Video.State.Downloaded("internal/path", bigFileSize, true)
            state.fileSize shouldBe bigFileSize
        }

        test("State.Failed isUpdating returns false") {
            Video.State.Failed("http://example.com").isUpdating() shouldBe false
        }

        test("State.Failed holds sourceUrl") {
            val url = "http://example.com/video"
            val state = Video.State.Failed(url)
            state.sourceUrl shouldBe url
            state.shouldBeInstanceOf<Video.State.Failed>()
        }
    })