package net.turtton.ytalarm

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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

    override val coilIdlingResourceController = CoilIdlingResourceController()

    override val workManagerConfiguration: Configuration = Configuration.Builder()
        .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
        .build()

    override fun newImageLoader(): ImageLoader {
        val builder = ImageLoader.Builder(this)
        // テスト時のみEventListenerを設定
        coilIdlingResourceController.getEventListener()?.let {
            builder.eventListener(it)
        }
        return builder.build()
    }

    companion object {
        val Application.dataContainerProvider: DataContainerProvider
            get() = (this as YtApplication).dataContainerProvider
    }
}