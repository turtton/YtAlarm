package net.turtton.ytalarm.util.extensions

import net.turtton.ytalarm.database.structure.Playlist

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