package net.turtton.ytalarm.kernel.fake

import arrow.core.Either
import arrow.core.left
import net.turtton.ytalarm.kernel.dto.VideoInformation
import net.turtton.ytalarm.kernel.error.StreamError
import net.turtton.ytalarm.kernel.error.VideoInfoError
import net.turtton.ytalarm.kernel.repository.VideoInfoRepository

class FakeVideoInfoRepository : VideoInfoRepository<Unit> {
    var videoInfoResponses: MutableMap<String, Either<VideoInfoError, VideoInformation>> =
        mutableMapOf()
    var playlistInfoResponses:
        MutableMap<String, Either<VideoInfoError, List<VideoInformation>>> =
        mutableMapOf()
    var streamUrlResponses: MutableMap<String, Either<StreamError, String>> = mutableMapOf()

    override suspend fun fetchVideoInfo(
        executor: Unit,
        url: String
    ): Either<VideoInfoError, VideoInformation> = videoInfoResponses[url]
        ?: VideoInfoError.UnsupportedUrl(url).left()

    override suspend fun fetchPlaylistInfo(
        executor: Unit,
        url: String
    ): Either<VideoInfoError, List<VideoInformation>> = playlistInfoResponses[url]
        ?: VideoInfoError.UnsupportedUrl(url).left()

    override suspend fun getStreamUrl(
        executor: Unit,
        videoUrl: String,
        formatSelector: String
    ): Either<StreamError, String> = streamUrlResponses[videoUrl]
        ?: StreamError.FormatNotAvailable(videoUrl, formatSelector).left()
}