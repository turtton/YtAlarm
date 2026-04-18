package net.turtton.ytalarm.usecase

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import net.turtton.ytalarm.kernel.dto.VideoInformation
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.kernel.error.VideoInfoError
import net.turtton.ytalarm.kernel.fake.FakeVideoInfoRepository
import net.turtton.ytalarm.kernel.fake.FakeVideoRepository
import net.turtton.ytalarm.usecase.fake.FakeLocalDataSourceContainer
import net.turtton.ytalarm.usecase.fake.FakeRemoteDataSourceContainer
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

class VideoUseCaseTest :
    FunSpec({
        lateinit var fakeVideoRepo: FakeVideoRepository
        lateinit var fakeVideoInfoRepo: FakeVideoInfoRepository
        lateinit var useCase: VideoUseCase<
            Unit,
            Unit,
            FakeLocalDataSourceContainer,
            FakeRemoteDataSourceContainer
            >

        beforeEach {
            fakeVideoRepo = FakeVideoRepository()
            fakeVideoInfoRepo = FakeVideoInfoRepository()
            val localDs = FakeLocalDataSourceContainer(
                videoRepository = fakeVideoRepo
            )
            val remoteDs = FakeRemoteDataSourceContainer(
                videoInfoRepository = fakeVideoInfoRepo
            )
            useCase = object : VideoUseCase<
                Unit,
                Unit,
                FakeLocalDataSourceContainer,
                FakeRemoteDataSourceContainer
                > {
                override val localDataSource: FakeLocalDataSourceContainer = localDs
                override val remoteDataSource: FakeRemoteDataSourceContainer = remoteDs
            }
        }

        context("reimportVideo") {
            test("reimport success: updates video in repository") {
                val failedVideo = Video(
                    id = 1L,
                    videoId = "oldId",
                    state = Video.State.Failed("http://example.com/video")
                )
                val fetchedInfo = VideoInformation(
                    id = "newId",
                    title = "New Title",
                    url = "http://example.com/video",
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video(
                        fullTitle = "New Title",
                        thumbnailUrl = "http://thumb.com",
                        videoUrl = "http://example.com/video.mp4"
                    )
                )
                fakeVideoRepo.resetWith(failedVideo)
                fakeVideoInfoRepo.videoInfoResponses["http://example.com/video"] =
                    Either.Right(fetchedInfo)

                val result = useCase.reimportVideo(failedVideo)

                result shouldBe ReimportResult.Success
                fakeVideoRepo.currentData.first { it.id == 1L }.videoId shouldBe "newId"
            }

            test("reimport with no url: returns NoUrl error") {
                val importingVideo = Video(
                    id = 1L,
                    videoId = "vid1",
                    state = Video.State.Importing
                )
                fakeVideoRepo.resetWith(importingVideo)

                val result = useCase.reimportVideo(importingVideo)

                result shouldBe ReimportResult.Error.NoUrl
                fakeVideoRepo.currentData.first { it.id == 1L }.state shouldBe
                    Video.State.Importing
            }

            test("reimport network error: returns Network error") {
                val failedVideo = Video(
                    id = 1L,
                    videoId = "vid1",
                    state = Video.State.Failed("http://example.com/video")
                )
                fakeVideoRepo.resetWith(failedVideo)
                fakeVideoInfoRepo.videoInfoResponses["http://example.com/video"] =
                    Either.Left(VideoInfoError.NetworkError(RuntimeException("network error")))

                val result = useCase.reimportVideo(failedVideo)

                result shouldBe ReimportResult.Error.Network
                fakeVideoRepo.currentData.first { it.id == 1L }.state shouldBe
                    Video.State.Failed("http://example.com/video")
            }
        }

        context("getPlayableVideosForAlarm") {
            test("returns only Information state videos from alarm playlists") {
                val alarm = Alarm(id = 1L, playlistIds = listOf(1L, 2L))
                val playableVideo = Video(
                    id = 10L,
                    videoId = "v1",
                    state = Video.State.Information(isStreamable = true)
                )
                val failedVideo = Video(
                    id = 11L,
                    videoId = "v2",
                    state = Video.State.Failed("url")
                )
                val importingVideo = Video(
                    id = 12L,
                    videoId = "v3",
                    state = Video.State.Importing
                )
                fakeVideoRepo.resetWith(playableVideo, failedVideo, importingVideo)

                val result = useCase.getPlayableVideosForAlarm(
                    alarm,
                    mapOf(1L to listOf(10L, 11L), 2L to listOf(12L))
                )

                result shouldHaveSize 1
                result.first().id shouldBe 10L
            }
        }

        context("collectGarbageVideos") {
            test("marks stuck Importing videos as Failed after threshold") {
                val threshold = 1.hours
                val oldImporting = Video(
                    id = 1L,
                    videoId = "old",
                    state = Video.State.Importing,
                    creationDate = Clock.System.now() - 2.hours
                )
                val newImporting = Video(
                    id = 2L,
                    videoId = "new",
                    state = Video.State.Importing,
                    creationDate = Clock.System.now()
                )
                fakeVideoRepo.resetWith(oldImporting, newImporting)

                useCase.collectGarbageVideos(
                    listOf(oldImporting.id, newImporting.id),
                    threshold
                )

                fakeVideoRepo.currentData.first { it.id == 1L }.state shouldBe
                    Video.State.Failed("")
                fakeVideoRepo.currentData.first { it.id == 2L }.state shouldBe
                    Video.State.Importing
            }
        }
    })