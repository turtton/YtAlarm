package net.turtton.ytalarm.worker

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.util.extensions.compatPendingIntentFlag
import java.util.*

const val SNOOZE_NOTIFICATION = "net.turtton.ytalarm.SnoozeNotification"

class SnoozeRemoveWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val targetId = inputData.getLong(KEY_TARGET, -1)
        if (targetId == -1L) {
            return Result.failure()
        }

        val useCaseContainer = (applicationContext as YtApplication).dataContainerProvider
            .getUseCaseContainer()
        val target = useCaseContainer.getAlarmById(targetId) ?: return Result.success()
        useCaseContainer.deleteAlarmAndReschedule(target)

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

class UpdateSnoozeNotifyWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val useCaseContainer = (applicationContext as YtApplication).dataContainerProvider
            .getUseCaseContainer()
        val snoozeAlarms = useCaseContainer.getMatchedAlarms(Alarm.RepeatType.Snooze)
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        if (snoozeAlarms.isEmpty()) {
            notificationManager.cancel(NOTIFICATION_ID)
            return Result.success()
        }

        val now = Calendar.getInstance()
        val (nextSnooze, nextSnoozeCalendar) = snoozeAlarms
            .associateWith { alarm ->
                Calendar.getInstance().also { cal ->
                    cal.timeInMillis = now.timeInMillis
                    cal.set(Calendar.HOUR_OF_DAY, alarm.hour)
                    cal.set(Calendar.MINUTE, alarm.minute)
                    cal.set(Calendar.SECOND, 0)
                    if (cal.timeInMillis <= now.timeInMillis) {
                        cal.add(Calendar.DATE, 1)
                    }
                }
            }
            .minByOrNull { (_, cal) -> cal.timeInMillis }
            ?.toPair()
            ?: run {
                notificationManager.cancel(NOTIFICATION_ID)
                return Result.success()
            }
        val diffMillis = nextSnoozeCalendar.timeInMillis - now.timeInMillis
        val minute = (diffMillis / MILLIS_PER_MINUTE).toInt().coerceAtLeast(0)

        val title = applicationContext.getString(R.string.notification_snooze_title)
        val description = if (minute == 0) {
            applicationContext.getString(R.string.notification_snooze_remain_soon)
        } else {
            applicationContext.resources
                .getQuantityString(R.plurals.notification_snooze_remain, minute, minute)
        }
        val cancelText = applicationContext.getString(R.string.cancel)

        val removeIntent = SnoozeRemoveReceiver.getIntent(applicationContext, nextSnooze.id)

        val builder =
            NotificationCompat.Builder(applicationContext, SNOOZE_NOTIFICATION)
                .setSmallIcon(R.drawable.ic_snooze)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(R.drawable.ic_trash, cancelText, removeIntent)

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }

        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 0
        private const val WORKER_ID = "UpdateSnoozeNotifyWorker"
        private const val MILLIS_PER_MINUTE = 60_000L

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