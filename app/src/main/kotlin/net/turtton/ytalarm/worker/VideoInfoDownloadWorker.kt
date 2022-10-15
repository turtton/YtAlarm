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
import net.turtton.ytalarm.structure.Playlist
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

        inputData.getLongArray(KEY_PLAYLIST)?.forEach { targetPlaylist ->
            val playlist = repository.getPlaylistFromIdSync(targetPlaylist) ?: Playlist()
            val newList = (playlist.videos + videos.map { it.id }).distinct()
            val new = when (type) {
                is Type.Video -> playlist.copy(videos = newList)
                is Type.Playlist -> {
                    playlist.copy(videos = newList, originUrl = type.url).let {
                        if (it.id == 0L) {
                            it.copy(title = type.title)
                        } else {
                            it
                        }
                    }
                }
            }
            if (targetPlaylist != -1L) {
                repository.update(new)
            } else {
                repository.insert(new)
            }
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
            when (it.typeData) {
                is VideoInformation.Type.Video -> {
                    val video = Video(
                        it.id,
                        it.typeData.fullTitle,
                        it.typeData.thumbnailUrl,
                        it.url,
                        it.domain,
                        Video.State.Information
                    )
                    return@mapBoth listOf(video) to Type.Video
                }
                is VideoInformation.Type.Playlist -> {
                    return@mapBoth it.typeData.entries.map { entry ->
                        entry.typeData as VideoInformation.Type.Video
                        Video(
                            entry.id,
                            entry.typeData.fullTitle,
                            entry.typeData.thumbnailUrl,
                            entry.url,
                            entry.domain,
                            Video.State.Information
                        )
                    } to Type.Playlist(it.title!!, it.url)
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

        data class Playlist(val title: String, val url: String) : Type
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        const val WORKER_ID = "VideoDownloadWorker"
        private const val KEY_URL = "DownloadUrl"
        private const val KEY_PLAYLIST = "PlaylistId"

        fun registerWorker(
            context: Context,
            targetUrl: String,
            targetPlaylists: LongArray = longArrayOf()
        ): OneTimeWorkRequest {
            val data = Data.Builder().putString(KEY_URL, targetUrl)
            if (targetPlaylists.isNotEmpty()) {
                data.putLongArray(KEY_PLAYLIST, targetPlaylists)
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