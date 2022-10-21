package net.turtton.ytalarm.util.extensions

import net.turtton.ytalarm.R
import net.turtton.ytalarm.database.structure.Playlist

fun createImportingPlaylist() = Playlist(
    type = Playlist.Type.Importing,
    thumbnail = Playlist.Thumbnail.Drawable(R.drawable.ic_download)
)

fun Playlist.insertVideos(videoIds: Set<Long>): Playlist {
    val newVideoSet = videos.toMutableSet()
    newVideoSet += videoIds
    return copy(videos = newVideoSet.toList())
}

fun Playlist.updateThumbnail(): Playlist? {
    val targetVideoId = videos.firstOrNull() ?: return null
    return copy(thumbnail = Playlist.Thumbnail.Video(targetVideoId))
}

fun List<Playlist>.deleteVideo(videoId: Long): List<Playlist> = map {
    val newVideoList = it.videos.toMutableSet()
    newVideoList.remove(videoId)
    it.copy(videos = newVideoList.toList())
}

fun List<Playlist>.insertVideos(videoIds: List<Long>): List<Playlist> = map {
    val newVideoSet = it.videos.toMutableSet()
    newVideoSet += videoIds
    it.copy(videos = newVideoSet.toList())
}