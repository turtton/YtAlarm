package net.turtton.ytalarm.usecase

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.turtton.ytalarm.kernel.di.DataSource
import net.turtton.ytalarm.kernel.di.DependsOnDataSource
import net.turtton.ytalarm.kernel.di.DependsOnVideoDownloadRepository
import net.turtton.ytalarm.kernel.di.DependsOnVideoRepository
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.kernel.error.DownloadError
import net.turtton.ytalarm.kernel.error.DownloadResult
import net.turtton.ytalarm.kernel.port.FileStoragePort
import net.turtton.ytalarm.kernel.repository.VideoDownloadRepository
import net.turtton.ytalarm.kernel.repository.VideoRepository
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class DownloadUseCaseTest :
    FunSpec({
        lateinit var mockVideoRepo: VideoRepository<Unit>
        lateinit var mockDownloadRepo: VideoDownloadRepository<Unit>
        lateinit var mockFileStorage: FileStoragePort
        lateinit var useCase: DownloadUseCase<Unit, Unit, TestDlLocalDS, TestDlRemoteDS>

        beforeEach {
            mockVideoRepo = mock()
            mockDownloadRepo = mock()
            mockFileStorage = mock()

            val localDs = TestDlLocalDS(mockVideoRepo)
            val remoteDs = TestDlRemoteDS(mockDownloadRepo)
            useCase =
                object : DownloadUseCase<Unit, Unit, TestDlLocalDS, TestDlRemoteDS> {
                    override val localDataSource: TestDlLocalDS = localDs
                    override val remoteDataSource: TestDlRemoteDS = remoteDs
                    override val fileStorage: FileStoragePort = mockFileStorage
                }
        }

        context("downloadVideo") {
            test("returns null when video not found") {
                whenever(mockVideoRepo.getFromIdSync(any(), eq(999L))).thenReturn(null)

                val result = useCase.downloadVideo(999L)

                result shouldBe null
            }

            test("returns null for already Downloaded video") {
                val video = Video(
                    id = 1L,
                    videoId = "v1",
                    state = Video.State.Downloaded("downloads/1.mp4", 1000L, true)
                )
                whenever(mockVideoRepo.getFromIdSync(any(), eq(1L))).thenReturn(video)

                val result = useCase.downloadVideo(1L)

                result shouldBe null
                verify(mockVideoRepo, never()).update(any(), any())
            }

            test("returns null for Downloading video") {
                val video = Video(
                    id = 1L,
                    videoId = "v1",
                    state = Video.State.Downloading
                )
                whenever(mockVideoRepo.getFromIdSync(any(), eq(1L))).thenReturn(video)

                val result = useCase.downloadVideo(1L)

                result shouldBe null
            }

            test("successful download transitions Information to Downloaded") {
                val video = Video(
                    id = 1L,
                    videoId = "v1",
                    videoUrl = "http://example.com/video",
                    state = Video.State.Information(isStreamable = true)
                )
                val downloadDir = File("/tmp/test-downloads")
                val dlResult = DownloadResult("/tmp/test-downloads/1.mp4", 5000L)

                whenever(mockVideoRepo.getFromIdSync(any(), eq(1L))).thenReturn(video)
                whenever(mockFileStorage.getDownloadDir()).thenReturn(downloadDir)
                whenever(
                    mockDownloadRepo.downloadVideo(
                        any(),
                        eq("http://example.com/video"),
                        eq("${downloadDir.absolutePath}/1.%(ext)s"),
                        eq(DownloadUseCase.DEFAULT_FORMAT_SELECTOR),
                        any()
                    )
                ).thenReturn(Either.Right(dlResult))

                val result = useCase.downloadVideo(1L)

                result.shouldBeInstanceOf<Either.Right<DownloadResult>>()
                result.getOrNull()?.fileSize shouldBe 5000L

                val captor = argumentCaptor<Video>()
                // 2回のupdate: Downloading状態への遷移 + Downloaded状態への遷移
                verify(mockVideoRepo, org.mockito.kotlin.times(2)).update(any(), captor.capture())
                captor.firstValue.state shouldBe Video.State.Downloading
                captor.secondValue.state shouldBe Video.State.Downloaded(
                    "downloads/1.mp4",
                    5000L,
                    true
                )
            }

            test("failed download reverts to Information") {
                val video = Video(
                    id = 1L,
                    videoId = "v1",
                    videoUrl = "http://example.com/video",
                    state = Video.State.Information(isStreamable = false)
                )
                val downloadDir = File("/tmp/test-downloads")
                val error = DownloadError.NetworkError(RuntimeException("timeout"))

                whenever(mockVideoRepo.getFromIdSync(any(), eq(1L))).thenReturn(video)
                whenever(mockFileStorage.getDownloadDir()).thenReturn(downloadDir)
                whenever(
                    mockDownloadRepo.downloadVideo(any(), any(), any(), any(), any())
                ).thenReturn(Either.Left(error))

                val result = useCase.downloadVideo(1L)

                result.shouldBeInstanceOf<Either.Left<DownloadError>>()

                val captor = argumentCaptor<Video>()
                verify(mockVideoRepo, org.mockito.kotlin.times(2)).update(any(), captor.capture())
                captor.firstValue.state shouldBe Video.State.Downloading
                captor.secondValue.state shouldBe Video.State.Information(isStreamable = false)
            }
        }

        context("canDownload") {
            test("returns true when under limit") {
                whenever(mockFileStorage.getTotalDownloadSize()).thenReturn(500L)
                useCase.canDownload(1000L) shouldBe true
            }

            test("returns false when at limit") {
                whenever(mockFileStorage.getTotalDownloadSize()).thenReturn(1000L)
                useCase.canDownload(1000L) shouldBe false
            }

            test("returns false when over limit") {
                whenever(mockFileStorage.getTotalDownloadSize()).thenReturn(1500L)
                useCase.canDownload(1000L) shouldBe false
            }
        }

        context("deleteAllDownloads") {
            test("reverts Downloaded videos to Information and deletes files") {
                val downloadedVideo = Video(
                    id = 1L,
                    videoId = "v1",
                    state = Video.State.Downloaded("downloads/1.mp4", 5000L, true)
                )
                val infoVideo = Video(
                    id = 2L,
                    videoId = "v2",
                    state = Video.State.Information(isStreamable = true)
                )
                whenever(mockVideoRepo.getExceptIdsSync(any(), eq(emptyList()))).thenReturn(
                    listOf(downloadedVideo, infoVideo)
                )
                whenever(mockFileStorage.deleteAllDownloads()).thenReturn(1L)

                val deletedCount = useCase.deleteAllDownloads()

                deletedCount shouldBe 1L
                val captor = argumentCaptor<Video>()
                verify(mockVideoRepo).update(any(), captor.capture())
                captor.firstValue.id shouldBe 1L
                captor.firstValue.state shouldBe Video.State.Information(isStreamable = true)
            }
        }

        context("getTotalDownloadSize") {
            test("delegates to fileStorage") {
                whenever(mockFileStorage.getTotalDownloadSize()).thenReturn(42L)
                useCase.getTotalDownloadSize() shouldBe 42L
            }
        }
    })

class TestDlLocalDS(override val videoRepository: VideoRepository<Unit>) :
    DependsOnVideoRepository<Unit>,
    DependsOnDataSource<Unit> {
    override val dataSource: DataSource<Unit> = object : DataSource<Unit> {
        override fun createExecutor(): Unit = Unit
    }
}

class TestDlRemoteDS(override val videoDownloadRepository: VideoDownloadRepository<Unit>) :
    DependsOnVideoDownloadRepository<Unit>,
    DependsOnDataSource<Unit> {
    override val dataSource: DataSource<Unit> = object : DataSource<Unit> {
        override fun createExecutor(): Unit = Unit
    }
}