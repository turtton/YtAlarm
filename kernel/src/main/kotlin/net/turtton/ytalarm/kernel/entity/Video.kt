package net.turtton.ytalarm.kernel.entity

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class Video(
    val id: Long = 0L,
    val videoId: String,
    val title: String = "No title",
    val thumbnailUrl: String = "",
    val videoUrl: String = "",
    val domain: String = "",
    val state: State,
    val creationDate: Instant = Clock.System.now()
) {
    sealed interface State {
        data object Importing : State

        data class Information(val isStreamable: Boolean = true) : State

        data object Downloading : State

        data class Downloaded(
            val internalLink: String,
            val fileSize: Long,
            val isStreamable: Boolean
        ) : State

        data class Failed(val sourceUrl: String) : State

        fun isUpdating(): Boolean = this is Importing || this is Downloading
    }
}