package net.turtton.ytalarm.kernel.repository

import arrow.core.Either
import net.turtton.ytalarm.kernel.dto.VideoInformation
import net.turtton.ytalarm.kernel.error.StreamError
import net.turtton.ytalarm.kernel.error.VideoInfoError

interface VideoInfoRepository<Executor> {
    suspend fun fetchVideoInfo(
        executor: Executor,
        url: String
    ): Either<VideoInfoError, VideoInformation>

    suspend fun fetchPlaylistInfo(
        executor: Executor,
        url: String
    ): Either<VideoInfoError, List<VideoInformation>>

    suspend fun getStreamUrl(
        executor: Executor,
        videoUrl: String,
        formatSelector: String
    ): Either<StreamError, String>
}