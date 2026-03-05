package net.turtton.ytalarm.datasource.mapper

import net.turtton.ytalarm.datasource.entity.PlaylistEntity
import net.turtton.ytalarm.kernel.entity.Playlist

fun PlaylistEntity.toDomain(): Playlist = Playlist(
    id = id,
    title = title,
    thumbnail = thumbnail.toDomain(),
    videos = videos,
    type = type.toDomain(),
    creationDate = creationDate.toKotlinInstant(),
    lastUpdated = lastUpdated.toKotlinInstant()
)

internal fun Playlist.toEntity(): PlaylistEntity = PlaylistEntity(
    id = id,
    title = title,
    thumbnail = thumbnail.toEntity(),
    videos = videos,
    type = type.toEntity(),
    creationDate = creationDate.toCalendar(),
    lastUpdated = lastUpdated.toCalendar()
)

private fun PlaylistEntity.Thumbnail.toDomain(): Playlist.Thumbnail = when (this) {
    is PlaylistEntity.Thumbnail.Video -> Playlist.Thumbnail.Video(id = id)
    is PlaylistEntity.Thumbnail.None -> Playlist.Thumbnail.None
}

private fun Playlist.Thumbnail.toEntity(): PlaylistEntity.Thumbnail = when (this) {
    is Playlist.Thumbnail.Video -> PlaylistEntity.Thumbnail.Video(id = id)
    is Playlist.Thumbnail.None -> PlaylistEntity.Thumbnail.None
}

private fun PlaylistEntity.Type.toDomain(): Playlist.Type = when (this) {
    is PlaylistEntity.Type.Importing -> Playlist.Type.Importing

    is PlaylistEntity.Type.Original -> Playlist.Type.Original

    is PlaylistEntity.Type.CloudPlaylist -> Playlist.Type.CloudPlaylist(
        url = url,
        syncRule = syncRule.toDomain()
    )
}

private fun Playlist.Type.toEntity(): PlaylistEntity.Type = when (this) {
    is Playlist.Type.Importing -> PlaylistEntity.Type.Importing

    is Playlist.Type.Original -> PlaylistEntity.Type.Original

    is Playlist.Type.CloudPlaylist -> PlaylistEntity.Type.CloudPlaylist(
        url = url,
        syncRule = syncRule.toEntity()
    )
}

private fun PlaylistEntity.SyncRule.toDomain(): Playlist.SyncRule = when (this) {
    PlaylistEntity.SyncRule.ALWAYS_ADD -> Playlist.SyncRule.ALWAYS_ADD
    PlaylistEntity.SyncRule.DELETE_IF_NOT_EXIST -> Playlist.SyncRule.DELETE_IF_NOT_EXIST
}

private fun Playlist.SyncRule.toEntity(): PlaylistEntity.SyncRule = when (this) {
    Playlist.SyncRule.ALWAYS_ADD -> PlaylistEntity.SyncRule.ALWAYS_ADD
    Playlist.SyncRule.DELETE_IF_NOT_EXIST -> PlaylistEntity.SyncRule.DELETE_IF_NOT_EXIST
}