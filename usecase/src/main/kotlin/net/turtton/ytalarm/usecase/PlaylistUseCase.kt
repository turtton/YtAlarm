package net.turtton.ytalarm.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import net.turtton.ytalarm.kernel.di.DependsOnAlarmRepository
import net.turtton.ytalarm.kernel.di.DependsOnDataSource
import net.turtton.ytalarm.kernel.di.DependsOnPlaylistRepository
import net.turtton.ytalarm.kernel.di.DependsOnVideoRepository
import net.turtton.ytalarm.kernel.entity.Playlist
import net.turtton.ytalarm.kernel.entity.Video
import kotlin.coroutines.cancellation.CancellationException

/**
 * プレイリストに関するビジネスロジックを定義するUseCaseインターフェース。
 *
 * @param LExec ローカルデータソースのExecutor型
 * @param LDS 複数のRepositoryDependsOnおよびDependsOnDataSourceを実装したローカルデータソース型
 */
interface PlaylistUseCase<LExec, LDS>
    where LDS : DependsOnPlaylistRepository<LExec>,
          LDS : DependsOnVideoRepository<LExec>,
          LDS : DependsOnAlarmRepository<LExec>,
          LDS : DependsOnDataSource<LExec> {
    val localDataSource: LDS

    /**
     * 全プレイリストをFlowとして返す。
     * ViewModelのLiveDataに変換するために使用する。
     */
    fun getAllPlaylistsFlow(): Flow<List<Playlist>> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.playlistRepository.getAll(executor)
    }

    /**
     * 全プレイリストを同期的に返す。
     */
    suspend fun getAllPlaylistsSync(): List<Playlist> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.playlistRepository.getAllSync(executor)
    }

    /**
     * IDでプレイリストをFlowとして返す。存在しないIDの場合はnullを流す。
     */
    fun getPlaylistByIdFlow(id: Long): Flow<Playlist?> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.playlistRepository.getFromId(executor, id)
    }

    /**
     * IDでプレイリストを同期的に返す。
     */
    suspend fun getPlaylistByIdSync(id: Long): Playlist? {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.playlistRepository.getFromIdSync(executor, id)
    }

    /**
     * IDリストでプレイリストを同期的に返す。
     */
    suspend fun getPlaylistsByIdsSync(ids: List<Long>): List<Playlist> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.playlistRepository.getFromIdsSync(executor, ids)
    }

    /**
     * プレイリストを更新する。
     * lastUpdatedを自動的に現在時刻に更新する。
     */
    suspend fun updatePlaylist(playlist: Playlist) {
        val executor = localDataSource.dataSource.createExecutor()
        localDataSource.playlistRepository.update(
            executor,
            playlist.copy(lastUpdated = Clock.System.now())
        )
    }

    /**
     * プレイリストリストを一括更新する。
     */
    suspend fun updateAllPlaylists(playlists: List<Playlist>) {
        val executor = localDataSource.dataSource.createExecutor()
        localDataSource.playlistRepository.updateAll(executor, playlists)
    }

    /**
     * プレイリストを挿入してIDを返す。
     */
    suspend fun insertPlaylist(playlist: Playlist): Long {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.playlistRepository.insert(executor, playlist)
    }

    /**
     * プレイリストを削除する。
     */
    suspend fun deletePlaylist(playlist: Playlist) {
        val executor = localDataSource.dataSource.createExecutor()
        localDataSource.playlistRepository.delete(executor, playlist)
    }

    /**
     * プレイリストリストを一括削除する。
     */
    suspend fun deleteAllPlaylists(playlists: List<Playlist>) {
        val executor = localDataSource.dataSource.createExecutor()
        localDataSource.playlistRepository.deleteAll(executor, playlists)
    }

    /**
     * プレイリストのサムネイルを検証し、無効なサムネイルを更新する。
     * - サムネイル動画が存在しない→Noneにフォールバック
     * - サムネイル動画がInformation状態でない→プレイリスト内の有効な動画にフォールバック
     */
    suspend fun validateAndUpdateThumbnails() {
        val executor = localDataSource.dataSource.createExecutor()
        val playlists = localDataSource.playlistRepository.getAllSync(executor)
        playlists.forEach { playlist ->
            try {
                val updated = validateThumbnail(executor, playlist)
                if (updated != null) {
                    localDataSource.playlistRepository.update(executor, updated)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // validation failure は個別プレイリストの問題なのでスキップ
            }
        }
    }

    private suspend fun validateThumbnail(executor: LExec, playlist: Playlist): Playlist? {
        val thumbnail = playlist.thumbnail
        if (thumbnail !is Playlist.Thumbnail.Video) return null

        val thumbnailVideo = try {
            localDataSource.videoRepository.getFromIdSync(executor, thumbnail.id)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

        if (thumbnailVideo != null && thumbnailVideo.state is Video.State.Information) {
            return null
        }

        // フォールバック: プレイリスト内の他の動画を探す
        val candidateIds = playlist.videos.filter { it != thumbnail.id }
        if (candidateIds.isEmpty()) {
            return playlist.copy(thumbnail = Playlist.Thumbnail.None)
        }

        val fallbackVideoId = try {
            val videos = localDataSource.videoRepository.getFromIdsSync(executor, candidateIds)
            videos.firstOrNull { it.state is Video.State.Information }?.id
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

        val newThumbnail = if (fallbackVideoId != null) {
            Playlist.Thumbnail.Video(fallbackVideoId)
        } else {
            Playlist.Thumbnail.None
        }
        return playlist.copy(thumbnail = newThumbnail)
    }

    /**
     * プレイリストを安全に削除する。
     * アラームがこのプレイリストを参照している場合、アラームから参照を除去する。
     *
     * @param playlist 削除するプレイリスト
     */
    suspend fun safeDeletePlaylist(playlist: Playlist) {
        val executor = localDataSource.dataSource.createExecutor()
        localDataSource.playlistRepository.delete(executor, playlist)

        val allAlarms = localDataSource.alarmRepository.getAllSync(executor)
        allAlarms
            .filter { it.playlistIds.contains(playlist.id) }
            .forEach { alarm ->
                val updatedAlarm = alarm.copy(
                    playlistIds = alarm.playlistIds.filter { it != playlist.id }
                )
                localDataSource.alarmRepository.update(executor, updatedAlarm)
            }
    }

    /**
     * 未使用のプレイリストを削除する。
     * どのアラームからも参照されていないプレイリストを削除する。
     */
    suspend fun deleteUnusedPlaylists() {
        val executor = localDataSource.dataSource.createExecutor()
        val allAlarms = localDataSource.alarmRepository.getAllSync(executor)
        val usedPlaylistIds = allAlarms.flatMap { it.playlistIds }.toSet()
        val allPlaylists = localDataSource.playlistRepository.getAllSync(executor)
        val unusedPlaylists = allPlaylists.filter { !usedPlaylistIds.contains(it.id) }
        if (unusedPlaylists.isNotEmpty()) {
            localDataSource.playlistRepository.deleteAll(executor, unusedPlaylists)
        }
    }

    /**
     * プレイリストから動画を削除し、サムネイルを更新する。
     *
     * @param playlist 対象プレイリスト
     * @param videoIds 削除する動画のIDリスト
     */
    suspend fun removeVideosFromPlaylist(playlist: Playlist, videoIds: List<Long>) {
        val executor = localDataSource.dataSource.createExecutor()
        val remainingVideoIds = playlist.videos.filter { !videoIds.contains(it) }
        var updatedPlaylist = playlist.copy(videos = remainingVideoIds)

        // サムネイルが削除対象動画の場合、更新する
        val currentThumbnail = playlist.thumbnail
        if (currentThumbnail is Playlist.Thumbnail.Video &&
            videoIds.contains(currentThumbnail.id)
        ) {
            val newThumbnail = findValidThumbnail(executor, remainingVideoIds)
            updatedPlaylist = updatedPlaylist.copy(thumbnail = newThumbnail)
        }

        localDataSource.playlistRepository.update(executor, updatedPlaylist)
    }

    private suspend fun findValidThumbnail(
        executor: LExec,
        videoIds: List<Long>
    ): Playlist.Thumbnail {
        if (videoIds.isEmpty()) return Playlist.Thumbnail.None
        val videos = localDataSource.videoRepository.getFromIdsSync(executor, videoIds)
        val firstValid = videos.firstOrNull { it.state is Video.State.Information }
        return if (firstValid != null) {
            Playlist.Thumbnail.Video(firstValid.id)
        } else {
            Playlist.Thumbnail.None
        }
    }

    /**
     * プレイリストのサムネイルを設定する。
     *
     * @param playlist 対象プレイリスト
     * @param thumbnail 新しいサムネイル
     */
    suspend fun setPlaylistThumbnail(playlist: Playlist, thumbnail: Playlist.Thumbnail) {
        val executor = localDataSource.dataSource.createExecutor()
        localDataSource.playlistRepository.update(executor, playlist.copy(thumbnail = thumbnail))
    }

    /**
     * CloudPlaylistの同期ルールを更新する。
     * プレイリストタイプがCloudPlaylistでない場合は何もしない。
     *
     * @param playlist 対象プレイリスト
     * @param syncRule 新しい同期ルール
     */
    suspend fun updateSyncRule(playlist: Playlist, syncRule: Playlist.SyncRule) {
        val executor = localDataSource.dataSource.createExecutor()
        val currentType = playlist.type as? Playlist.Type.CloudPlaylist ?: return
        val updatedPlaylist = playlist.copy(
            type = currentType.copy(syncRule = syncRule)
        )
        localDataSource.playlistRepository.update(executor, updatedPlaylist)
    }
}