package net.turtton.ytalarm.util.extensions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldNotBe
import net.turtton.ytalarm.kernel.entity.Playlist

/**
 * Tests for Playlist.Thumbnail.toDrawableRes() extension function.
 */
@Suppress("UNUSED")
class TestThumbnailDisplay :
    FunSpec({
        context("Playlist.Thumbnail.toDrawableRes") {
            test("None returns non-null drawable resource ID") {
                val thumbnail: Playlist.Thumbnail = Playlist.Thumbnail.None
                thumbnail.toDrawableRes() shouldNotBe null
            }

            test("Video returns null (handled by Glide/Coil separately)") {
                val thumbnail: Playlist.Thumbnail = Playlist.Thumbnail.Video(id = 1L)
                thumbnail.toDrawableRes().shouldBeNull()
            }
        }
    })