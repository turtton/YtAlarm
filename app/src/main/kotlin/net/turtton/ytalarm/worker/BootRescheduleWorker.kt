package net.turtton.ytalarm.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import net.turtton.ytalarm.YtApplication

class BootRescheduleWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val useCaseContainer = (applicationContext as YtApplication).dataContainerProvider
            .getUseCaseContainer()
        val enabledAlarms = useCaseContainer.getEnabledAlarms()
        Log.d(TAG, "Rescheduling alarms after boot: ${enabledAlarms.size} enabled")
        useCaseContainer.alarmScheduler.scheduleNextAlarm(enabledAlarms).onLeft { error ->
            Log.e(TAG, "Failed to reschedule alarms: $error")
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "BootRescheduleWorker"
        private const val WORKER_ID = "BootRescheduleWorker"

        fun registerWorker(context: Context): Operation {
            val request = OneTimeWorkRequestBuilder<BootRescheduleWorker>()
            return WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORKER_ID,
                    ExistingWorkPolicy.REPLACE,
                    request.build()
                )
        }
    }
}