package net.turtton.ytalarm.ui.model

import kotlinx.datetime.Instant
import net.turtton.ytalarm.kernel.entity.Playlist
import kotlin.time.Clock

data class PlaylistUiModel(
    val id: Long,
    val title: String,
    val videoCount: Int,
    val videoIds: List<Long>,
    val thumbnailVideoId: Long?,
    val isImporting: Boolean,
    val isCloudPlaylist: Boolean,
    val isOriginal: Boolean,
    val creationDate: Instant,
    val lastUpdated: Instant
) {
    companion object {
        fun preview(id: Long = 1L, title: String = "My Playlist", videoCount: Int = 5) =
            PlaylistUiModel(
                id, title, videoCount, emptyList(), null,
                isImporting = false, isCloudPlaylist = false, isOriginal = true,
                Clock.System.now(), Clock.System.now()
            )
    }
}

fun Playlist.toUiModel(): PlaylistUiModel = PlaylistUiModel(
    id = id,
    title = title,
    videoCount = videos.size,
    videoIds = videos,
    thumbnailVideoId = (thumbnail as? Playlist.Thumbnail.Video)?.id,
    isImporting = type is Playlist.Type.Importing,
    isCloudPlaylist = type is Playlist.Type.CloudPlaylist,
    isOriginal = type is Playlist.Type.Original,
    creationDate = creationDate,
    lastUpdated = lastUpdated
)