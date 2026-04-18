package net.turtton.ytalarm.usecase

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.kernel.error.DownloadError
import net.turtton.ytalarm.kernel.error.DownloadResult
import net.turtton.ytalarm.kernel.fake.FakeFileStoragePort
import net.turtton.ytalarm.kernel.fake.FakeVideoDownloadRepository
import net.turtton.ytalarm.kernel.fake.FakeVideoDownloadRepository.DownloadRequest
import net.turtton.ytalarm.kernel.fake.FakeVideoRepository
import net.turtton.ytalarm.kernel.port.FileStoragePort
import net.turtton.ytalarm.usecase.fake.FakeLocalDataSourceContainer
import net.turtton.ytalarm.usecase.fake.FakeRemoteDataSourceContainer
import java.io.File

class DownloadUseCaseTest :
    FunSpec({
        lateinit var fakeVideoRepo: FakeVideoRepository
        lateinit var fakeDownloadRepo: FakeVideoDownloadRepository
        lateinit var fakeFileStorage: FakeFileStoragePort
        lateinit var useCase:
            DownloadUseCase<Unit, Unit, FakeLocalDataSourceContainer, FakeRemoteDataSourceContainer>

        beforeEach {
            fakeVideoRepo = FakeVideoRepository()
            fakeDownloadRepo = FakeVideoDownloadRepository()
            fakeFileStorage = FakeFileStoragePort()
            val localDs = FakeLocalDataSourceContainer(videoRepository = fakeVideoRepo)
            val remoteDs = FakeRemoteDataSourceContainer(videoDownloadRepository = fakeDownloadRepo)
            useCase =
                object :
                    DownloadUseCase<
                        Unit,
                        Unit,
                        FakeLocalDataSourceContainer,
                        FakeRemoteDataSourceContainer
                        > {
                    override val localDataSource: FakeLocalDataSourceContainer = localDs
                    override val remoteDataSource: FakeRemoteDataSourceContainer = remoteDs
                    override val fileStorage: FileStoragePort = fakeFileStorage
                }
        }

        context("downloadVideo") {
            test("returns null when video not found") {
                val result = useCase.downloadVideo(999L)

                result shouldBe null
            }

            test("returns null for already Downloaded video") {
                val video = Video(
                    id = 1L,
                    videoId = "v1",
                    state = Video.State.Downloaded("downloads/1.mp4", 1000L, true)
                )
                fakeVideoRepo.resetWith(video)

                val result = useCase.downloadVideo(1L)

                result shouldBe null
                fakeVideoRepo.currentData.find { it.id == 1L }?.state shouldBe
                    Video.State.Downloaded("downloads/1.mp4", 1000L, true)
            }

            test("retries download for stuck Downloading video") {
                val video = Video(
                    id = 1L,
                    videoId = "v1",
                    videoUrl = "http://example.com/video",
                    state = Video.State.Downloading
                )
                fakeVideoRepo.resetWith(video)
                fakeDownloadRepo.downloadResponses["http://example.com/video"] =
                    Either.Right(
                        DownloadResult(
                            "${fakeFileStorage.getDownloadDir().absolutePath}/1.mp4",
                            3000L
                        )
                    )

                val result = useCase.downloadVideo(1L)

                result.shouldBeInstanceOf<Either.Right<DownloadResult>>()
                result.getOrNull()?.fileSize shouldBe 3000L
            }

            test("successful download transitions Information to Downloaded") {
                val video = Video(
                    id = 1L,
                    videoId = "v1",
                    videoUrl = "http://example.com/video",
                    state = Video.State.Information(isStreamable = true)
                )
                fakeVideoRepo.resetWith(video)
                fakeDownloadRepo.downloadResponses["http://example.com/video"] =
                    Either.Right(
                        DownloadResult(
                            "${fakeFileStorage.getDownloadDir().absolutePath}/1.mp4",
                            5000L
                        )
                    )

                val result = useCase.downloadVideo(1L)

                result.shouldBeInstanceOf<Either.Right<DownloadResult>>()
                result.getOrNull()?.fileSize shouldBe 5000L

                fakeVideoRepo.updateHistory.size shouldBe 2
                fakeVideoRepo.updateHistory[0].state shouldBe Video.State.Downloading

                fakeDownloadRepo.downloadRequests.size shouldBe 1
                val req = fakeDownloadRepo.downloadRequests.first()
                req shouldBe DownloadRequest(
                    videoUrl = "http://example.com/video",
                    outputPath =
                        "${fakeFileStorage.getDownloadDir().absolutePath}/1.%(ext)s",
                    formatSelector = DownloadUseCase.DEFAULT_FORMAT_SELECTOR
                )

                val updatedVideo = fakeVideoRepo.currentData.find { it.id == 1L }
                updatedVideo?.state shouldBe
                    Video.State.Downloaded("downloads/1.mp4", 5000L, true)
            }

            test("failed download reverts to Information") {
                val video = Video(
                    id = 1L,
                    videoId = "v1",
                    videoUrl = "http://example.com/video",
                    state = Video.State.Information(isStreamable = false)
                )
                fakeVideoRepo.resetWith(video)
                fakeDownloadRepo.downloadResponses["http://example.com/video"] =
                    Either.Left(DownloadError.NetworkError(RuntimeException("timeout")))

                val result = useCase.downloadVideo(1L)

                result.shouldBeInstanceOf<Either.Left<DownloadError>>()

                val updatedVideo = fakeVideoRepo.currentData.find { it.id == 1L }
                updatedVideo?.state shouldBe Video.State.Information(isStreamable = false)
            }
        }

        context("canDownload") {
            test("returns true when under limit") {
                fakeFileStorage.totalSize = 500L
                useCase.canDownload(1000L) shouldBe true
            }

            test("returns false when at limit") {
                fakeFileStorage.totalSize = 1000L
                useCase.canDownload(1000L) shouldBe false
            }

            test("returns false when over limit") {
                fakeFileStorage.totalSize = 1500L
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
                fakeVideoRepo.resetWith(downloadedVideo, infoVideo)
                fakeFileStorage.existingFiles.add("downloads/1.mp4")
                fakeFileStorage.totalSize = 5000L

                val deletedCount = useCase.deleteAllDownloads()

                deletedCount shouldBe 1L
                val updatedVideo = fakeVideoRepo.currentData.find { it.id == 1L }
                updatedVideo?.state shouldBe Video.State.Information(isStreamable = true)
                val unchangedVideo = fakeVideoRepo.currentData.find { it.id == 2L }
                unchangedVideo?.state shouldBe Video.State.Information(isStreamable = true)
            }
        }

        context("getTotalDownloadSize") {
            test("delegates to fileStorage") {
                fakeFileStorage.totalSize = 42L
                useCase.getTotalDownloadSize() shouldBe 42L
            }
        }
    })