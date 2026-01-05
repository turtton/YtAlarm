package net.turtton.ytalarm.idling

import androidx.annotation.VisibleForTesting
import androidx.test.espresso.IdlingResource
import coil.EventListener
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.util.concurrent.atomic.AtomicInteger

/**
 * Coil画像読み込み用のIdlingResource
 *
 * Coilの画像読み込みを追跡し、すべての読み込みが完了するまで待機できるようにする。
 * テスト時に使用して、サムネイル画像の読み込み完了を待ってからスクリーンショットを取得する。
 */
class CoilIdlingResource :
    IdlingResource,
    EventListener {
    @Volatile
    private var callback: IdlingResource.ResourceCallback? = null

    private val activeRequests = AtomicInteger(0)

    override fun getName(): String = "CoilIdlingResource"

    override fun isIdleNow(): Boolean = activeRequests.get() == 0

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
    }

    override fun onStart(request: ImageRequest) {
        activeRequests.incrementAndGet()
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
        decrementAndNotify()
    }

    override fun onError(request: ImageRequest, result: ErrorResult) {
        decrementAndNotify()
    }

    override fun onCancel(request: ImageRequest) {
        decrementAndNotify()
    }

    private fun decrementAndNotify() {
        val count = activeRequests.decrementAndGet()
        if (count == 0) {
            callback?.onTransitionToIdle()
        }
    }

    /**
     * 現在進行中のリクエスト数を取得（デバッグ用）
     */
    @VisibleForTesting
    fun getActiveRequestCount(): Int = activeRequests.get()
}

/**
 * CoilIdlingResourceを管理するコントローラー
 *
 * アプリ全体で単一のCoilIdlingResourceインスタンスを管理し、
 * ImageLoaderとテストコードの両方からアクセスできるようにする。
 */
class CoilIdlingResourceController {
    var coilIdlingResource: CoilIdlingResource? = null
        private set

    /**
     * テスト用にCoilIdlingResourceを登録
     *
     * @return 登録されたCoilIdlingResource
     */
    @VisibleForTesting
    fun registerCoilIdlingResource(): CoilIdlingResource {
        if (coilIdlingResource == null) {
            coilIdlingResource = CoilIdlingResource()
        }
        return coilIdlingResource!!
    }

    /**
     * CoilIdlingResourceを取得（存在する場合のみ）
     *
     * @return CoilIdlingResourceまたはnull
     */
    fun getEventListener(): EventListener? = coilIdlingResource
}

/**
 * CoilIdlingResourceControllerを保持するインターフェース
 */
interface CoilIdlingResourceContainer {
    val coilIdlingResourceController: CoilIdlingResourceController
}