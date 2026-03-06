package net.turtton.ytalarm.di

import android.content.Context
import net.turtton.ytalarm.datasource.local.AppDatabase
import net.turtton.ytalarm.datasource.local.RoomDataSource
import net.turtton.ytalarm.datasource.remote.YtDlpDataSource
import net.turtton.ytalarm.datasource.remote.YtDlpExecutor
import net.turtton.ytalarm.datasource.repository.RoomAlarmRepository
import net.turtton.ytalarm.datasource.repository.RoomPlaylistRepository
import net.turtton.ytalarm.datasource.repository.RoomVideoRepository
import net.turtton.ytalarm.datasource.repository.YtDlpVideoInfoRepository
import net.turtton.ytalarm.platform.AndroidAlarmScheduler
import net.turtton.ytalarm.usecase.LocalDataSourceContainer
import net.turtton.ytalarm.usecase.RemoteDataSourceContainer
import net.turtton.ytalarm.usecase.UseCaseContainer

/**
 * UseCaseContainerを提供するインターフェース。
 * テスト時に差し替え可能にするため interface として定義する。
 */
interface DataContainerProvider {
    fun getUseCaseContainer(): UseCaseContainer<*, *, *, *>
}

/**
 * 本番用 [DataContainerProvider] 実装。
 * Room + yt-dlp を使った実際のデータソースコンテナを組み立てる。
 */
class DefaultDataContainerProvider(private val context: Context) : DataContainerProvider {

    private val localContainer = object : LocalDataSourceContainer<AppDatabase> {
        override val dataSource = RoomDataSource(context)
        override val alarmRepository = RoomAlarmRepository<AppDatabase> { db -> db.alarmDao() }
        override val videoRepository = RoomVideoRepository<AppDatabase> { db -> db.videoDao() }
        override val playlistRepository =
            RoomPlaylistRepository<AppDatabase> { db -> db.playlistDao() }
    }

    private val remoteContainer = object : RemoteDataSourceContainer<YtDlpExecutor> {
        override val dataSource = YtDlpDataSource()
        override val videoInfoRepository = YtDlpVideoInfoRepository()
    }

    private val alarmScheduler = AndroidAlarmScheduler(context)

    private val useCaseContainer by lazy {
        object : UseCaseContainer<
            AppDatabase,
            YtDlpExecutor,
            LocalDataSourceContainer<AppDatabase>,
            RemoteDataSourceContainer<YtDlpExecutor>
            > {
            override val localDataSource = localContainer
            override val remoteDataSource = remoteContainer
            override val alarmScheduler = this@DefaultDataContainerProvider.alarmScheduler
        }
    }

    override fun getUseCaseContainer() = useCaseContainer
}