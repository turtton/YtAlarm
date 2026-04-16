package net.turtton.ytalarm.usecase

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.turtton.ytalarm.kernel.dto.VideoInformation
import net.turtton.ytalarm.kernel.entity.Playlist
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.kernel.error.VideoInfoError
import net.turtton.ytalarm.kernel.fake.FakePlaylistRepository
import net.turtton.ytalarm.kernel.fake.FakeVideoInfoRepository
import net.turtton.ytalarm.kernel.fake.FakeVideoRepository
import net.turtton.ytalarm.usecase.fake.FakeLocalDataSourceContainer
import net.turtton.ytalarm.usecase.fake.FakeRemoteDataSourceContainer

class ImportUseCaseTest :
    FunSpec({
        lateinit var fakeVideoRepo: FakeVideoRepository
        lateinit var fakePlaylistRepo: FakePlaylistRepository
        lateinit var fakeVideoInfoRepo: FakeVideoInfoRepository
        lateinit var useCase:
            ImportUseCase<Unit, Unit, FakeLocalDataSourceContainer, FakeRemoteDataSourceContainer>

        beforeEach {
            fakeVideoRepo = FakeVideoRepository()
            fakePlaylistRepo = FakePlaylistRepository()
            fakeVideoInfoRepo = FakeVideoInfoRepository()
            val localDs = FakeLocalDataSourceContainer(
                videoRepository = fakeVideoRepo,
                playlistRepository = fakePlaylistRepo
            )
            val remoteDs =
                FakeRemoteDataSourceContainer(videoInfoRepository = fakeVideoInfoRepo)
            useCase =
                object :
                    ImportUseCase<
                        Unit,
                        Unit,
                        FakeLocalDataSourceContainer,
                        FakeRemoteDataSourceContainer
                        > {
                    override val localDataSource: FakeLocalDataSourceContainer = localDs
                    override val remoteDataSource: FakeRemoteDataSourceContainer = remoteDs
                }
        }

        context("checkVideoDuplication") {
            test("duplicate found: returns existing video id") {
                val existingVideo =
                    Video(
                        id = 99L,
                        videoId = "vid1",
                        domain = "example.com",
                        state = Video.State.Information()
                    )
                fakeVideoRepo.seed(existingVideo)

                val result = useCase.checkVideoDuplication("vid1", "example.com")

                result shouldBe 99L
            }

            test("different domain: not a duplicate") {
                val existingVideo =
                    Video(
                        id = 99L,
                        videoId = "vid1",
                        domain = "other.com",
                        state = Video.State.Information()
                    )
                fakeVideoRepo.seed(existingVideo)

                val result = useCase.checkVideoDuplication("vid1", "example.com")

                result shouldBe null
            }

            test("no existing video: returns null") {
                val result = useCase.checkVideoDuplication("vid1", "example.com")

                result shouldBe null
            }
        }

        context("fetchAndImportVideo") {
            test("new video: inserts to repository") {
                val videoInfo = VideoInformation(
                    id = "vid1",
                    title = "Test Video",
                    url = "http://example.com/video",
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video(
                        fullTitle = "Test Video",
                        thumbnailUrl = "http://thumb.com",
                        videoUrl = "http://example.com/video.mp4"
                    )
                )
                fakeVideoInfoRepo.videoInfoResponses["http://example.com/video"] =
                    Either.Right(videoInfo)

                val result = useCase.fetchAndImportVideo("http://example.com/video")

                result shouldBe ImportResult.Success(1L)
                fakeVideoRepo.currentData.size shouldBe 1
            }

            test("duplicate video: returns existing id") {
                val videoInfo = VideoInformation(
                    id = "vid1",
                    title = "Test Video",
                    url = "http://example.com/video",
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video(
                        fullTitle = "Test Video",
                        thumbnailUrl = "http://thumb.com",
                        videoUrl = "http://example.com/video.mp4"
                    )
                )
                val existingVideo = Video(
                    id = 42L,
                    videoId = "vid1",
                    domain = "example.com",
                    state = Video.State.Information()
                )
                fakeVideoRepo.seed(existingVideo)
                fakeVideoInfoRepo.videoInfoResponses["http://example.com/video"] =
                    Either.Right(videoInfo)

                val result = useCase.fetchAndImportVideo("http://example.com/video")

                result shouldBe ImportResult.Duplicate(42L)
                fakeVideoRepo.currentData.size shouldBe 1
            }

            test("network error: returns failure") {
                fakeVideoInfoRepo.videoInfoResponses["http://example.com/video"] =
                    Either.Left(VideoInfoError.NetworkError(RuntimeException("network error")))

                val result = useCase.fetchAndImportVideo("http://example.com/video")

                result shouldBe ImportResult.Failure.Network
            }
        }

        context("syncCloudPlaylist") {
            test("ALWAYS_ADD: adds new videos to existing") {
                val playlist = Playlist(
                    id = 1L,
                    videos = listOf(1L, 2L),
                    type = Playlist.Type.CloudPlaylist(
                        "http://example.com",
                        Playlist.SyncRule.ALWAYS_ADD
                    )
                )
                fakePlaylistRepo.seed(playlist)
                fakeVideoRepo.seed(
                    Video(id = 1L, videoId = "existing1", state = Video.State.Information()),
                    Video(id = 2L, videoId = "existing2", state = Video.State.Information())
                )
                val newVideo1 = VideoInformation(
                    id = "vid3",
                    url = "http://example.com/vid3",
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video("V3", "", "")
                )

                useCase.syncCloudPlaylist(playlist, listOf(newVideo1))

                val updatedPlaylist = fakePlaylistRepo.currentData.find { it.id == 1L }
                updatedPlaylist?.videos?.containsAll(listOf(1L, 2L, 3L)) shouldBe true
            }

            test("DELETE_IF_NOT_EXIST: replaces videos with new set") {
                val playlist = Playlist(
                    id = 1L,
                    videos = listOf(1L, 2L),
                    type = Playlist.Type.CloudPlaylist(
                        "http://example.com",
                        Playlist.SyncRule.DELETE_IF_NOT_EXIST
                    ),
                    thumbnail = Playlist.Thumbnail.Video(1L)
                )
                fakePlaylistRepo.seed(playlist)
                fakeVideoRepo.seed(
                    Video(id = 1L, videoId = "existing1", state = Video.State.Information()),
                    Video(id = 2L, videoId = "existing2", state = Video.State.Information())
                )
                val newVideo = VideoInformation(
                    id = "vid3",
                    url = "http://example.com/vid3",
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video("V3", "", "")
                )

                useCase.syncCloudPlaylist(playlist, listOf(newVideo))

                val updatedPlaylist = fakePlaylistRepo.currentData.find { it.id == 1L }
                updatedPlaylist?.videos shouldBe listOf(3L)
                updatedPlaylist?.thumbnail shouldBe Playlist.Thumbnail.Video(3L)
            }
        }
    })