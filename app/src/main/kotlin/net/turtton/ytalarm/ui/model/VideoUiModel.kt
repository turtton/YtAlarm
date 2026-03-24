package net.turtton.ytalarm.ui.model

import net.turtton.ytalarm.kernel.entity.Video
import kotlin.time.Clock
import kotlin.time.Instant

data class VideoUiModel(
    val id: Long,
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val domain: String,
    val isFailed: Boolean,
    val isUpdating: Boolean,
    val isDownloaded: Boolean,
    val creationDate: Instant
) {
    companion object {
        fun preview(
            id: Long = 1L,
            videoId: String = "test",
            title: String = "Sample Video",
            thumbnailUrl: String = "",
            domain: String = "youtube.com",
            isFailed: Boolean = false,
            isUpdating: Boolean = false,
            isDownloaded: Boolean = false
        ) = VideoUiModel(
            id,
            videoId,
            title,
            thumbnailUrl,
            domain,
            isFailed,
            isUpdating,
            isDownloaded,
            Clock.System.now()
        )
    }
}

fun Video.toUiModel(): VideoUiModel = VideoUiModel(
    id = id,
    videoId = videoId,
    title = title,
    thumbnailUrl = thumbnailUrl,
    domain = domain,
    isFailed = state is Video.State.Failed,
    isUpdating = state.isUpdating(),
    isDownloaded = state is Video.State.Downloaded,
    creationDate = creationDate
)