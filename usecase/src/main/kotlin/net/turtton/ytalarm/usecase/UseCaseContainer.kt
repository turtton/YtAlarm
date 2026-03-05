package net.turtton.ytalarm.usecase

import net.turtton.ytalarm.kernel.di.DependsOnAlarmRepository
import net.turtton.ytalarm.kernel.di.DependsOnDataSource
import net.turtton.ytalarm.kernel.di.DependsOnPlaylistRepository
import net.turtton.ytalarm.kernel.di.DependsOnVideoInfoRepository
import net.turtton.ytalarm.kernel.di.DependsOnVideoRepository
import net.turtton.ytalarm.kernel.port.AlarmSchedulerPort

/**
 * ローカルデータソースの依存をまとめるコンテナインターフェース。
 * Alarm, Video, Playlist の各Repositoryとその共通Executorを束ねる。
 */
interface LocalDataSourceContainer<Executor> :
    DependsOnAlarmRepository<Executor>,
    DependsOnVideoRepository<Executor>,
    DependsOnPlaylistRepository<Executor>,
    DependsOnDataSource<Executor>

/**
 * リモートデータソースの依存をまとめるコンテナインターフェース。
 * VideoInfoRepositoryとそのExecutorを束ねる。
 */
interface RemoteDataSourceContainer<Executor> :
    DependsOnVideoInfoRepository<Executor>,
    DependsOnDataSource<Executor>

/**
 * 全UseCaseを合成するコンテナインターフェース。
 * AlarmUseCase, PlaylistUseCase, VideoUseCase, ImportUseCaseのmixin。
 *
 * @param LExec ローカルデータソースのExecutor型
 * @param RExec リモートデータソースのExecutor型
 * @param LDS LocalDataSourceContainerを実装したローカルデータソース型
 * @param RDS RemoteDataSourceContainerを実装したリモートデータソース型
 */
interface UseCaseContainer<LExec, RExec, LDS, RDS> :
    AlarmUseCase<LExec, LDS>,
    PlaylistUseCase<LExec, LDS>,
    VideoUseCase<LExec, RExec, LDS, RDS>,
    ImportUseCase<LExec, RExec, LDS, RDS>
    where LDS : LocalDataSourceContainer<LExec>,
          RDS : RemoteDataSourceContainer<RExec> {
    override val localDataSource: LDS
    override val remoteDataSource: RDS
    override val alarmScheduler: AlarmSchedulerPort
}