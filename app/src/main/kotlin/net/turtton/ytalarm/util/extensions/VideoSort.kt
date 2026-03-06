package net.turtton.ytalarm.util.extensions

import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.util.order.VideoOrder

fun List<Video>.sorted(orderRule: VideoOrder, orderUp: Boolean): List<Video> {
    val sorted = when (orderRule) {
        VideoOrder.TITLE -> this.sortedBy { it.title }
        VideoOrder.CREATION_DATE -> this.sortedBy { it.creationDate.toEpochMilliseconds() }
    }
    return if (orderUp) sorted else sorted.reversed()
}