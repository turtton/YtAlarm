package net.turtton.ytalarm.kernel.entity

import kotlinx.datetime.Instant
import kotlin.time.Clock

data class Playlist(
    val id: Long = 0L,
    val title: String = "Playlist",
    val thumbnail: Thumbnail = Thumbnail.None,
    val videos: List<Long> = emptyList(),
    val type: Type = Type.Original,
    val creationDate: Instant = Clock.System.now(),
    val lastUpdated: Instant = Clock.System.now()
) {
    sealed interface Thumbnail {
        data class Video(val id: Long) : Thumbnail

        data object None : Thumbnail
    }

    sealed interface Type {
        data object Importing : Type

        data object Original : Type

        data class CloudPlaylist(val url: String, val syncRule: SyncRule) : Type
    }

    enum class SyncRule {
        ALWAYS_ADD,
        DELETE_IF_NOT_EXIST
    }
}