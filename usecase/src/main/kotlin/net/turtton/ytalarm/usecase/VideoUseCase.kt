package net.turtton.ytalarm.usecase

import arrow.core.Either
import kotlinx.coroutines.flow.Flow
import net.turtton.ytalarm.kernel.di.DependsOnDataSource
import net.turtton.ytalarm.kernel.di.DependsOnVideoInfoRepository
import net.turtton.ytalarm.kernel.di.DependsOnVideoRepository
import net.turtton.ytalarm.kernel.dto.VideoInformation
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.kernel.error.VideoInfoError
import kotlin.time.Duration

/**
 * 動画の再インポート結果を表すシールドインターフェース。
 */
sealed interface ReimportResult {
    /** 再インポート成功 */
    data object Success : ReimportResult

    /** 再インポートエラー */
    sealed interface Error : ReimportResult {
        data object NoUrl : Error
        data object Network : Error
        data object Parse : Error
    }
}

/**
 * 動画に関するビジネスロジックを定義するUseCaseインターフェース。
 *
 * @param LExec ローカルデータソースのExecutor型
 * @param RExec リモートデータソースのExecutor型
 * @param LDS DependsOnVideoRepositoryおよびDependsOnDataSourceを実装したローカルデータソース型
 * @param RDS DependsOnVideoInfoRepositoryおよびDependsOnDataSourceを実装したリモートデータソース型
 */
