package net.turtton.ytalarm.platform

/** AlarmActivity remains active while stopped/behind the lock screen, until destroyed. */
object AlarmPlaybackState {
    private val activities = mutableSetOf<Any>()

    @Synchronized
    fun started(owner: Any) {
        activities.add(owner)
    }

    @Synchronized
    fun stopped(owner: Any) {
        activities.remove(owner)
    }

    @Synchronized
    fun isRinging(): Boolean = activities.isNotEmpty()
}