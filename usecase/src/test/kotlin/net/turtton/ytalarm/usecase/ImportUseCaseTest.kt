package net.turtton.ytalarm.usecase

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
                fakeVideoRepo.resetWith(existingVideo)

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
                fakeVideoRepo.resetWith(existingVideo)

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
                    pageUrl = "http://example.com/video",
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video(
                        fullTitle = "Test Video",
                        thumbnailUrl = "http://thumb.com",
                        streamUrl = "http://example.com/video.mp4"
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
                    pageUrl = "http://example.com/video",
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video(
                        fullTitle = "Test Video",
                        thumbnailUrl = "http://thumb.com",
                        streamUrl = "http://example.com/video.mp4"
                    )
                )
                val existingVideo = Video(
                    id = 42L,
                    videoId = "vid1",
                    domain = "example.com",
                    state = Video.State.Information()
                )
                fakeVideoRepo.resetWith(existingVideo)
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

            test("preserves webpage_url (not yt-dlp resolved stream url) in Video.pageUrl") {
                val webpageUrl = "https://soundcloud.com/osagechanmusic/ebrquaepj4uv"
                val streamUrl = "https://playback.media-streaming.soundcloud.cloud/abc/aac_160k/sig"
                val info = VideoInformation(
                    id = "2266842407",
                    title = "test",
                    pageUrl = webpageUrl,
                    domain = "soundcloud.com",
                    typeData = VideoInformation.Type.Video(
                        fullTitle = "test",
                        thumbnailUrl = "https://example.com/thumb.jpg",
                        streamUrl = streamUrl
                    )
                )
                fakeVideoInfoRepo.videoInfoResponses[webpageUrl] = Either.Right(info)

                val result = useCase.fetchAndImportVideo(webpageUrl)

                result shouldBe ImportResult.Success(1L)
                val saved = fakeVideoRepo.getFromIdSync(Unit, 1L)!!
                saved.pageUrl shouldBe webpageUrl
                saved.pageUrl shouldNotBe streamUrl
            }
        }

        context("importCloudPlaylist") {
            test("playlist URL: uses playlist title, not first video title") {
                val playlistUrl = "http://example.com/playlist"
                val entry1 = VideoInformation(
                    id = "vid1",
                    title = "First Song Title",
                    pageUrl = "http://example.com/vid1",
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video("First Song Title", "", "")
                )
                val entry2 = VideoInformation(
                    id = "vid2",
                    title = "Second Song Title",
                    pageUrl = "http://example.com/vid2",
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video("Second Song Title", "", "")
                )
                val playlistInfo = VideoInformation(
                    id = "playlist1",
                    title = "My Awesome Playlist",
                    pageUrl = playlistUrl,
                    domain = "example.com",
                    typeData = VideoInformation.Type.Playlist(listOf(entry1, entry2))
                )
                fakeVideoInfoRepo.videoInfoResponses[playlistUrl] = Either.Right(playlistInfo)

                val result = useCase.importCloudPlaylist(playlistUrl)

                result.isRight() shouldBe true
                val playlistId = result.getOrNull()!!
                val createdPlaylist =
                    fakePlaylistRepo.currentData.find { it.id == playlistId }
                createdPlaylist?.title shouldBe "My Awesome Playlist"
                fakeVideoRepo.currentData.size shouldBe 2
            }

            test("playlist URL with null title: falls back to default") {
                val playlistUrl = "http://example.com/playlist"
                val entry = VideoInformation(
                    id = "vid1",
                    title = "Song Title",
                    pageUrl = "http://example.com/vid1",
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video("Song Title", "", "")
                )
                val playlistInfo = VideoInformation(
                    id = "playlist1",
                    title = null,
                    pageUrl = playlistUrl,
                    domain = "example.com",
                    typeData = VideoInformation.Type.Playlist(listOf(entry))
                )
                fakeVideoInfoRepo.videoInfoResponses[playlistUrl] = Either.Right(playlistInfo)

                val result = useCase.importCloudPlaylist(playlistUrl)

                result.isRight() shouldBe true
                val playlistId = result.getOrNull()!!
                val createdPlaylist =
                    fakePlaylistRepo.currentData.find { it.id == playlistId }
                createdPlaylist?.title shouldBe "Playlist"
            }

            test("single video URL: uses video title as playlist name") {
                val pageUrl = "http://example.com/video"
                val videoInfo = VideoInformation(
                    id = "vid1",
                    title = "Single Video Title",
                    pageUrl = pageUrl,
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video(
                        "Single Video Title",
                        "http://thumb.com",
                        "http://example.com/video.mp4"
                    )
                )
                fakeVideoInfoRepo.videoInfoResponses[pageUrl] = Either.Right(videoInfo)

                val result = useCase.importCloudPlaylist(pageUrl)

                result.isRight() shouldBe true
                val playlistId = result.getOrNull()!!
                val createdPlaylist =
                    fakePlaylistRepo.currentData.find { it.id == playlistId }
                createdPlaylist?.title shouldBe "Single Video Title"
                createdPlaylist?.videos?.size shouldBe 1
            }

            test(
                "single video URL preserves webpage_url (not yt-dlp resolved stream url) in Video.pageUrl"
            ) {
                val webpageUrl = "https://soundcloud.com/osagechanmusic/ebrquaepj4uv"
                val streamUrl = "https://playback.media-streaming.soundcloud.cloud/abc/aac_160k/sig"
                val info = VideoInformation(
                    id = "2266842407",
                    title = "test",
                    pageUrl = webpageUrl,
                    domain = "soundcloud.com",
                    typeData = VideoInformation.Type.Video(
                        fullTitle = "test",
                        thumbnailUrl = "https://example.com/thumb.jpg",
                        streamUrl = streamUrl
                    )
                )
                fakeVideoInfoRepo.videoInfoResponses[webpageUrl] = Either.Right(info)

                val result = useCase.importCloudPlaylist(webpageUrl)

                result.isRight() shouldBe true
                val saved = fakeVideoRepo.getFromIdSync(Unit, 1L)!!
                saved.pageUrl shouldBe webpageUrl
                saved.pageUrl shouldNotBe streamUrl
            }

            test("network error: returns failure") {
                fakeVideoInfoRepo.videoInfoResponses["http://example.com/playlist"] =
                    Either.Left(VideoInfoError.NetworkError(RuntimeException("network")))

                val result =
                    useCase.importCloudPlaylist("http://example.com/playlist")

                result.isLeft() shouldBe true
                result.leftOrNull() shouldBe ImportResult.Failure.Network
            }

            test("playlist URL with blank title: falls back to default") {
                val playlistUrl = "http://example.com/playlist"
                val entry = VideoInformation(
                    id = "vid1",
                    title = "Song Title",
                    pageUrl = "http://example.com/vid1",
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video("Song Title", "", "")
                )
                val playlistInfo = VideoInformation(
                    id = "playlist1",
                    title = "   ",
                    pageUrl = playlistUrl,
                    domain = "example.com",
                    typeData = VideoInformation.Type.Playlist(listOf(entry))
                )
                fakeVideoInfoRepo.videoInfoResponses[playlistUrl] = Either.Right(playlistInfo)

                val result = useCase.importCloudPlaylist(playlistUrl)

                result.isRight() shouldBe true
                val playlistId = result.getOrNull()!!
                val createdPlaylist =
                    fakePlaylistRepo.currentData.find { it.id == playlistId }
                createdPlaylist?.title shouldBe "Playlist"
            }

            test("single video URL with blank title: falls back to fullTitle") {
                val pageUrl = "http://example.com/video"
                val videoInfo = VideoInformation(
                    id = "vid1",
                    title = "",
                    pageUrl = pageUrl,
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video(
                        "Full Title From TypeData",
                        "http://thumb.com",
                        "http://example.com/video.mp4"
                    )
                )
                fakeVideoInfoRepo.videoInfoResponses[pageUrl] = Either.Right(videoInfo)

                val result = useCase.importCloudPlaylist(pageUrl)

                result.isRight() shouldBe true
                val playlistId = result.getOrNull()!!
                val createdPlaylist =
                    fakePlaylistRepo.currentData.find { it.id == playlistId }
                createdPlaylist?.title shouldBe "Full Title From TypeData"
            }

            test("existingPlaylistId: reuses existing playlist") {
                val existingPlaylist = Playlist(
                    id = 10L,
                    title = "Old Title",
                    type = Playlist.Type.Importing
                )
                fakePlaylistRepo.resetWith(existingPlaylist)

                val playlistUrl = "http://example.com/playlist"
                val entry = VideoInformation(
                    id = "vid1",
                    title = "Song",
                    pageUrl = "http://example.com/vid1",
                    domain = "example.com",
                    typeData = VideoInformation.Type.Video("Song", "", "")
                )
                val playlistInfo = VideoInformation(
                    id = "playlist1",
                    title = "Correct Playlist Name",
                    pageUrl = playlistUrl,
                    domain = "example.com",
                    typeData = VideoInformation.Type.Playlist(listOf(entry))
                )
                fakeVideoInfoRepo.videoInfoResponses[playlistUrl] = Either.Right(playlistInfo)

                val result = useCase.importCloudPlaylist(playlistUrl, existingPlaylistId = 10L)

                result.isRight() shouldBe true
                result.getOrNull() shouldBe 10L
                val updatedPlaylist = fakePlaylistRepo.currentData.find { it.id == 10L }
                updatedPlaylist?.title shouldBe "Correct Playlist Name"
                fakePlaylistRepo.currentData.size shouldBe 1
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
                fakePlaylistRepo.resetWith(playlist)
                fakeVideoRepo.resetWith(
                    Video(id = 1L, videoId = "existing1", state = Video.State.Information()),
                    Video(id = 2L, videoId = "existing2", state = Video.State.Information())
                )
                val newVideo1 =
                    VideoInformation(
                        id = "vid3",
                        pageUrl = "http://example.com/vid3",
                        domain = "example.com",
                        typeData = VideoInformation.Type.Video("V3", "", "")
                    )

                useCase.syncCloudPlaylist(playlist, listOf(newVideo1))

                val updatedPlaylist = fakePlaylistRepo.currentData.find { it.id == 1L }
                updatedPlaylist?.videos?.containsAll(listOf(1L, 2L, 3L)) shouldBe true
            }

            test(
                "ALWAYS_ADD preserves webpage_url (not yt-dlp resolved stream url) in Video.pageUrl"
            ) {
                val playlist = Playlist(
                    id = 1L,
                    type = Playlist.Type.CloudPlaylist(
                        "https://soundcloud.com/example/sets/test",
                        Playlist.SyncRule.ALWAYS_ADD
                    )
                )
                val webpageUrl = "https://soundcloud.com/x/y"
                val streamUrl = "https://playback.media-streaming.soundcloud.cloud/signed?token=abc"
                fakePlaylistRepo.resetWith(playlist)

                val newVideo = VideoInformation(
                    id = "soundcloud-id",
                    title = "SoundCloud track",
                    pageUrl = webpageUrl,
                    domain = "soundcloud.com",
                    typeData = VideoInformation.Type.Video(
                        fullTitle = "SoundCloud track",
                        thumbnailUrl = "https://example.com/thumb.jpg",
                        streamUrl = streamUrl
                    )
                )

                useCase.syncCloudPlaylist(playlist, listOf(newVideo))

                val saved = fakeVideoRepo.getFromIdSync(Unit, 1L)!!
                saved.pageUrl shouldBe webpageUrl
                saved.pageUrl shouldNotBe streamUrl
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
                fakePlaylistRepo.resetWith(playlist)
                fakeVideoRepo.resetWith(
                    Video(id = 1L, videoId = "existing1", state = Video.State.Information()),
                    Video(id = 2L, videoId = "existing2", state = Video.State.Information())
                )
                val newVideo =
                    VideoInformation(
                        id = "vid3",
                        pageUrl = "http://example.com/vid3",
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