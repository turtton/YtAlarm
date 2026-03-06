package net.turtton.ytalarm.usecase

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import net.turtton.ytalarm.kernel.di.DataSource
import net.turtton.ytalarm.kernel.di.DependsOnAlarmRepository
import net.turtton.ytalarm.kernel.di.DependsOnDataSource
import net.turtton.ytalarm.kernel.di.DependsOnPlaylistRepository
import net.turtton.ytalarm.kernel.di.DependsOnVideoRepository
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.entity.Playlist
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.kernel.port.AlarmSchedulerPort
import net.turtton.ytalarm.kernel.repository.AlarmRepository
import net.turtton.ytalarm.kernel.repository.PlaylistRepository
import net.turtton.ytalarm.kernel.repository.VideoRepository
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PlaylistUseCaseTest :
    FunSpec({
        lateinit var mockPlaylistRepo: PlaylistRepository<Unit>
        lateinit var mockVideoRepo: VideoRepository<Unit>
        lateinit var mockAlarmRepo: AlarmRepository<Unit>
        lateinit var mockScheduler: AlarmSchedulerPort
        lateinit var useCase: PlaylistUseCase<Unit, TestPlaylistLocalDS>

        beforeEach {
            mockPlaylistRepo = mock()
            mockVideoRepo = mock()
            mockAlarmRepo = mock()
            mockScheduler = mock()
            whenever(mockScheduler.scheduleNextAlarm(any())).thenReturn(Either.Right(Unit))

            val ds = TestPlaylistLocalDS(mockPlaylistRepo, mockVideoRepo, mockAlarmRepo)
            useCase = object : PlaylistUseCase<Unit, TestPlaylistLocalDS> {
                override val localDataSource: TestPlaylistLocalDS = ds
            }
        }

        context("validateAndUpdateThumbnails") {
            test("valid thumbnail: no update") {
                val video = Video(
                    id = 1L,
                    videoId = "vid1",
                    state = Video.State.Information()
                )
                val playlist = Playlist(
                    id = 1L,
                    thumbnail = Playlist.Thumbnail.Video(1L),
                    videos = listOf(1L)
                )
                whenever(mockPlaylistRepo.getAllSync(Unit)).thenReturn(listOf(playlist))
                whenever(mockVideoRepo.getFromIdSync(Unit, 1L)).thenReturn(video)

                useCase.validateAndUpdateThumbnails()

                verify(mockPlaylistRepo, never()).update(any(), any())
            }

            test("invalid thumbnail (video missing): update to None") {
                val playlist = Playlist(
                    id = 1L,
                    thumbnail = Playlist.Thumbnail.Video(99L),
                    videos = listOf(99L)
                )
                whenever(mockPlaylistRepo.getAllSync(Unit)).thenReturn(listOf(playlist))
                whenever(mockVideoRepo.getFromIdSync(Unit, 99L)).thenReturn(null)
                whenever(mockVideoRepo.getFromIdsSync(Unit, emptyList())).thenReturn(emptyList())

                useCase.validateAndUpdateThumbnails()

                val captor = argumentCaptor<Playlist>()
                verify(mockPlaylistRepo).update(any(), captor.capture())
                captor.firstValue.thumbnail shouldBe Playlist.Thumbnail.None
            }

            test("thumbnail video not in Information state: update to fallback") {
                val failedVideo = Video(
                    id = 1L,
                    videoId = "vid1",
                    state = Video.State.Failed("url")
                )
                val goodVideo = Video(
                    id = 2L,
                    videoId = "vid2",
                    state = Video.State.Information()
                )
                val playlist = Playlist(
                    id = 1L,
                    thumbnail = Playlist.Thumbnail.Video(1L),
                    videos = listOf(1L, 2L)
                )
                whenever(mockPlaylistRepo.getAllSync(Unit)).thenReturn(listOf(playlist))
                whenever(mockVideoRepo.getFromIdSync(Unit, 1L)).thenReturn(failedVideo)
                whenever(
                    mockVideoRepo.getFromIdsSync(Unit, listOf(2L))
                ).thenReturn(listOf(goodVideo))

                useCase.validateAndUpdateThumbnails()

                val captor = argumentCaptor<Playlist>()
                verify(mockPlaylistRepo).update(any(), captor.capture())
                captor.firstValue.thumbnail shouldBe Playlist.Thumbnail.Video(2L)
            }
        }

        context("safeDeletePlaylist") {
            test("deletes playlist and removes reference from alarms") {
                val playlist = Playlist(id = 1L)
                val alarm = Alarm(id = 10L, playlistIds = listOf(1L, 2L))
                whenever(mockAlarmRepo.getAllSync(Unit)).thenReturn(listOf(alarm))

                useCase.safeDeletePlaylist(playlist)

                verify(mockPlaylistRepo).delete(any(), any())
                val alarmCaptor = argumentCaptor<Alarm>()
                verify(mockAlarmRepo).update(any(), alarmCaptor.capture())
                alarmCaptor.firstValue.playlistIds shouldBe listOf(2L)
            }

            test("deletes playlist with no alarm references") {
                val playlist = Playlist(id = 1L)
                whenever(mockAlarmRepo.getAllSync(Unit)).thenReturn(emptyList())

                useCase.safeDeletePlaylist(playlist)

                verify(mockPlaylistRepo).delete(any(), any())
                verify(mockAlarmRepo, never()).update(any(), any())
            }
        }

        context("removeVideosFromPlaylist") {
            test("removes videos and updates thumbnail if needed") {
                val video1 = Video(id = 1L, videoId = "vid1", state = Video.State.Information())
                val video2 = Video(id = 2L, videoId = "vid2", state = Video.State.Information())
                val playlist = Playlist(
                    id = 1L,
                    videos = listOf(1L, 2L),
                    thumbnail = Playlist.Thumbnail.Video(1L)
                )
                whenever(mockVideoRepo.getFromIdSync(Unit, 1L)).thenReturn(video1)
                whenever(mockVideoRepo.getFromIdSync(Unit, 2L)).thenReturn(video2)
                whenever(mockVideoRepo.getFromIdsSync(Unit, listOf(2L))).thenReturn(listOf(video2))

                useCase.removeVideosFromPlaylist(playlist, listOf(1L))

                val captor = argumentCaptor<Playlist>()
                verify(mockPlaylistRepo).update(any(), captor.capture())
                captor.firstValue.videos shouldBe listOf(2L)
                // サムネイルが削除された動画なので更新される
                captor.firstValue.thumbnail shouldBe Playlist.Thumbnail.Video(2L)
            }
        }

        context("setPlaylistThumbnail") {
            test("sets thumbnail to specified video") {
                val playlist = Playlist(id = 1L)

                useCase.setPlaylistThumbnail(playlist, Playlist.Thumbnail.Video(5L))

                val captor = argumentCaptor<Playlist>()
                verify(mockPlaylistRepo).update(any(), captor.capture())
                captor.firstValue.thumbnail shouldBe Playlist.Thumbnail.Video(5L)
            }
        }

        context("updateSyncRule") {
            test("updates sync rule for CloudPlaylist type") {
                val cloudPlaylist = Playlist(
                    id = 1L,
                    type = Playlist.Type.CloudPlaylist(
                        "http://example.com",
                        Playlist.SyncRule.ALWAYS_ADD
                    )
                )

                useCase.updateSyncRule(cloudPlaylist, Playlist.SyncRule.DELETE_IF_NOT_EXIST)

                val captor = argumentCaptor<Playlist>()
                verify(mockPlaylistRepo).update(any(), captor.capture())
                val updatedType = captor.firstValue.type as Playlist.Type.CloudPlaylist
                updatedType.syncRule shouldBe Playlist.SyncRule.DELETE_IF_NOT_EXIST
            }

            test("does nothing if not CloudPlaylist type") {
                val originalPlaylist = Playlist(id = 1L, type = Playlist.Type.Original)

                useCase.updateSyncRule(originalPlaylist, Playlist.SyncRule.ALWAYS_ADD)

                verify(mockPlaylistRepo, never()).update(any(), any())
            }
        }
    })

// テスト用のLocalDataSource実装
class TestPlaylistLocalDS(
    override val playlistRepository: PlaylistRepository<Unit>,
    override val videoRepository: VideoRepository<Unit>,
    override val alarmRepository: AlarmRepository<Unit>
) : DependsOnPlaylistRepository<Unit>,
    DependsOnVideoRepository<Unit>,
    DependsOnAlarmRepository<Unit>,
    DependsOnDataSource<Unit> {
    override val dataSource: DataSource<Unit> = object : DataSource<Unit> {
        override fun createExecutor(): Unit = Unit
    }
}