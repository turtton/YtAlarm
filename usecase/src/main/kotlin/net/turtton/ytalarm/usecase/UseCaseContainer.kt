package net.turtton.ytalarm.usecase

import net.turtton.ytalarm.kernel.di.LocalDataSourceContainer
import net.turtton.ytalarm.kernel.di.RemoteDataSourceContainer
import net.turtton.ytalarm.kernel.port.AlarmSchedulerPort

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