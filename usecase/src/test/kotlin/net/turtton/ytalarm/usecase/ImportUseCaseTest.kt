package net.turtton.ytalarm.usecase

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.turtton.ytalarm.kernel.di.DataSource
import net.turtton.ytalarm.kernel.di.DependsOnDataSource
import net.turtton.ytalarm.kernel.di.DependsOnPlaylistRepository
import net.turtton.ytalarm.kernel.di.DependsOnVideoInfoRepository
import net.turtton.ytalarm.kernel.di.DependsOnVideoRepository
import net.turtton.ytalarm.kernel.dto.VideoInformation
import net.turtton.ytalarm.kernel.entity.Playlist
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.kernel.error.VideoInfoError
import net.turtton.ytalarm.kernel.repository.PlaylistRepository
import net.turtton.ytalarm.kernel.repository.VideoInfoRepository
import net.turtton.ytalarm.kernel.repository.VideoRepository
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ImportUseCaseTest :
    FunSpec({
        lateinit var mockVideoRepo: VideoRepository<Unit>
        lateinit var mockPlaylistRepo: PlaylistRepository<Unit>
        lateinit var mockVideoInfoRepo: VideoInfoRepository<Unit>
        lateinit var useCase: ImportUseCase<Unit, Unit, TestImportLocalDS, TestImportRemoteDS>

        beforeEach {
            mockVideoRepo = mock()
            mockPlaylistRepo = mock()
            mockVideoInfoRepo = mock()

            val localDs = TestImportLocalDS(mockVideoRepo, mockPlaylistRepo)
            val remoteDs = TestImportRemoteDS(mockVideoInfoRepo)
            useCase = object : ImportUseCase<Unit, Unit, TestImportLocalDS, TestImportRemoteDS> {
                override val localDataSource: TestImportLocalDS = localDs
                override val remoteDataSource: TestImportRemoteDS = remoteDs
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
                whenever(mockVideoRepo.getFromVideoIdSync(Unit, "vid1")).thenReturn(existingVideo)

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
                whenever(mockVideoRepo.getFromVideoIdSync(Unit, "vid1")).thenReturn(existingVideo)

                val result = useCase.checkVideoDuplication("vid1", "example.com")

                result shouldBe null
            }

            test("no existing video: returns null") {
                whenever(mockVideoRepo.getFromVideoIdSync(Unit, "vid1")).thenReturn(null)

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
                whenever(
                    mockVideoInfoRepo.fetchVideoInfo(Unit, "http://example.com/video")
                ).thenReturn(Either.Right(videoInfo))
                whenever(mockVideoRepo.getFromVideoIdSync(Unit, "vid1")).thenReturn(null)
                whenever(mockVideoRepo.insert(any(), any())).thenReturn(1L)

                val result = useCase.fetchAndImportVideo("http://example.com/video")

                result shouldBe ImportResult.Success(1L)
                verify(mockVideoRepo).insert(any(), any())
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
                whenever(
                    mockVideoInfoRepo.fetchVideoInfo(Unit, "http://example.com/video")
                ).thenReturn(Either.Right(videoInfo))
                whenever(mockVideoRepo.getFromVideoIdSync(Unit, "vid1")).thenReturn(existingVideo)

                val result = useCase.fetchAndImportVideo("http://example.com/video")

                result shouldBe ImportResult.Duplicate(42L)
                verify(mockVideoRepo, never()).insert(any(), any())
            }

            test("network error: returns failure") {
                whenever(
                    mockVideoInfoRepo.fetchVideoInfo(Unit, "http://example.com/video")
                ).thenReturn(
                    Either.Left(VideoInfoError.NetworkError(RuntimeException("network error")))
                )

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
                val newVideo1 = VideoInformation(
                    id = "vid3",
                    url = "http://example.com/vid3",
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video("V3", "", "")
                )
                whenever(mockVideoRepo.getFromVideoIdSync(Unit, "vid3")).thenReturn(null)
                whenever(mockVideoRepo.insert(any(), any())).thenReturn(3L)

                useCase.syncCloudPlaylist(playlist, listOf(newVideo1))

                val captor = argumentCaptor<Playlist>()
                verify(mockPlaylistRepo).update(any(), captor.capture())
                // videos should include existing (1L, 2L) + new (3L)
                captor.firstValue.videos.containsAll(listOf(1L, 2L, 3L)) shouldBe true
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
                val newVideo = VideoInformation(
                    id = "vid3",
                    url = "http://example.com/vid3",
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video("V3", "", "")
                )
                whenever(mockVideoRepo.getFromVideoIdSync(Unit, "vid3")).thenReturn(null)
                whenever(mockVideoRepo.insert(any(), any())).thenReturn(3L)

                useCase.syncCloudPlaylist(playlist, listOf(newVideo))

                val captor = argumentCaptor<Playlist>()
                verify(mockPlaylistRepo).update(any(), captor.capture())
                // Should only have the new video
                captor.firstValue.videos shouldBe listOf(3L)
                // Thumbnail should be updated since old thumbnail (vid 1) no longer exists
                captor.firstValue.thumbnail shouldBe Playlist.Thumbnail.Video(3L)
            }
        }
    })

// テスト用DataSource実装
class TestImportLocalDS(
    override val videoRepository: VideoRepository<Unit>,
    override val playlistRepository: PlaylistRepository<Unit>
) : DependsOnVideoRepository<Unit>,
    DependsOnPlaylistRepository<Unit>,
    DependsOnDataSource<Unit> {
    override val dataSource: DataSource<Unit> = object : DataSource<Unit> {
        override fun createExecutor(): Unit = Unit
    }
}

class TestImportRemoteDS(override val videoInfoRepository: VideoInfoRepository<Unit>) :
    DependsOnVideoInfoRepository<Unit>,
    DependsOnDataSource<Unit> {
    override val dataSource: DataSource<Unit> = object : DataSource<Unit> {
        override fun createExecutor(): Unit = Unit
    }
}