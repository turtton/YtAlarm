package net.turtton.ytalarm.util.extensions

import net.turtton.ytalarm.R
import net.turtton.ytalarm.database.structure.Playlist
import java.util.*

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

fun Playlist.updateDate(): Playlist = copy(lastUpdated = Calendar.getInstance())

fun List<Playlist>.updateDate(): List<Playlist> = map { it.updateDate() }

fun List<Playlist>.deleteVideo(videoId: Long): List<Playlist> = map {
    val newVideoList = it.videos.toMutableSet()
    newVideoList.remove(videoId)
    it.copy(videos = newVideoList.toList())
}

fun List<Playlist>.deleteVideos(videoIds: Collection<Long>): List<Playlist> = map { playlist ->
    val newVideos = playlist.videos.toMutableList()
    newVideos.removeAll(videoIds)
    playlist.copy(videos = newVideos)
}

fun List<Playlist>.insertVideos(videoIds: List<Long>): List<Playlist> = map {
    val newVideoSet = it.videos.toMutableSet()
    newVideoSet += videoIds
    it.copy(videos = newVideoSet.toList())
}