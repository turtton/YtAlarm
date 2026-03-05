package net.turtton.ytalarm.usecase

import arrow.core.Either
import net.turtton.ytalarm.kernel.di.DependsOnDataSource
import net.turtton.ytalarm.kernel.di.DependsOnPlaylistRepository
import net.turtton.ytalarm.kernel.di.DependsOnVideoInfoRepository
import net.turtton.ytalarm.kernel.di.DependsOnVideoRepository
import net.turtton.ytalarm.kernel.dto.VideoInformation
import net.turtton.ytalarm.kernel.entity.Playlist
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.kernel.error.VideoInfoError

/**
 * 動画インポートの結果を表すシールドインターフェース。
 */
sealed interface ImportResult {
    /** インポート成功: 新規挿入した動画のID */
    data class Success(val videoId: Long) : ImportResult

    /** 重複: 既存動画のID */
    data class Duplicate(val existingVideoId: Long) : ImportResult

    /** 失敗 */
    sealed interface Failure : ImportResult {
        data object Network : Failure
        data object Parse : Failure
        data object UnsupportedUrl : Failure
    }
}

/**
 * 動画インポートに関するビジネスロジックを定義するUseCaseインターフェース。
 *
 * @param LExec ローカルデータソースのExecutor型
 * @param RExec リモートデータソースのExecutor型
 * @param LDS VideoRepositoryとPlaylistRepositoryに依存するローカルデータソース型
 * @param RDS VideoInfoRepositoryに依存するリモートデータソース型
 */
