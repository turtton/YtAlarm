package net.turtton.ytalarm.worker

import android.app.AlarmManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yausername.youtubedl_android.YoutubeDL.UpdateChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.datasource.remote.YtDlpExecutor
import net.turtton.ytalarm.platform.AlarmPlaybackState
import net.turtton.ytalarm.util.extensions.appSettings
import net.turtton.ytalarm.util.extensions.ytDlpAutoUpdate
import net.turtton.ytalarm.util.extensions.ytDlpUpdateChannel
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Instant

const val YTDLP_UPDATE_NOTIFICATION = "net.turtton.ytalarm.YtDlpUpdateNotification"

class YtDlpUpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result {
        return try {
            val automatic = inputData.getBoolean(YtDlpUpdateScheduler.AUTOMATIC, false)
            if (automatic &&
                !applicationContext.appSettings.ytDlpAutoUpdate
            ) {
                return Result.success()
            }
            val app = applicationContext as YtApplication
            val alarms = withContext(Dispatchers.IO) {
                app.dataContainerProvider.getUseCaseContainer().getAllAlarmsSync()
            }
            val manager = applicationContext.getSystemService(AlarmManager::class.java)
            val shouldDefer = {
                shouldDeferYtDlpUpdate(
                    AlarmPlaybackState.isRinging(),
                    alarms,
                    Clock.System.now(),
                    TimeZone.currentSystemDefault(),
                    manager?.nextAlarmClock?.triggerTime?.let(Instant::fromEpochMilliseconds)
                ) || (automatic && !applicationContext.appSettings.ytDlpAutoUpdate)
            }
            if (shouldDefer()) return Result.retry()
            if (app.ytDlInitJob.await().isFailure) return Result.retry()
            // Periodic work stays a normal background worker; no background FGS launch required.
            if (!automatic) setForeground(createForegroundInfo())
            val status = withContext(Dispatchers.IO) {
                val channelName = applicationContext.appSettings.ytDlpUpdateChannel
                val channel = when (channelName) {
                    "NIGHTLY" -> UpdateChannel.NIGHTLY
                    else -> UpdateChannel.STABLE
                }
                YtDlpExecutor.update(applicationContext, channel, shouldDefer)
            }
            if (status == null) return Result.retry()
            Log.i(TAG, "YtDL update status: $status")
            NotificationManagerCompat.from(applicationContext)
                .cancel(FAILURE_NOTIFICATION_ID)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "YtDL update failed", e)
            showUpdateFailedNotification()
            Result.retry()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val title =
            applicationContext.getString(R.string.notification_ytdlp_updating_title)
        val notification =
            NotificationCompat.Builder(applicationContext, YTDLP_UPDATE_NOTIFICATION)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(title)
                .setProgress(0, 0, true)
                .setSilent(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                FOREGROUND_NOTIFICATION_ID,
                notification.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(FOREGROUND_NOTIFICATION_ID, notification.build())
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo()

    private fun showUpdateFailedNotification() {
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification =
            NotificationCompat.Builder(applicationContext, YTDLP_UPDATE_NOTIFICATION)
                .setSmallIcon(R.drawable.ic_error)
                .setContentTitle(
                    applicationContext.getString(
                        R.string.notification_ytdlp_update_failed_title
                    )
                )
                .setContentText(
                    applicationContext.getString(
                        R.string.notification_ytdlp_update_failed_description
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

        notificationManager.notify(FAILURE_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val UPDATE_BACKOFF_MINUTES = 30L
        private const val TAG = "YtDlpUpdateWorker"
        private const val WORKER_ID = "YtDlpUpdateWorker"
        private const val FOREGROUND_NOTIFICATION_ID = 1001
        private const val FAILURE_NOTIFICATION_ID = 1002

        fun registerWorker(context: Context): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<YtDlpUpdateWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    UPDATE_BACKOFF_MINUTES,
                    TimeUnit.MINUTES
                )
                .build()
            return WorkManager.getInstance(context)
                .enqueueUniqueWork(WORKER_ID, ExistingWorkPolicy.KEEP, request)
        }

        fun observeWorkInfo(context: Context): Flow<List<WorkInfo>> =
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(WORKER_ID)
    }
}