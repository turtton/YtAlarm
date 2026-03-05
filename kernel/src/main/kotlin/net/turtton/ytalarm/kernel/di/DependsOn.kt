package net.turtton.ytalarm.kernel.di

import net.turtton.ytalarm.kernel.repository.AlarmRepository
import net.turtton.ytalarm.kernel.repository.PlaylistRepository
import net.turtton.ytalarm.kernel.repository.VideoInfoRepository
import net.turtton.ytalarm.kernel.repository.VideoRepository

interface DependsOnDataSource<Executor> {
    val dataSource: DataSource<Executor>
}

interface DependsOnAlarmRepository<Executor> {
    val alarmRepository: AlarmRepository<Executor>
}

interface DependsOnVideoRepository<Executor> {
    val videoRepository: VideoRepository<Executor>
}

interface DependsOnPlaylistRepository<Executor> {
    val playlistRepository: PlaylistRepository<Executor>
}

interface DependsOnVideoInfoRepository<Executor> {
    val videoInfoRepository: VideoInfoRepository<Executor>
}