interface VideoUseCase<LExec, RExec, LDS, RDS>
    where LDS : DependsOnVideoRepository<LExec>,
          LDS : DependsOnDataSource<LExec>,
          RDS : DependsOnVideoInfoRepository<RExec>,
          RDS : DependsOnDataSource<RExec> {
    val localDataSource: LDS
    val remoteDataSource: RDS

    /**
     * 全動画をFlowとして返す。
     * ViewModelのLiveDataに変換するために使用する。
     */
    fun getAllVideosFlow(): Flow<List<Video>> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.videoRepository.getAll(executor)
    }

    /**
     * IDで動画を同期的に返す。
     */
    suspend fun getVideoByIdSync(id: Long): Video? {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.videoRepository.getFromIdSync(executor, id)
    }

    /**
     * IDリストで動画を同期的に返す。
     */
    suspend fun getVideosByIdsSync(ids: List<Long>): List<Video> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.videoRepository.getFromIdsSync(executor, ids)
    }

    /**
     * IDリストで動画をFlowとして返す。
     */
    fun getVideosByIdsFlow(ids: List<Long>): Flow<List<Video>> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.videoRepository.getFromIds(executor, ids)
    }

    /**
     * 指定IDを除いた動画を同期的に返す。
     */
    suspend fun getVideosExceptIdsSync(ids: List<Long>): List<Video> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.videoRepository.getExceptIdsSync(executor, ids)
    }

    /**
     * VideoIDリストで動画をFlowとして返す。
     */
    fun getVideosByVideoIdsFlow(ids: List<String>): Flow<List<Video>> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.videoRepository.getFromVideoIds(executor, ids)
    }

    /**
     * VideoIDで動画を同期的に返す。
     */
    suspend fun getVideoByVideoIdSync(id: String): Video? {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.videoRepository.getFromVideoIdSync(executor, id)
    }

    /**
     * VideoIDリストで動画を同期的に返す。
     */
    suspend fun getVideosByVideoIdsSync(ids: List<String>): List<Video> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.videoRepository.getFromVideoIdsSync(executor, ids)
    }

    /**
     * 指定VideoIDを除いた動画を同期的に返す。
     */
    suspend fun getVideosExceptVideoIdsSync(ids: List<String>): List<Video> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.videoRepository.getExceptVideoIdsSync(executor, ids)
    }

    /**
     * 動画を更新する。
     */
    suspend fun updateVideo(video: Video) {
        val executor = localDataSource.dataSource.createExecutor()
        localDataSource.videoRepository.update(executor, video)
    }

    /**
     * 動画を挿入してIDを返す。
     */
    suspend fun insertVideo(video: Video): Long {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.videoRepository.insert(executor, video)
    }

    /**
     * 動画リストを一括挿入してIDリストを返す。
     */
    suspend fun insertAllVideos(videos: List<Video>): List<Long> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.videoRepository.insertAll(executor, videos)
    }

    /**
     * 動画を削除する。
     */
    suspend fun deleteVideo(video: Video) {
        val executor = localDataSource.dataSource.createExecutor()
        localDataSource.videoRepository.delete(executor, video)
    }

    /**
     * IDを指定して動画を削除する。
     */
    suspend fun deleteVideoById(videoId: Long) {
        val executor = localDataSource.dataSource.createExecutor()
        val video = localDataSource.videoRepository.getFromIdSync(executor, videoId) ?: return
        localDataSource.videoRepository.delete(executor, video)
    }

    /**
     * 動画リストを一括削除する。
     */
    suspend fun deleteAllVideos(videos: List<Video>) {
        val executor = localDataSource.dataSource.createExecutor()
        localDataSource.videoRepository.deleteAll(executor, videos)
    }

    /**
     * 動画情報を再取得してDBを更新する。
     * Failed状態の動画のsourceUrlか、videoUrlを使って再フェッチする。
     *
     * @param video 再インポート対象の動画
     * @return 再インポート結果
     */
    suspend fun reimportVideo(video: Video): ReimportResult {
        val url = when (val state = video.state) {
            is Video.State.Failed -> state.sourceUrl.ifEmpty { return ReimportResult.Error.NoUrl }
            else -> video.videoUrl.ifEmpty { return ReimportResult.Error.NoUrl }
        }

        val lExecutor = localDataSource.dataSource.createExecutor()
        val rExecutor = remoteDataSource.dataSource.createExecutor()

        return when (
            val result = remoteDataSource.videoInfoRepository.fetchVideoInfo(rExecutor, url)
        ) {
            is Either.Left -> when (result.value) {
                is VideoInfoError.NetworkError -> ReimportResult.Error.Network
                is VideoInfoError.ParseError -> ReimportResult.Error.Parse
                is VideoInfoError.UnsupportedUrl -> ReimportResult.Error.NoUrl
                is VideoInfoError.Unavailable -> ReimportResult.Error.Network
            }

            is Either.Right -> {
                val info = result.value
                val typeData = info.typeData
                if (typeData !is VideoInformation.Type.Video) {
                    return ReimportResult.Error.NoUrl
                }
                val updatedVideo = Video(
                    id = video.id,
                    videoId = info.id,
                    title = typeData.fullTitle,
                    thumbnailUrl = typeData.thumbnailUrl,
                    videoUrl = typeData.videoUrl,
                    domain = info.domain,
                    state = Video.State.Information(),
                    creationDate = video.creationDate
                )
                localDataSource.videoRepository.update(lExecutor, updatedVideo)
                ReimportResult.Success
            }
        }
    }

    /**
     * アラームで再生可能な動画リストを返す。
     * Information状態の動画のみを対象とする。
     *
     * @param alarm 対象アラーム
     * @param playlistVideoMap プレイリストIDと動画IDリストのマップ
     * @return 再生可能な動画リスト
     */
    suspend fun getPlayableVideosForAlarm(
        alarm: Alarm,
        playlistVideoMap: Map<Long, List<Long>>
    ): List<Video> {
        val executor = localDataSource.dataSource.createExecutor()
        val videoIds = alarm.playlistIds.flatMap { playlistVideoMap[it] ?: emptyList() }
        val videos = localDataSource.videoRepository.getFromIdsSync(executor, videoIds)
        return videos.filter { it.state is Video.State.Information }
    }

    /**
     * Importing/Downloading状態が指定時間以上経過した動画をFailed状態に遷移させる。
     *
     * @param videoIds 対象の動画IDリスト
     * @param threshold この時間を超えたらゴミとみなす
     * @param clock 現在時刻取得用のClock
     */
    suspend fun collectGarbageVideos(
        videoIds: List<Long>,
        threshold: Duration,
        clock: kotlinx.datetime.Clock = kotlinx.datetime.Clock.System
    ) {
        val executor = localDataSource.dataSource.createExecutor()
        val now = clock.now()
        val videos = localDataSource.videoRepository.getFromIdsSync(executor, videoIds)
        videos
            .filter { video ->
                video.state.isUpdating() &&
                    (now - video.creationDate) > threshold
            }
            .forEach { video ->
                localDataSource.videoRepository.update(
                    executor,
                    video.copy(state = Video.State.Failed(video.videoUrl))
                )
            }
    }
}