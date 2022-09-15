package net.turtton.ytalarm

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.turtton.ytalarm.database.AppDatabase

class YtApplication : Application() {
    val appCoroutineScope = CoroutineScope(SupervisorJob())
    val database by lazy { AppDatabase.getDataBase(this, appCoroutineScope) }
    val repository by lazy { DataRepository(database) }

    companion object {
        val Application.repository: DataRepository
            get() = (this as YtApplication).repository
    }
}