package net.turtton.ytalarm.worker

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDL.UpdateChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.R
import net.turtton.ytalarm.util.extensions.appSettings
import net.turtton.ytalarm.util.extensions.ytDlpUpdateChannel

const val YTDLP_UPDATE_NOTIFICATION = "net.turtton.ytalarm.YtDlpUpdateNotification"

class YtDlpUpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result = try {
        withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().init(applicationContext)
            val channelName = applicationContext.appSettings.ytDlpUpdateChannel
            val channel = when (channelName) {
                "NIGHTLY" -> UpdateChannel.NIGHTLY
                else -> UpdateChannel.STABLE
            }
            val status = YoutubeDL.getInstance()
                .updateYoutubeDL(applicationContext, channel)
            Log.i(TAG, "YtDL update status: $status")
        }
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "YtDL update failed", e)
        showUpdateFailedNotification()
        Result.failure()
    }

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

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "YtDlpUpdateWorker"
        private const val WORKER_ID = "YtDlpUpdateWorker"
        private const val NOTIFICATION_ID = 1001

        fun registerWorker(context: Context): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<YtDlpUpdateWorker>()
                .setConstraints(constraints)
                .build()
            return WorkManager.getInstance(context)
                .enqueueUniqueWork(WORKER_ID, ExistingWorkPolicy.KEEP, request)
        }
    }
}