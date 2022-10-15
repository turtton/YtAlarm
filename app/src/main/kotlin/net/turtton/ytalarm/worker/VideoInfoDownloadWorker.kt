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
        val playlists = inputData.getLongArray(KEY_PLAYLIST)

        val stateTitle = applicationContext.getString(R.string.item_video_list_state_downloading)
        val data = Video.State.Importing(id)
        var targetVideo = Video(videoId = "", title = stateTitle, stateData = data)

        val targetVideoId = repository.insert(targetVideo)
        targetVideo = targetVideo.copy(id = targetVideoId)
        playlists?.insertVideosInPlaylists(listOf(targetVideo), Type.Video)

        val request = YoutubeDLRequest(targetUrl)
            .addOption("--dump-single-json")
            .addOption("-f", "b")

        val (videos, type) = download(request)

        if (type is Type.Video) {
            val video = videos.first()
            val duplication = checkVideoDuplication(video.videoId, video.domain)
            if (duplication == null) {
                repository.update(video.copy(id = targetVideoId))
            } else {
                repository.delete(targetVideo)
                playlists?.let {
                    it.deleteVideoFromPlaylists(targetVideoId)
                    repository.getPlaylistFromIdsSync(it.toList()).forEach { playlist ->
                        val videoSet = playlist.videos.toMutableSet()
                        videoSet += duplication
                        repository.update(playlist.copy(videos = videoSet.toList()))
                    }
                }
            }
        } else {
            repository.delete(targetVideo)
            playlists?.deleteVideoFromPlaylists(targetVideoId)
            repository.insert(videos)
            playlists?.insertVideosInPlaylists(videos, type)
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
                        0,
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
                            0,
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

    private suspend fun LongArray.insertVideosInPlaylists(videos: List<Video>, type: Type) =
        forEach { targetPlaylist ->
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

    private suspend fun LongArray.deleteVideoFromPlaylists(targetVideoId: Long) = forEach {
        val playlist = repository.getPlaylistFromIdSync(it) ?: return@forEach
        val newVideoList = playlist.videos.toMutableList().apply { remove(targetVideoId) }
        repository.update(playlist.copy(videos = newVideoList))
    }

    private suspend fun checkVideoDuplication(videoId: String, domain: String): Long? =
        repository.getVideoFromVideoIdSync(videoId)?.let {
            if (it.domain == domain) {
                it.id
            } else {
                null
            }
        }

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