package net.turtton.ytalarm

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import net.turtton.ytalarm.di.DataContainerProvider
import net.turtton.ytalarm.di.DefaultDataContainerProvider
import net.turtton.ytalarm.idling.CoilIdlingResourceContainer
import net.turtton.ytalarm.idling.CoilIdlingResourceController

class YtApplication :
    Application(),
    Configuration.Provider,
    ImageLoaderFactory,
    CoilIdlingResourceContainer {
    val appCoroutineScope = CoroutineScope(SupervisorJob())

    val dataContainerProvider: DataContainerProvider by lazy { DefaultDataContainerProvider(this) }

    /**
     * YoutubeDLの初期化Job
     *
     * アプリ起動時に一度だけ実行される。再生処理は `await()` で完了を待つこと。
     * これによりAlarmActivityのような起動直後すぐに再生を開始するケースでも、
     * 初期化前に [YoutubeDL.getInfo] が呼ばれてハングする問題を回避する。
     */
    lateinit var ytDlInitJob: Deferred<Result<Unit>>
        private set

    override val coilIdlingResourceController = CoilIdlingResourceController()

    override val workManagerConfiguration: Configuration = Configuration.Builder()
        .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
        .build()

    override fun onCreate() {
        super.onCreate()
        ytDlInitJob = appCoroutineScope.async(Dispatchers.IO) {
            runCatching {
                YoutubeDL.getInstance().init(applicationContext)
            }.onFailure {
                Log.e(APP_TAG, "YtDL initialization failed", it)
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        val builder = ImageLoader.Builder(this)
        // テスト時のみEventListenerを設定
        coilIdlingResourceController.getEventListener()?.let {
            builder.eventListener(it)
        }
        return builder.build()
    }

    companion object {
        const val APP_TAG = "YtApplication"

        val Application.dataContainerProvider: DataContainerProvider
            get() = (this as YtApplication).dataContainerProvider

        val Application.ytDlInitJob: Deferred<Result<Unit>>
            get() = (this as YtApplication).ytDlInitJob
    }
}