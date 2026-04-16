package net.turtton.ytalarm.usecase.fake

import net.turtton.ytalarm.kernel.di.DataSource
import net.turtton.ytalarm.kernel.fake.FakeVideoDownloadRepository
import net.turtton.ytalarm.kernel.fake.FakeVideoInfoRepository
import net.turtton.ytalarm.kernel.repository.VideoDownloadRepository
import net.turtton.ytalarm.kernel.repository.VideoInfoRepository
import net.turtton.ytalarm.usecase.RemoteDataSourceContainer

class FakeRemoteDataSourceContainer(
    override val videoInfoRepository: VideoInfoRepository<Unit> = FakeVideoInfoRepository(),
    override val videoDownloadRepository: VideoDownloadRepository<Unit> =
        FakeVideoDownloadRepository(),
    override val dataSource: DataSource<Unit> = object : DataSource<Unit> {
        override fun createExecutor() = Unit
    }
) : RemoteDataSourceContainer<Unit>