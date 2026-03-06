package net.turtton.ytalarm.util.extensions

import net.turtton.ytalarm.kernel.entity.Video

/**
 * Creates a copy of this video marked as failed import.
 * Note: [domain] is intentionally set to the full URL to display the source URL in the UI
 * when the import fails, as the actual domain cannot be determined at this point.
 */
fun Video.copyAsFailed(url: String) = copy(
    videoUrl = url,
    domain = url,
    state = Video.State.Failed(url)
)

/**
 * Collects videos whose state is [Video.State.Failed].
 */
fun List<Video>.collectGarbage(): List<Video> = filter {
    it.state is Video.State.Failed
}

val List<Video>.hasUpdatingVideo: Boolean
    get() = any { it.state.isUpdating() }