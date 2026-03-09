package net.turtton.ytalarm.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import arrow.core.Either
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.util.extensions.appSettings
import net.turtton.ytalarm.util.extensions.downloadStorageLimitBytes
import net.turtton.ytalarm.util.extensions.downloadWifiOnly
import java.util.concurrent.TimeUnit

const val VIDEO_FILE_DOWNLOAD_NOTIFICATION = "net.turtton.ytalarm.VideoFileDLNotification"

private const val MAX_RETRY_COUNT = 3
private const val BACKOFF_DELAY_SECONDS = 30L
private const val NOTIFICATION_ID = 2
private const val PROGRESS_MAX = 100

class VideoFileDownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val useCaseContainer = (applicationContext as YtApplication).dataContainerProvider
            .getUseCaseContainer()
        val videoId = inputData.getLong(KEY_VIDEO_ID, 0L)
        if (videoId == 0L) return Result.failure()

        if (!useCaseContainer.canDownload(
                applicationContext.appSettings.downloadStorageLimitBytes
            )
        ) {
            Log.w(WORKER_TAG, "Storage limit reached, skipping download for video $videoId")
            return Result.failure()
        }

        setForeground(createForegroundInfo(0))

        val result = useCaseContainer.downloadVideo(videoId) { progress ->
            val progressInt = progress.toInt().coerceIn(0, PROGRESS_MAX)
            setProgressAsync(
                Data.Builder()
                    .putInt(KEY_PROGRESS, progressInt)
                    .build()
            )
            setForegroundAsync(createForegroundInfo(progressInt))
        }

        return when {
            result == null -> {
                Log.w(
                    WORKER_TAG,
                    "Video $videoId not found or already handled (no download performed)"
                )
                Result.success()
            }

            result is Either.Left -> {
                Log.e(WORKER_TAG, "Download failed for video $videoId: ${result.value}")
                if (runAttemptCount < MAX_RETRY_COUNT) {
                    Result.retry()
                } else {
                    markVideoAsFailed(videoId)
                    Result.failure()
                }
            }

            else -> Result.success()
        }
    }

    private suspend fun markVideoAsFailed(videoId: Long) {
        val useCaseContainer = (applicationContext as YtApplication).dataContainerProvider
            .getUseCaseContainer()
        val video = useCaseContainer.getVideoByIdSync(videoId) ?: return
        useCaseContainer.updateVideo(
            video.copy(state = Video.State.Failed(video.videoUrl))
        )
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val title =
            applicationContext.getString(R.string.notification_download_video_file_title)
        val cancel = applicationContext.getString(R.string.cancel)
        val cancelIntent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)
        val notification =
            NotificationCompat.Builder(applicationContext, VIDEO_FILE_DOWNLOAD_NOTIFICATION)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(title)
                .setProgress(PROGRESS_MAX, progress, progress == 0)
                .addAction(R.drawable.ic_cancel, cancel, cancelIntent)
                .setSilent(true)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification.build())
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo(0)

    companion object {
        private const val WORKER_TAG = "VideoFileDownloadWorker"
        private const val KEY_VIDEO_ID = "VideoId"
        private const val KEY_PROGRESS = "Progress"

        fun registerWorker(context: Context, videoId: Long) {
            val settings = context.appSettings
            val data = Data.Builder()
                .putLong(KEY_VIDEO_ID, videoId)
                .build()

            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(
                    if (settings.downloadWifiOnly) {
                        androidx.work.NetworkType.UNMETERED
                    } else {
                        androidx.work.NetworkType.CONNECTED
                    }
                )
                .build()

            val request = OneTimeWorkRequestBuilder<VideoFileDownloadWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_SECONDS,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "$WORKER_TAG$videoId",
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun registerWorkers(context: Context, videoIds: List<Long>) {
            videoIds.forEach { videoId ->
                registerWorker(context, videoId)
            }
        }
    }
}