interface ImportUseCase<LExec, RExec, LDS, RDS>
    where LDS : DependsOnVideoRepository<LExec>,
          LDS : DependsOnPlaylistRepository<LExec>,
          LDS : DependsOnDataSource<LExec>,
          RDS : DependsOnVideoInfoRepository<RExec>,
          RDS : DependsOnDataSource<RExec> {
    val localDataSource: LDS
    val remoteDataSource: RDS

    /**
     * URLから動画情報を取得してDBに登録する（重複チェック含む）。
     *
     * @param url 動画URL
     * @return インポート結果
     */
    suspend fun fetchAndImportVideo(url: String): ImportResult {
        val lExecutor = localDataSource.dataSource.createExecutor()
        val rExecutor = remoteDataSource.dataSource.createExecutor()

        return when (
            val result = remoteDataSource.videoInfoRepository.fetchVideoInfo(rExecutor, url)
        ) {
            is Either.Left -> when (result.value) {
                is VideoInfoError.NetworkError -> ImportResult.Failure.Network
                is VideoInfoError.ParseError -> ImportResult.Failure.Parse
                is VideoInfoError.UnsupportedUrl -> ImportResult.Failure.UnsupportedUrl
                is VideoInfoError.Unavailable -> ImportResult.Failure.Network
            }

            is Either.Right -> {
                val info = result.value
                val typeData = info.typeData
                if (typeData !is VideoInformation.Type.Video) {
                    return ImportResult.Failure.UnsupportedUrl
                }

                val duplicateId = checkVideoDuplication(info.id, info.domain)
                if (duplicateId != null) {
                    return ImportResult.Duplicate(duplicateId)
                }

                val video = Video(
                    videoId = info.id,
                    title = typeData.fullTitle,
                    thumbnailUrl = typeData.thumbnailUrl,
                    videoUrl = typeData.videoUrl,
                    domain = info.domain,
                    state = Video.State.Information()
                )
                val newId = localDataSource.videoRepository.insert(lExecutor, video)
                ImportResult.Success(newId)
            }
        }
    }

    /**
     * クラウドプレイリストをインポートする。
     * 重複チェックを行い、新規動画をDBに登録してプレイリストを作成する。
     *
     * @param url プレイリストURL
     * @return インポート結果（プレイリストID or エラー）
     */
    suspend fun importCloudPlaylist(url: String): Either<ImportResult.Failure, Long> {
        val lExecutor = localDataSource.dataSource.createExecutor()
        val rExecutor = remoteDataSource.dataSource.createExecutor()

        return when (
            val result = remoteDataSource.videoInfoRepository.fetchPlaylistInfo(rExecutor, url)
        ) {
            is Either.Left -> Either.Left(
                when (result.value) {
                    is VideoInfoError.NetworkError -> ImportResult.Failure.Network
                    is VideoInfoError.ParseError -> ImportResult.Failure.Parse
                    is VideoInfoError.UnsupportedUrl -> ImportResult.Failure.UnsupportedUrl
                    is VideoInfoError.Unavailable -> ImportResult.Failure.Network
                }
            )

            is Either.Right -> {
                val videoInfoList = result.value
                val videoIds = importVideoInfoList(lExecutor, videoInfoList)

                val playlist = Playlist(
                    title = videoInfoList.firstOrNull()?.title ?: "Playlist",
                    videos = videoIds,
                    type = Playlist.Type.CloudPlaylist(url, Playlist.SyncRule.ALWAYS_ADD)
                )
                val playlistId = localDataSource.playlistRepository.insert(lExecutor, playlist)
                Either.Right(playlistId)
            }
        }
    }

    /**
     * SyncRuleに従ってクラウドプレイリストを同期する。
     *
     * @param playlist 同期対象のプレイリスト（CloudPlaylistタイプである必要がある）
     * @param newVideoInfoList 新たに取得した動画情報リスト
     */
    suspend fun syncCloudPlaylist(playlist: Playlist, newVideoInfoList: List<VideoInformation>) {
        val playlistType = playlist.type as? Playlist.Type.CloudPlaylist ?: return
        val lExecutor = localDataSource.dataSource.createExecutor()

        val newVideoIds = importVideoInfoList(lExecutor, newVideoInfoList)

        val updatedPlaylist = when (playlistType.syncRule) {
            Playlist.SyncRule.ALWAYS_ADD -> {
                val mergedVideos = (playlist.videos + newVideoIds).distinct()
                playlist.copy(videos = mergedVideos)
            }

            Playlist.SyncRule.DELETE_IF_NOT_EXIST -> {
                val currentThumbnail = playlist.thumbnail
                val newThumbnail = if (
                    currentThumbnail is Playlist.Thumbnail.Video &&
                    !newVideoIds.contains(currentThumbnail.id)
                ) {
                    newVideoIds.firstOrNull()?.let { Playlist.Thumbnail.Video(it) }
                        ?: Playlist.Thumbnail.None
                } else {
                    currentThumbnail
                }
                playlist.copy(videos = newVideoIds, thumbnail = newThumbnail)
            }
        }

        localDataSource.playlistRepository.update(lExecutor, updatedPlaylist)
    }

    /**
     * プレイリスト情報を取得する（sync用）。
     * Worker からリモートデータソースに直接アクセスせずに済むよう、UseCase 層でカプセル化する。
     *
     * @param url プレイリストURL
     * @return 動画情報リストまたはエラー
     */
    suspend fun fetchPlaylistInfoForSync(
        url: String
    ): Either<VideoInfoError, List<VideoInformation>> {
        val executor = remoteDataSource.dataSource.createExecutor()
        return remoteDataSource.videoInfoRepository.fetchPlaylistInfo(executor, url)
    }

    /**
     * 動画の重複をチェックする。
     * 同一videoIdかつ同一ドメインの動画がDBに存在する場合、そのIDを返す。
     *
     * @param videoId 動画ID
     * @param domain ドメイン
     * @return 重複する既存動画のIDまたはnull
     */
    suspend fun checkVideoDuplication(videoId: String, domain: String): Long? {
        val executor = localDataSource.dataSource.createExecutor()
        val existing = localDataSource.videoRepository.getFromVideoIdSync(executor, videoId)
            ?: return null
        return if (existing.domain == domain) existing.id else null
    }

    private suspend fun importVideoInfoList(
        executor: LExec,
        videoInfoList: List<VideoInformation>
    ): List<Long> {
        val resultIds = mutableListOf<Long>()
        for (info in videoInfoList) {
            val typeData = info.typeData as? VideoInformation.Type.Video ?: continue
            // executor を統一するため、重複チェックを直接インライン化
            val existing = localDataSource.videoRepository.getFromVideoIdSync(executor, info.id)
            val duplicateId = if (existing != null &&
                existing.domain == info.domain
            ) {
                existing.id
            } else {
                null
            }
            if (duplicateId != null) {
                resultIds += duplicateId
            } else {
                val video = Video(
                    videoId = info.id,
                    title = typeData.fullTitle,
                    thumbnailUrl = typeData.thumbnailUrl,
                    videoUrl = typeData.videoUrl,
                    domain = info.domain,
                    state = Video.State.Information()
                )
                val newId = localDataSource.videoRepository.insert(executor, video)
                resultIds += newId
            }
        }
        return resultIds
    }
}