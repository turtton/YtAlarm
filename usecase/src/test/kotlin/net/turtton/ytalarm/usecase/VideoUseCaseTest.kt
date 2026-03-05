package net.turtton.ytalarm.usecase

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Clock
import net.turtton.ytalarm.kernel.di.DataSource
import net.turtton.ytalarm.kernel.di.DependsOnDataSource
import net.turtton.ytalarm.kernel.di.DependsOnVideoInfoRepository
import net.turtton.ytalarm.kernel.di.DependsOnVideoRepository
import net.turtton.ytalarm.kernel.dto.VideoInformation
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.kernel.error.VideoInfoError
import net.turtton.ytalarm.kernel.repository.VideoInfoRepository
import net.turtton.ytalarm.kernel.repository.VideoRepository
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.hours

class VideoUseCaseTest :
    FunSpec({
        lateinit var mockVideoRepo: VideoRepository<Unit>
        lateinit var mockVideoInfoRepo: VideoInfoRepository<Unit>
        lateinit var useCase: VideoUseCase<Unit, Unit, TestVideoLocalDS, TestVideoRemoteDS>

        beforeEach {
            mockVideoRepo = mock()
            mockVideoInfoRepo = mock()

            val localDs = TestVideoLocalDS(mockVideoRepo)
            val remoteDs = TestVideoRemoteDS(mockVideoInfoRepo)
            useCase = object : VideoUseCase<Unit, Unit, TestVideoLocalDS, TestVideoRemoteDS> {
                override val localDataSource: TestVideoLocalDS = localDs
                override val remoteDataSource: TestVideoRemoteDS = remoteDs
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
                whenever(
                    mockVideoInfoRepo.fetchVideoInfo(Unit, "http://example.com/video")
                ).thenReturn(Either.Right(fetchedInfo))

                val result = useCase.reimportVideo(failedVideo)

                result shouldBe ReimportResult.Success
                val captor = argumentCaptor<Video>()
                verify(mockVideoRepo).update(any(), captor.capture())
                captor.firstValue.id shouldBe 1L
                captor.firstValue.videoId shouldBe "newId"
            }

            test("reimport with no url: returns NoUrl error") {
                val importingVideo = Video(
                    id = 1L,
                    videoId = "vid1",
                    state = Video.State.Importing
                )

                val result = useCase.reimportVideo(importingVideo)

                result shouldBe ReimportResult.Error.NoUrl
                verify(mockVideoRepo, never()).update(any(), any())
            }

            test("reimport network error: returns Network error") {
                val failedVideo = Video(
                    id = 1L,
                    videoId = "vid1",
                    state = Video.State.Failed("http://example.com/video")
                )
                whenever(
                    mockVideoInfoRepo.fetchVideoInfo(Unit, "http://example.com/video")
                ).thenReturn(
                    Either.Left(VideoInfoError.NetworkError(RuntimeException("network error")))
                )

                val result = useCase.reimportVideo(failedVideo)

                result shouldBe ReimportResult.Error.Network
                verify(mockVideoRepo, never()).update(any(), any())
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
                whenever(
                    mockVideoRepo.getFromIdsSync(Unit, listOf(10L, 11L, 12L))
                ).thenReturn(listOf(playableVideo, failedVideo, importingVideo))

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
                    // 作成日時を古くするために creationDate を手動で設定
                    creationDate = Clock.System.now() - 2.hours
                )
                val newImporting = Video(
                    id = 2L,
                    videoId = "new",
                    state = Video.State.Importing,
                    creationDate = Clock.System.now()
                )
                whenever(mockVideoRepo.getFromIdsSync(any(), any())).thenReturn(
                    listOf(oldImporting, newImporting)
                )

                useCase.collectGarbageVideos(listOf(oldImporting.id, newImporting.id), threshold)

                val captor = argumentCaptor<Video>()
                verify(mockVideoRepo).update(any(), captor.capture())
                captor.firstValue.id shouldBe 1L
                captor.firstValue.state shouldBe Video.State.Failed("")
            }
        }
    })

// テスト用DataSource実装
class TestVideoLocalDS(override val videoRepository: VideoRepository<Unit>) :
    DependsOnVideoRepository<Unit>,
    DependsOnDataSource<Unit> {
    override val dataSource: DataSource<Unit> = object : DataSource<Unit> {
        override fun createExecutor(): Unit = Unit
    }
}

class TestVideoRemoteDS(override val videoInfoRepository: VideoInfoRepository<Unit>) :
    DependsOnVideoInfoRepository<Unit>,
    DependsOnDataSource<Unit> {
    override val dataSource: DataSource<Unit> = object : DataSource<Unit> {
        override fun createExecutor(): Unit = Unit
    }
}