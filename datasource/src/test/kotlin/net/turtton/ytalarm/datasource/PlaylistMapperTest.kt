package net.turtton.ytalarm.datasource

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant
import net.turtton.ytalarm.datasource.entity.PlaylistEntity
import net.turtton.ytalarm.datasource.mapper.toDomain
import net.turtton.ytalarm.datasource.mapper.toEntity
import net.turtton.ytalarm.kernel.entity.Playlist
import java.util.Calendar

class PlaylistMapperTest :
    FunSpec({

        test("PlaylistEntity.toDomain - Original type, Video thumbnail") {
            val calendar = Calendar.getInstance().apply { timeInMillis = 1000L }
            val entity = PlaylistEntity(
                id = 1L,
                title = "My Playlist",
                thumbnail = PlaylistEntity.Thumbnail.Video(id = 42L),
                videos = listOf(1L, 2L, 3L),
                type = PlaylistEntity.Type.Original,
                creationDate = calendar,
                lastUpdated = calendar
            )
            val domain = entity.toDomain()
            domain.id shouldBe 1L
            domain.title shouldBe "My Playlist"
            domain.thumbnail shouldBe Playlist.Thumbnail.Video(id = 42L)
            domain.videos shouldBe listOf(1L, 2L, 3L)
            domain.type shouldBe Playlist.Type.Original
            domain.creationDate shouldBe Instant.fromEpochMilliseconds(1000L)
            domain.lastUpdated shouldBe Instant.fromEpochMilliseconds(1000L)
        }

        test("PlaylistEntity.toDomain - None thumbnail") {
            val entity = PlaylistEntity(
                id = 2L,
                title = "Empty Playlist",
                thumbnail = PlaylistEntity.Thumbnail.None,
                videos = emptyList(),
                type = PlaylistEntity.Type.Original,
                creationDate = Calendar.getInstance(),
                lastUpdated = Calendar.getInstance()
            )
            val domain = entity.toDomain()
            domain.thumbnail shouldBe Playlist.Thumbnail.None
        }

        test("PlaylistEntity.toDomain - Importing type") {
            val entity = PlaylistEntity(
                id = 3L,
                title = "Importing Playlist",
                thumbnail = PlaylistEntity.Thumbnail.None,
                videos = emptyList(),
                type = PlaylistEntity.Type.Importing,
                creationDate = Calendar.getInstance(),
                lastUpdated = Calendar.getInstance()
            )
            val domain = entity.toDomain()
            domain.type shouldBe Playlist.Type.Importing
        }

        test("PlaylistEntity.toDomain - CloudPlaylist type") {
            val entity = PlaylistEntity(
                id = 4L,
                title = "Cloud Playlist",
                thumbnail = PlaylistEntity.Thumbnail.None,
                videos = emptyList(),
                type = PlaylistEntity.Type.CloudPlaylist(
                    url = "https://youtube.com/playlist?list=xxx",
                    syncRule = PlaylistEntity.SyncRule.ALWAYS_ADD
                ),
                creationDate = Calendar.getInstance(),
                lastUpdated = Calendar.getInstance()
            )
            val domain = entity.toDomain()
            domain.type shouldBe Playlist.Type.CloudPlaylist(
                url = "https://youtube.com/playlist?list=xxx",
                syncRule = Playlist.SyncRule.ALWAYS_ADD
            )
        }

        test("PlaylistEntity.toDomain - SyncRule DELETE_IF_NOT_EXIST") {
            val entity = PlaylistEntity(
                id = 5L,
                title = "Delete Sync Playlist",
                thumbnail = PlaylistEntity.Thumbnail.None,
                videos = emptyList(),
                type = PlaylistEntity.Type.CloudPlaylist(
                    url = "https://example.com/list",
                    syncRule = PlaylistEntity.SyncRule.DELETE_IF_NOT_EXIST
                ),
                creationDate = Calendar.getInstance(),
                lastUpdated = Calendar.getInstance()
            )
            val domain = entity.toDomain()
            val cloudType = domain.type as Playlist.Type.CloudPlaylist
            cloudType.syncRule shouldBe Playlist.SyncRule.DELETE_IF_NOT_EXIST
        }

        test("Playlist.toEntity - roundtrip") {
            val instant = Instant.fromEpochMilliseconds(4000L)
            val playlist = Playlist(
                id = 6L,
                title = "Roundtrip Playlist",
                thumbnail = Playlist.Thumbnail.Video(id = 10L),
                videos = listOf(5L, 6L),
                type = Playlist.Type.CloudPlaylist(
                    url = "https://youtube.com/playlist?list=yyy",
                    syncRule = Playlist.SyncRule.DELETE_IF_NOT_EXIST
                ),
                creationDate = instant,
                lastUpdated = instant
            )
            val entity = playlist.toEntity()
            val backToDomain = entity.toDomain()

            backToDomain.id shouldBe playlist.id
            backToDomain.title shouldBe playlist.title
            backToDomain.thumbnail shouldBe playlist.thumbnail
            backToDomain.videos shouldBe playlist.videos
            backToDomain.type shouldBe playlist.type
            backToDomain.creationDate shouldBe playlist.creationDate
            backToDomain.lastUpdated shouldBe playlist.lastUpdated
        }

        test("Playlist.toEntity - None thumbnail converts to Entity None") {
            val playlist = Playlist(
                id = 7L,
                title = "No Thumb",
                thumbnail = Playlist.Thumbnail.None,
                videos = emptyList(),
                type = Playlist.Type.Original,
                creationDate = Instant.fromEpochMilliseconds(0),
                lastUpdated = Instant.fromEpochMilliseconds(0)
            )
            val entity = playlist.toEntity()
            entity.thumbnail shouldBe PlaylistEntity.Thumbnail.None
        }
    })