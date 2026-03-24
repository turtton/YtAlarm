package net.turtton.ytalarm.datasource

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.turtton.ytalarm.datasource.entity.VideoEntity
import net.turtton.ytalarm.datasource.mapper.toDomain
import net.turtton.ytalarm.datasource.mapper.toEntity
import net.turtton.ytalarm.kernel.entity.Video
import java.util.Calendar
import kotlin.time.Instant

class VideoMapperTest :
    FunSpec({

        test("VideoEntity.toDomain - Importing state") {
            val calendar = Calendar.getInstance().apply { timeInMillis = 1000L }
            val entity = VideoEntity(
                id = 1L,
                videoId = "abc123",
                title = "Test Video",
                thumbnailUrl = "https://example.com/thumb.jpg",
                videoUrl = "https://example.com/video",
                domain = "youtube.com",
                stateData = VideoEntity.State.Importing,
                creationDate = calendar
            )
            val domain = entity.toDomain()
            domain.id shouldBe 1L
            domain.videoId shouldBe "abc123"
            domain.title shouldBe "Test Video"
            domain.thumbnailUrl shouldBe "https://example.com/thumb.jpg"
            domain.videoUrl shouldBe "https://example.com/video"
            domain.domain shouldBe "youtube.com"
            domain.state shouldBe Video.State.Importing
            domain.creationDate shouldBe Instant.fromEpochMilliseconds(1000L)
        }

        test("VideoEntity.toDomain - Information state") {
            val entity = VideoEntity(
                id = 2L,
                videoId = "def456",
                title = "Info Video",
                thumbnailUrl = "",
                videoUrl = "",
                domain = "",
                stateData = VideoEntity.State.Information(isStreamable = true),
                creationDate = Calendar.getInstance()
            )
            val domain = entity.toDomain()
            domain.state shouldBe Video.State.Information(isStreamable = true)
        }

        test("VideoEntity.toDomain - Downloading state") {
            val entity = VideoEntity(
                id = 3L,
                videoId = "ghi789",
                title = "Downloading Video",
                thumbnailUrl = "",
                videoUrl = "",
                domain = "",
                stateData = VideoEntity.State.Downloading,
                creationDate = Calendar.getInstance()
            )
            val domain = entity.toDomain()
            domain.state shouldBe Video.State.Downloading
        }

        test("VideoEntity.toDomain - Downloaded state") {
            val entity = VideoEntity(
                id = 4L,
                videoId = "jkl012",
                title = "Downloaded Video",
                thumbnailUrl = "",
                videoUrl = "",
                domain = "",
                stateData = VideoEntity.State.Downloaded(
                    internalLink = "/storage/video.mp4",
                    fileSize = 1024L,
                    isStreamable = false
                ),
                creationDate = Calendar.getInstance()
            )
            val domain = entity.toDomain()
            domain.state shouldBe Video.State.Downloaded(
                internalLink = "/storage/video.mp4",
                fileSize = 1024L,
                isStreamable = false
            )
        }

        test("VideoEntity.toDomain - Failed state") {
            val entity = VideoEntity(
                id = 5L,
                videoId = "mno345",
                title = "Failed Video",
                thumbnailUrl = "",
                videoUrl = "",
                domain = "",
                stateData = VideoEntity.State.Failed(sourceUrl = "https://example.com/video"),
                creationDate = Calendar.getInstance()
            )
            val domain = entity.toDomain()
            domain.state shouldBe Video.State.Failed(sourceUrl = "https://example.com/video")
        }

        test("Video.toEntity - roundtrip") {
            val instant = Instant.fromEpochMilliseconds(3000L)
            val video = Video(
                id = 6L,
                videoId = "pqr678",
                title = "Roundtrip Video",
                thumbnailUrl = "https://example.com/thumb2.jpg",
                videoUrl = "https://example.com/video2",
                domain = "vimeo.com",
                state = Video.State.Information(isStreamable = true),
                creationDate = instant
            )
            val entity = video.toEntity()
            val backToDomain = entity.toDomain()

            backToDomain.id shouldBe video.id
            backToDomain.videoId shouldBe video.videoId
            backToDomain.title shouldBe video.title
            backToDomain.thumbnailUrl shouldBe video.thumbnailUrl
            backToDomain.videoUrl shouldBe video.videoUrl
            backToDomain.domain shouldBe video.domain
            backToDomain.state shouldBe video.state
            backToDomain.creationDate shouldBe video.creationDate
        }

        test("Video.toEntity - Failed state toEntity conversion") {
            val video = Video(
                id = 7L,
                videoId = "stu901",
                title = "Failed",
                thumbnailUrl = "",
                videoUrl = "",
                domain = "",
                state = Video.State.Failed(sourceUrl = "https://source.com"),
                creationDate = Instant.fromEpochMilliseconds(0)
            )
            val entity = video.toEntity()
            entity.stateData shouldBe VideoEntity.State.Failed(sourceUrl = "https://source.com")
        }
    })