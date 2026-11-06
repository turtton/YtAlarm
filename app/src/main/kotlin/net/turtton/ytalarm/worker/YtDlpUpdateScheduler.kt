package net.turtton.ytalarm.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import net.turtton.ytalarm.util.extensions.appSettings
import net.turtton.ytalarm.util.extensions.ytDlpAutoUpdate
import java.util.concurrent.TimeUnit

object YtDlpUpdateScheduler {
    internal const val UPDATE_BACKOFF_MINUTES = 30L
    internal const val WORK_NAME = "YtDlpPeriodicUpdate"
    internal const val AUTOMATIC = "automatic"

    fun reconcile(context: Context) = reconcile(
        WorkManager.getInstance(context),
        context.appSettings.ytDlpAutoUpdate
    )

    internal fun reconcile(manager: WorkManager, enabled: Boolean) {
        if (enabled) {
            manager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest()
            )
        } else {
            manager.cancelUniqueWork(WORK_NAME)
        }
    }

    internal fun periodicRequest(): PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<YtDlpUpdateWorker>(1, TimeUnit.DAYS)
            .setInputData(workDataOf(AUTOMATIC to true))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresCharging(true)
                    .setRequiresStorageNotLow(true)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, UPDATE_BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()
}