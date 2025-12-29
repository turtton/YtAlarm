package net.turtton.ytalarm

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.turtton.ytalarm.database.AppDatabase

class YtApplication :
    Application(),
    Configuration.Provider {
    val appCoroutineScope = CoroutineScope(SupervisorJob())
    val database by lazy { AppDatabase.getDataBase(this, appCoroutineScope) }
    val repository by lazy { DataRepository(database) }

    override val workManagerConfiguration: Configuration = Configuration.Builder()
        .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
        .build()

    companion object {
        val Application.repository: DataRepository
            get() = (this as YtApplication).repository
    }
}