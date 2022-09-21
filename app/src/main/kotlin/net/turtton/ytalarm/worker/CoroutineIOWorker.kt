package net.turtton.ytalarm.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import net.turtton.ytalarm.DataRepository
import net.turtton.ytalarm.database.AppDatabase

abstract class CoroutineIOWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    @Suppress("DEPRECATION")
    private val database by lazy {
        AppDatabase.getDataBase(appContext, CoroutineScope(coroutineContext))
    }
    val repository by lazy { DataRepository(database) }
}