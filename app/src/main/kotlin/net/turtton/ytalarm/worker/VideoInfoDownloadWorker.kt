package net.turtton.ytalarm.worker

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.runCatching
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.turtton.ytalarm.R
import net.turtton.ytalarm.structure.Video
import net.turtton.ytalarm.util.VideoInformation

const val VIDEO_DOWNLOAD_NOTIFICATION = "net.turtton.ytalarm.VideoDLNotification"

class VideoInfoDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineIOWorker(appContext, workerParams) {
    private val json = Json { ignoreUnknownKeys = true }

    private val triedCount = MutableStateFlow(0)

    override suspend fun doWork(): Result {
        val targetUrl = inputData.getString(KEY_URL) ?: return Result.failure()

        val request = YoutubeDLRequest(targetUrl)
            .addOption("--dump-single-json")
            .addOption("-f", "b")

        val (videos, type) = download(request)
        repository.insert(videos)

        val targetPlaylist = inputData.getLong(KEY_PLAYLIST, -1)
        if (targetPlaylist != -1L) {
            val playlist = repository.getPlaylistFromIdSync(targetPlaylist)
            val newList = (playlist.videos + videos.map { it.id }).distinct()
            val new = when (type) {
                is Type.Video -> playlist.copy(videos = newList)
                is Type.Playlist -> playlist.copy(videos = newList, originUrl = type.url)
            }
            repository.update(new)
        }
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val title = applicationContext.getString(R.string.notification_download_video_info_title)
        val cancel = applicationContext.getString(R.string.notification_download_video_info_cancel)
        val cancelIntent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)
        val notification =
            NotificationCompat.Builder(applicationContext, VIDEO_DOWNLOAD_NOTIFICATION)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(title)
                .setProgress(1, 1, true)
                .addAction(R.drawable.ic_cancel, cancel, cancelIntent)
                .setSilent(true)

        val count = triedCount.value
        if (count > 0) {
            val text = applicationContext
                .getString(R.string.notification_download_video_info_retry, count)
            notification.setContentText(text)
        }

        return ForegroundInfo(NOTIFICATION_ID, notification.build())
    }

    private suspend fun download(request: YoutubeDLRequest): Pair<List<Video>, Type> = runCatching {
        YoutubeDL.getInstance().execute(request) { _, _, _ -> }
    }.andThen {
        val output = it.out
        runCatching {
            json.decodeFromString<VideoInformation>(output)
        }
    }.mapBoth(
        success = {
            when (it.type) {
                "video" -> {
                    val video = Video(
                        it.id,
                        it.fullTitle!!,
                        it.thumbnailUrl!!,
                        it.url,
                        it.videoUrl!!,
                        it.domain
                    )
                    return@mapBoth listOf(video) to Type.Video
                }
                "playlist" -> {
                    return@mapBoth it.entries!!.map { entry ->
                        Video(
                            entry.id,
                            entry.fullTitle!!,
                            entry.thumbnailUrl!!,
                            entry.url,
                            entry.videoUrl!!,
                            entry.domain
                        )
                    } to Type.Playlist(it.url)
                }
                else -> {
                    Log.e(WORKER_ID, "Unknown type:${it.type}, Data:$it")
                    return@mapBoth emptyList<Video>() to Type.Video
                }
            }
        },
        failure = {
            Log.e(WORKER_ID, "Download failed. Retrying...", it)
            triedCount.getAndUpdate { count -> count + 1 }
            runCatching {
                setForeground(getForegroundInfo())
            }
            return@mapBoth download(request)
        }
    )

    private sealed interface Type {
        object Video : Type

        @JvmInline
        value class Playlist(val url: String) : Type
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        const val WORKER_ID = "VideoDownloadWorker"
        private const val KEY_URL = "DownloadUrl"
        private const val KEY_PLAYLIST = "PlaylistId"

        fun registerWorker(
            context: Context,
            targetUrl: String,
            targetPlaylist: Long? = null
        ): OneTimeWorkRequest {
            val data = Data.Builder().putString(KEY_URL, targetUrl)
            targetPlaylist?.also {
                data.putLong(KEY_PLAYLIST, it)
            }
            val request = OneTimeWorkRequestBuilder<VideoInfoDownloadWorker>()
                .setInputData(data.build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORKER_ID,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request
                )
            return request
        }
    }
}