package net.turtton.ytalarm.usecase

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.entity.Playlist
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.kernel.fake.FakeAlarmRepository
import net.turtton.ytalarm.kernel.fake.FakePlaylistRepository
import net.turtton.ytalarm.kernel.fake.FakeVideoRepository
import net.turtton.ytalarm.usecase.fake.FakeLocalDataSourceContainer

class PlaylistUseCaseTest :
    FunSpec({
        lateinit var fakePlaylistRepo: FakePlaylistRepository
        lateinit var fakeVideoRepo: FakeVideoRepository
        lateinit var fakeAlarmRepo: FakeAlarmRepository
        lateinit var useCase: PlaylistUseCase<Unit, FakeLocalDataSourceContainer>

        beforeEach {
            fakePlaylistRepo = FakePlaylistRepository()
            fakeVideoRepo = FakeVideoRepository()
            fakeAlarmRepo = FakeAlarmRepository()
            val ds = FakeLocalDataSourceContainer(
                alarmRepository = fakeAlarmRepo,
                videoRepository = fakeVideoRepo,
                playlistRepository = fakePlaylistRepo
            )
            useCase = object : PlaylistUseCase<Unit, FakeLocalDataSourceContainer> {
                override val localDataSource: FakeLocalDataSourceContainer = ds
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
                fakeVideoRepo.seed(video)
                fakePlaylistRepo.seed(playlist)

                useCase.validateAndUpdateThumbnails()

                fakePlaylistRepo.currentData.first().thumbnail shouldBe Playlist.Thumbnail.Video(1L)
            }

            test("invalid thumbnail (video missing): update to None") {
                val playlist = Playlist(
                    id = 1L,
                    thumbnail = Playlist.Thumbnail.Video(99L),
                    videos = listOf(99L)
                )
                fakePlaylistRepo.seed(playlist)

                useCase.validateAndUpdateThumbnails()

                fakePlaylistRepo.currentData.first().thumbnail shouldBe Playlist.Thumbnail.None
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
                fakeVideoRepo.seed(failedVideo, goodVideo)
                fakePlaylistRepo.seed(playlist)

                useCase.validateAndUpdateThumbnails()

                fakePlaylistRepo.currentData.first().thumbnail shouldBe Playlist.Thumbnail.Video(2L)
            }
        }

        context("safeDeletePlaylist") {
            test("deletes playlist and removes reference from alarms") {
                val playlist = Playlist(id = 1L)
                val alarm = Alarm(id = 10L, playlistIds = listOf(1L, 2L))
                fakeAlarmRepo.seed(alarm)
                fakePlaylistRepo.seed(playlist)

                useCase.safeDeletePlaylist(playlist)

                fakePlaylistRepo.currentData shouldBe emptyList()
                fakeAlarmRepo.currentData.first().playlistIds shouldBe listOf(2L)
            }

            test("deletes playlist with no alarm references") {
                val playlist = Playlist(id = 1L)
                fakePlaylistRepo.seed(playlist)

                useCase.safeDeletePlaylist(playlist)

                fakePlaylistRepo.currentData shouldBe emptyList()
                fakeAlarmRepo.currentData shouldBe emptyList()
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
                fakeVideoRepo.seed(video1)
                fakeVideoRepo.seed(video2)
                fakePlaylistRepo.seed(playlist)

                useCase.removeVideosFromPlaylist(playlist, listOf(1L))

                val updatedPlaylist = fakePlaylistRepo.currentData.first()
                updatedPlaylist.videos shouldBe listOf(2L)
                updatedPlaylist.thumbnail shouldBe Playlist.Thumbnail.Video(2L)
            }
        }

        context("setPlaylistThumbnail") {
            test("sets thumbnail to specified video") {
                val playlist = Playlist(id = 1L)
                fakePlaylistRepo.seed(playlist)

                useCase.setPlaylistThumbnail(playlist, Playlist.Thumbnail.Video(5L))

                val updatedPlaylist = fakePlaylistRepo.currentData.first()
                updatedPlaylist.thumbnail shouldBe Playlist.Thumbnail.Video(5L)
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
                fakePlaylistRepo.seed(cloudPlaylist)

                useCase.updateSyncRule(cloudPlaylist, Playlist.SyncRule.DELETE_IF_NOT_EXIST)

                val updatedPlaylist = fakePlaylistRepo.currentData.first()
                val updatedType = updatedPlaylist.type as Playlist.Type.CloudPlaylist
                updatedType.syncRule shouldBe Playlist.SyncRule.DELETE_IF_NOT_EXIST
            }

            test("does nothing if not CloudPlaylist type") {
                val originalPlaylist = Playlist(id = 1L, type = Playlist.Type.Original)
                fakePlaylistRepo.seed(originalPlaylist)
                val originalThumbnail = originalPlaylist.thumbnail

                useCase.updateSyncRule(originalPlaylist, Playlist.SyncRule.ALWAYS_ADD)

                fakePlaylistRepo.currentData.first().type shouldBe Playlist.Type.Original
                fakePlaylistRepo.currentData.first().thumbnail shouldBe originalThumbnail
            }
        }
    })