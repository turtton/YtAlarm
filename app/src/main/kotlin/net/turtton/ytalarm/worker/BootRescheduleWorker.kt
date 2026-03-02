package net.turtton.ytalarm.worker

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.util.updateAlarmSchedule

class BootRescheduleWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineIOWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) {
            val alarms = repository.getAllAlarmsSync()
            Log.d(TAG, "Rescheduling alarms after boot: ${alarms.count { it.isEnable }} enabled")
            updateAlarmSchedule(applicationContext, alarms)
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