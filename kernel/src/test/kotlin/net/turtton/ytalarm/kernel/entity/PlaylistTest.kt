package net.turtton.ytalarm.kernel.entity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PlaylistTest :
    FunSpec({
        test("Playlist default values are set correctly") {
            val playlist = Playlist()
            playlist.id shouldBe 0L
            playlist.title shouldBe "Playlist"
            playlist.thumbnail shouldBe Playlist.Thumbnail.None
            playlist.videos shouldBe emptyList()
            playlist.type shouldBe Playlist.Type.Original
        }

        test("Thumbnail.Video holds video id") {
            val thumbnail = Playlist.Thumbnail.Video(42L)
            thumbnail.id shouldBe 42L
            thumbnail.shouldBeInstanceOf<Playlist.Thumbnail.Video>()
        }

        test("Thumbnail.None is singleton") {
            Playlist.Thumbnail.None shouldBe Playlist.Thumbnail.None
        }

        test("Type.CloudPlaylist holds url and syncRule") {
            val type = Playlist.Type.CloudPlaylist(
                "http://example.com/playlist",
                Playlist.SyncRule.ALWAYS_ADD
            )
            type.url shouldBe "http://example.com/playlist"
            type.syncRule shouldBe Playlist.SyncRule.ALWAYS_ADD
        }

        test("SyncRule has ALWAYS_ADD and DELETE_IF_NOT_EXIST") {
            Playlist.SyncRule.values().toList() shouldBe listOf(
                Playlist.SyncRule.ALWAYS_ADD,
                Playlist.SyncRule.DELETE_IF_NOT_EXIST
            )
        }
    })