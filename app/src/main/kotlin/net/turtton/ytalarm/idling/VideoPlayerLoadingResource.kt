package net.turtton.ytalarm.idling

import androidx.annotation.VisibleForTesting
import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicBoolean

class VideoPlayerLoadingResource : IdlingResource {
    @Volatile
    private var mCallback: IdlingResource.ResourceCallback? = null

    private val mIsIdleNow = AtomicBoolean(true)

    override fun getName(): String = javaClass.name

    override fun isIdleNow(): Boolean = mIsIdleNow.get()

    fun setIdleNow(isIdleNow: Boolean) {
        mIsIdleNow.set(isIdleNow)
        if (isIdleNow && mCallback != null) {
            mCallback?.onTransitionToIdle()
        }
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        mCallback = callback
    }
}

class VideoPlayerLoadingResourceController {
    var videoPlayerLoadingResource: VideoPlayerLoadingResource? = null
        private set

    @VisibleForTesting
    fun registerVideoPlayerLoadingResource(): VideoPlayerLoadingResource {
        if (videoPlayerLoadingResource == null) {
            videoPlayerLoadingResource = VideoPlayerLoadingResource()
        }
        return videoPlayerLoadingResource!!
    }
}

interface VideoPlayerLoadingResourceContainer {
    val videoPlayerLoadingResourceController: VideoPlayerLoadingResourceController
}