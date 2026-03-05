package net.turtton.ytalarm.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.kernel.entity.Playlist
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.usecase.ImportResult
import net.turtton.ytalarm.usecase.UseCaseContainer

const val VIDEO_DOWNLOAD_NOTIFICATION = "net.turtton.ytalarm.VideoDLNotification"

class VideoInfoDownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    @SuppressLint("RestrictedApi")
    override suspend fun doWork(): Result {
        val useCaseContainer = (applicationContext as YtApplication).dataContainerProvider
            .getUseCaseContainer()
        val targetUrl = inputData.getString(KEY_URL) ?: return Result.failure()
        val isSyncMode = inputData.getBoolean(KEY_SYNC_MODE, false)
        val syncPlaylistId = inputData.getLong(KEY_SYNC_PLAYLIST_ID, 0L)

        if (isSyncMode && syncPlaylistId > 0) {
            return doSyncWork(useCaseContainer, targetUrl, syncPlaylistId)
        }

        val playlistArray = inputData.getLongArray(KEY_PLAYLIST)
        return doImportWork(useCaseContainer, targetUrl, playlistArray)
    }

    private suspend fun doImportWork(
        useCaseContainer: UseCaseContainer<*, *, *, *>,
        targetUrl: String,
        playlistArray: LongArray?
    ): Result {
        // 1. Importingプレースホルダーを先にDBに挿入（UIに即表示）
        val importingTitle = applicationContext.getString(R.string.item_video_list_state_importing)
        val placeholderId = useCaseContainer.insertImportingPlaceholder(importingTitle)

        // 2. プレースホルダーをプレイリストに追加
        if (playlistArray != null && playlistArray.isNotEmpty()) {
            addVideoToPlaylists(useCaseContainer, placeholderId, playlistArray)
        }

        // 3. 動画情報を取得してプレースホルダーを更新
        return when (
            val importResult = useCaseContainer.fetchAndImportVideo(targetUrl, placeholderId)
        ) {
            is ImportResult.Success -> Result.success()

            is ImportResult.Duplicate -> {
                // プレースホルダーは fetchAndImportVideo 内で削除済み
                // 既存動画をプレイリストに追加（プレースホルダーから置き換え）
                if (playlistArray != null && playlistArray.isNotEmpty()) {
                    replaceVideoInPlaylists(
                        useCaseContainer,
                        placeholderId,
                        importResult.existingVideoId,
                        playlistArray
                    )
                }
                Result.success()
            }

            is ImportResult.Failure.UnsupportedUrl -> {
                // プレイリストとして再試行（プレースホルダーは削除）
                useCaseContainer.deleteVideoById(placeholderId)
                removeVideoFromPlaylists(useCaseContainer, placeholderId, playlistArray)
                doImportCloudPlaylist(useCaseContainer, targetUrl, playlistArray)
            }

            is ImportResult.Failure.Network -> {
                Log.e(WORKER_ID, "Network error. Url: $targetUrl")
                useCaseContainer.markVideoAsFailed(placeholderId, targetUrl)
                Result.failure()
            }

            is ImportResult.Failure.Parse -> {
                Log.e(WORKER_ID, "Parse error. Url: $targetUrl")
                useCaseContainer.markVideoAsFailed(placeholderId, targetUrl)
                Result.failure()
            }
        }
    }

    private suspend fun doImportCloudPlaylist(
        useCaseContainer: UseCaseContainer<*, *, *, *>,
        targetUrl: String,
        playlistArray: LongArray?
    ): Result {
        val playlistResult = useCaseContainer.importCloudPlaylist(targetUrl)
        return playlistResult.fold(
            ifLeft = { failure ->
                Log.e(WORKER_ID, "Import failed. Url: $targetUrl, reason: $failure")
                Result.failure()
            },
            ifRight = { newPlaylistId ->
                if (playlistArray != null && playlistArray.isNotEmpty()) {
                    val newPlaylist = useCaseContainer.getPlaylistByIdSync(newPlaylistId)
                    if (newPlaylist != null) {
                        addCloudPlaylistVideosToPlaylists(
                            useCaseContainer,
                            newPlaylist.videos,
                            playlistArray
                        )
                    }
                }
                Result.success()
            }
        )
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val title = applicationContext.getString(R.string.notification_download_video_info_title)
        val cancel = applicationContext.getString(R.string.cancel)
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

    private suspend fun addVideoToPlaylists(
        useCaseContainer: UseCaseContainer<*, *, *, *>,
        videoId: Long,
        playlistArray: LongArray
    ) {
        val playlists = useCaseContainer.getPlaylistsByIdsSync(playlistArray.toList())
        val updatedPlaylists = playlists.map { playlist ->
            val newVideos = playlist.videos.toMutableSet().apply { add(videoId) }.toList()
            updatePlaylistWithThumbnail(useCaseContainer, playlist, newVideos)
        }
        useCaseContainer.updateAllPlaylists(updatedPlaylists)
    }

    private suspend fun replaceVideoInPlaylists(
        useCaseContainer: UseCaseContainer<*, *, *, *>,
        oldVideoId: Long,
        newVideoId: Long,
        playlistArray: LongArray
    ) {
        val playlists = useCaseContainer.getPlaylistsByIdsSync(playlistArray.toList())
        val updatedPlaylists = playlists.map { playlist ->
            val newVideos = playlist.videos.map { if (it == oldVideoId) newVideoId else it }
            updatePlaylistWithThumbnail(useCaseContainer, playlist, newVideos)
        }
        useCaseContainer.updateAllPlaylists(updatedPlaylists)
    }

    private suspend fun removeVideoFromPlaylists(
        useCaseContainer: UseCaseContainer<*, *, *, *>,
        videoId: Long,
        playlistArray: LongArray?
    ) {
        if (playlistArray == null || playlistArray.isEmpty()) return
        val playlists = useCaseContainer.getPlaylistsByIdsSync(playlistArray.toList())
        val updatedPlaylists = playlists.map { playlist ->
            playlist.copy(videos = playlist.videos.filter { it != videoId })
        }
        useCaseContainer.updateAllPlaylists(updatedPlaylists)
    }

    private suspend fun addCloudPlaylistVideosToPlaylists(
        useCaseContainer: UseCaseContainer<*, *, *, *>,
        videoIds: List<Long>,
        playlistArray: LongArray
    ) {
        val playlists = useCaseContainer.getPlaylistsByIdsSync(playlistArray.toList())
        val updatedPlaylists = playlists.map { playlist ->
            val newVideos = (playlist.videos + videoIds).distinct()
            updatePlaylistWithThumbnail(useCaseContainer, playlist, newVideos)
        }
        useCaseContainer.updateAllPlaylists(updatedPlaylists)
    }

    private suspend fun updatePlaylistWithThumbnail(
        useCaseContainer: UseCaseContainer<*, *, *, *>,
        playlist: Playlist,
        newVideos: List<Long>
    ): Playlist {
        var updated = playlist.copy(videos = newVideos)

        // サムネイルがNoneの場合、最初の動画のサムネイルに更新する
        if (updated.thumbnail == Playlist.Thumbnail.None && newVideos.isNotEmpty()) {
            val firstVideo = useCaseContainer.getVideoByIdSync(newVideos.first())
            if (firstVideo != null && firstVideo.state is Video.State.Information) {
                updated = updated.copy(thumbnail = Playlist.Thumbnail.Video(firstVideo.id))
            }
        }

        return updated
    }

    private suspend fun doSyncWork(
        useCaseContainer: UseCaseContainer<*, *, *, *>,
        targetUrl: String,
        playlistId: Long
    ): Result {
        val playlist = useCaseContainer.getPlaylistByIdSync(playlistId)
            ?: return Result.failure()

        if (playlist.type !is Playlist.Type.CloudPlaylist) {
            return Result.failure()
        }

        // プレイリスト情報を取得
        val playlistInfoResult = useCaseContainer.fetchPlaylistInfoForSync(targetUrl)

        return playlistInfoResult.fold(
            ifLeft = { error ->
                Log.e(WORKER_ID, "Sync failed. Url: $targetUrl, error: $error")
                Result.failure()
            },
            ifRight = { videoInfoList ->
                useCaseContainer.syncCloudPlaylist(playlist, videoInfoList)
                Result.success()
            }
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        const val WORKER_ID = "VideoDownloadWorker"
        private const val KEY_URL = "DownloadUrl"
        private const val KEY_PLAYLIST = "PlaylistId"
        private const val KEY_SYNC_MODE = "SyncMode"
        private const val KEY_SYNC_PLAYLIST_ID = "SyncPlaylistId"

        fun registerSyncWorker(
            context: Context,
            playlistId: Long,
            playlistUrl: String
        ): OneTimeWorkRequest {
            val data = Data.Builder()
                .putString(KEY_URL, playlistUrl)
                .putBoolean(KEY_SYNC_MODE, true)
                .putLong(KEY_SYNC_PLAYLIST_ID, playlistId)
                .build()
            val request = OneTimeWorkRequestBuilder<VideoInfoDownloadWorker>()
                .setInputData(data)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "SyncWorker_$playlistId",
                ExistingWorkPolicy.REPLACE,
                request
            )
            return request
        }

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