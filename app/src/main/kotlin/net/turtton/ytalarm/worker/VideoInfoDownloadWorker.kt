package net.turtton.ytalarm.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.runCatching
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.turtton.ytalarm.R
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.util.VideoInformation
import net.turtton.ytalarm.util.extensions.copyAsFailed
import net.turtton.ytalarm.util.extensions.deleteVideo
import net.turtton.ytalarm.util.extensions.hasUpdatingVideo
import net.turtton.ytalarm.util.extensions.insertVideos
import net.turtton.ytalarm.util.extensions.updateThumbnail

const val VIDEO_DOWNLOAD_NOTIFICATION = "net.turtton.ytalarm.VideoDLNotification"

class VideoInfoDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineIOWorker(appContext, workerParams) {
    private val json = Json { ignoreUnknownKeys = true }

    @SuppressLint("RestrictedApi")
    override suspend fun doWork(): Result {
        val targetUrl = inputData.getString(KEY_URL) ?: return Result.failure()
        var playlistArray = inputData.getLongArray(KEY_PLAYLIST)

        val stateTitle = applicationContext.getString(R.string.item_video_list_state_importing)
        val data = Video.State.Importing(Video.WorkerState.Working(id))
        var targetVideo = Video(videoId = "", title = stateTitle, stateData = data)

        val targetVideoId = repository.insert(targetVideo)
        targetVideo = repository.getVideoFromIdSync(targetVideoId)!!

        @Suppress("ControlFlowWithEmptyBody", "EmptyWhileBlock")
        while (
            WorkManager.getInstance(applicationContext)
                .getWorkInfoById(id)
                .await()
                .let { it == null || it.state.isFinished }
        ) {
        }
        playlistArray = playlistArray?.insertVideoInPlaylists(targetVideo)

        val (videos, type) = download(targetUrl)
            ?: run {
                repository.update(targetVideo.copyAsFailed(targetUrl))
                return Result.failure()
            }

        when (type) {
            is Type.Video -> {
                val video = videos.first()
                insertVideo(playlistArray, video)
            }
            is Type.Playlist -> {
                repository.delete(targetVideo)
                insertCloudPlaylist(playlistArray, videos, targetVideoId, type.url)
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

        return ForegroundInfo(NOTIFICATION_ID, notification.build())
    }

    private fun download(targetUrl: String): Pair<List<Video>, Type>? = runCatching {
        val request = YoutubeDLRequest(targetUrl)
            .addOption("--dump-single-json")
            .addOption("-f", "b")
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
            Log.e(WORKER_ID, "Download failed. Url: $targetUrl", it)
            null
        }
    )

    private suspend fun insertVideo(playlistArray: LongArray?, video: Video) {
        val updatedPlaylist = checkVideoDuplication(video.videoId, video.domain)
            ?.let { duplicatedId ->
                repository.delete(video)
                playlistArray?.let { repository.getPlaylistFromIdsSync(it.toList()) }
                    ?.deleteVideo(video.id)
                    ?.map { playlist ->
                        val videoSet = playlist.videos.toMutableSet()
                        videoSet += duplicatedId
                        var newPlaylist = playlist.copy(videos = videoSet.toList())
                        if (newPlaylist.thumbnail is Playlist.Thumbnail.Drawable) {
                            newPlaylist.updateThumbnail()?.let {
                                newPlaylist = it
                            }
                        }
                        val currentVideos = repository.getVideoFromIdsSync(newPlaylist.videos)
                        if (!currentVideos.hasUpdatingVideo) {
                            newPlaylist = newPlaylist.copy(type = Playlist.Type.Original)
                        }
                        newPlaylist
                    }
            } ?: kotlin.run {
            val importedVideo = video.copy(id = video.id)
            repository.update(importedVideo)
            playlistArray?.let {
                repository.getPlaylistFromIdsSync(it.toList())
            }?.map { pl ->
                var playlist = pl
                var shouldUpdate = false
                val containsVideos = repository.getVideoFromIdsSync(playlist.videos)
                if (!containsVideos.hasUpdatingVideo) {
                    playlist = playlist.copy(type = Playlist.Type.Original)
                    shouldUpdate = true
                }
                if (playlist.thumbnail is Playlist.Thumbnail.Drawable) {
                    playlist.updateThumbnail()?.also {
                        playlist = it
                        shouldUpdate = true
                    }
                }
                playlist.takeIf { shouldUpdate }
            }
        }

        updatedPlaylist?.filterNotNull()
            .takeIf { !it.isNullOrEmpty() }
            ?.let {
                repository.update(it)
            }
    }

    private suspend fun insertCloudPlaylist(
        playlistArray: LongArray?,
        videos: List<Video>,
        deletedVideoId: Long,
        url: String
    ) {
        val targetIds = mutableListOf<Long>()
        val newVideos = videos.filter {
            checkVideoDuplication(it.videoId, it.domain)
                ?.also { duplicatedId ->
                    targetIds += duplicatedId
                }.let { duplicatedId ->
                    duplicatedId == null
                }
        }
        targetIds += repository.insert(newVideos)

        playlistArray?.let { _ ->
            var playlists = repository.getPlaylistFromIdsSync(playlistArray.toList())
            playlists = playlists.deleteVideo(deletedVideoId)
            playlists = playlists.insertVideos(targetIds)
            val playlistType = Playlist.Type.CloudPlaylist(url, id)
            playlists = playlists.map { playlist ->
                var newPlaylist = playlist.copy(type = playlistType)
                if (newPlaylist.thumbnail is Playlist.Thumbnail.Drawable) {
                    newPlaylist.updateThumbnail()?.let {
                        newPlaylist = it
                    }
                }
                newPlaylist
            }
            repository.update(playlists)
        }
    }

    private suspend fun LongArray.insertVideoInPlaylists(video: Video) = map {
        val playlist =
            if (it == 0L) {
                val icon = R.drawable.ic_download
                Playlist(
                    thumbnail = Playlist.Thumbnail.Drawable(icon),
                    type = Playlist.Type.Importing
                )
            } else {
                repository.getPlaylistFromIdSync(it) ?: return@map null
            }
        val newList = playlist.videos.toMutableSet().apply { add(video.id) }.toList()
        val newPlaylist = playlist.copy(videos = newList)
        if (it == 0L) {
            repository.insert(newPlaylist)
        } else {
            repository.update(newPlaylist)
            it
        }
    }.filterNotNull().toLongArray()

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
            val (request, task) = prepareWorker(targetUrl, targetPlaylists)
            WorkManager.getInstance(context).task()
            return request
        }

        fun prepareWorker(
            targetUrl: String,
            targetPlaylists: LongArray = longArrayOf()
        ): Pair<OneTimeWorkRequest, EnqueueTask> {
            val data = Data.Builder().putString(KEY_URL, targetUrl)
            if (targetPlaylists.isNotEmpty()) {
                data.putLongArray(KEY_PLAYLIST, targetPlaylists)
            }
            val request = OneTimeWorkRequestBuilder<VideoInfoDownloadWorker>()
                .setInputData(data.build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            val enqueueTask: EnqueueTask = {
                enqueueUniqueWork(
                    WORKER_ID,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request
                )
            }
            return request to enqueueTask
        }
    }
}

typealias EnqueueTask = WorkManager.() -> Operation