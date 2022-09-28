package net.turtton.ytalarm.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.R
import net.turtton.ytalarm.util.extensions.compatPendingIntentFlag
import net.turtton.ytalarm.util.extensions.pickNearestTime
import net.turtton.ytalarm.worker.CoroutineIOWorker
import java.util.*

const val SNOOZE_NOTIFICATION = "net.turtton.ytalarm.SnoozeNotification"

class SnoozeRemoveWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineIOWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val targetId = inputData.getLong(KEY_TARGET, -1)
        if (targetId == -1L) {
            return Result.failure()
        }

        withContext(Dispatchers.IO) {
            val target = repository.getAlarmFromIdSync(targetId)
            repository.delete(target)
        }

        UpdateSnoozeNotifyWorker.registerWorker(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORKER_ID = "SnoozeRemoveWorker"
        private const val KEY_TARGET = "RemoveTarget"

        fun registerWorker(context: Context, removeTargetId: Long): Operation {
            val request = OneTimeWorkRequestBuilder<SnoozeRemoveWorker>()
                .setInputData(workDataOf(KEY_TARGET to removeTargetId))
            return WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORKER_ID,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request.build()
                )
        }
    }
}

class UpdateSnoozeNotifyWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineIOWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val snoozeAlarms = withContext(Dispatchers.IO) {
            repository.getMatchedAlarmSync(RepeatType.Snooze)
        }
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        if (snoozeAlarms.isEmpty()) {
            notificationManager.cancel(NOTIFICATION_ID)
            return Result.success()
        }

        val nextSnooze = snoozeAlarms.pickNearestTime(Calendar.getInstance())!!.first

        val now = Calendar.getInstance()
        val minute = nextSnooze.minute - now[Calendar.MINUTE]

        val title = applicationContext.getString(R.string.notification_snooze_title)
        val description = applicationContext.resources
            .getQuantityString(R.plurals.notification_snooze_remain, minute, minute)
        val removeText = applicationContext.getString(R.string.notification_snooze_remove)

        val removeIntent = SnoozeRemoveReceiver.getIntent(applicationContext, nextSnooze.id!!)

        val builder =
            NotificationCompat.Builder(applicationContext, SNOOZE_NOTIFICATION)
                .setSmallIcon(R.drawable.ic_snooze)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(R.drawable.ic_trash, removeText, removeIntent)

        notificationManager
            .notify(NOTIFICATION_ID, builder.build())

        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 0
        private const val WORKER_ID = "UpdateSnoozeNotifyWorker"

        fun registerWorker(context: Context): Operation {
            val updateWorkRequest = OneTimeWorkRequestBuilder<UpdateSnoozeNotifyWorker>()
            return WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORKER_ID,
                    ExistingWorkPolicy.REPLACE,
                    updateWorkRequest.build()
                )
        }
    }
}

class SnoozeRemoveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            val id = intent.getLongExtra(FLAG_SNOOZE_ID, -1)
            SnoozeRemoveWorker.registerWorker(context, id)
        }
    }

    companion object {
        private const val FLAG_SNOOZE_ID = "SnoozeId"

        fun getIntent(context: Context, alarmId: Long): PendingIntent {
            val intent = Intent(context, SnoozeRemoveReceiver::class.java)
                .putExtra(FLAG_SNOOZE_ID, alarmId)
            return PendingIntent
                .getBroadcast(context, 0, intent, compatPendingIntentFlag)
        }
    }
}