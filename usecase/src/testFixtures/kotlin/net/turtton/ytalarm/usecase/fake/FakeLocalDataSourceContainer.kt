package net.turtton.ytalarm.usecase.fake

import net.turtton.ytalarm.kernel.di.DataSource
import net.turtton.ytalarm.kernel.fake.FakeAlarmRepository
import net.turtton.ytalarm.kernel.fake.FakePlaylistRepository
import net.turtton.ytalarm.kernel.fake.FakeVideoRepository
import net.turtton.ytalarm.kernel.repository.AlarmRepository
import net.turtton.ytalarm.kernel.repository.PlaylistRepository
import net.turtton.ytalarm.kernel.repository.VideoRepository
import net.turtton.ytalarm.usecase.LocalDataSourceContainer

class FakeLocalDataSourceContainer(
    override val alarmRepository: AlarmRepository<Unit> = FakeAlarmRepository(),
    override val videoRepository: VideoRepository<Unit> = FakeVideoRepository(),
    override val playlistRepository: PlaylistRepository<Unit> = FakePlaylistRepository(),
    override val dataSource: DataSource<Unit> = object : DataSource<Unit> {
        override fun createExecutor() = Unit
    }
) : LocalDataSourceContainer<Unit>