package net.turtton.ytalarm.datasource.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.turtton.ytalarm.datasource.remote.YtDlpExecutor
import net.turtton.ytalarm.datasource.serializer.VideoInformationSerializer
import net.turtton.ytalarm.kernel.dto.VideoInformation
import net.turtton.ytalarm.kernel.error.StreamError
import net.turtton.ytalarm.kernel.error.VideoInfoError
import net.turtton.ytalarm.kernel.repository.VideoInfoRepository

/**
 * [VideoInfoRepository] の yt-dlp (youtubedl-android) 実装。
 *
 * yt-dlp の `--dump-single-json` オプションで動画/プレイリスト情報を取得し、
 * [VideoInformationSerializer] でデシリアライズする。
 */
class YtDlpVideoInfoRepository : VideoInfoRepository<YtDlpExecutor> {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchVideoInfo(
        executor: YtDlpExecutor,
        url: String
    ): Either<VideoInfoError, VideoInformation> = withContext(Dispatchers.IO) {
        executeAndParse(executor, url)
    }

    override suspend fun fetchPlaylistInfo(
        executor: YtDlpExecutor,
        url: String
    ): Either<VideoInfoError, List<VideoInformation>> = withContext(Dispatchers.IO) {
        executeAndParse(executor, url).map { info ->
            when (val typeData = info.typeData) {
                is VideoInformation.Type.Playlist -> typeData.entries
                is VideoInformation.Type.Video -> listOf(info)
            }
        }
    }

    override suspend fun getStreamUrl(
        executor: YtDlpExecutor,
        videoUrl: String,
        formatSelector: String
    ): Either<StreamError, String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = YoutubeDLRequest(videoUrl)
                .addOption("-f", formatSelector)
            executor.instance.getInfo(request)
        }.fold(
            onSuccess = { info ->
                val streamUrl = info.url
                if (streamUrl.isNullOrEmpty()) {
                    StreamError.FormatNotAvailable(
                        videoUrl = videoUrl,
                        formatSelector = formatSelector
                    ).left()
                } else {
                    streamUrl.right()
                }
            },
            onFailure = { error ->
                StreamError.NetworkError(cause = error).left()
            }
        )
    }

    private fun executeAndParse(
        executor: YtDlpExecutor,
        url: String
    ): Either<VideoInfoError, VideoInformation> {
        val executeResult: Either<VideoInfoError, YoutubeDLResponse> = runCatching {
            val request = YoutubeDLRequest(url)
                .addOption("--dump-single-json")
                .addOption("-f", "b")
            executor.instance.execute(request) { _, _, _ -> }
        }.fold(
            onSuccess = { it.right() },
            onFailure = { error -> VideoInfoError.NetworkError(cause = error).left() }
        )

        return when (executeResult) {
            is Either.Left -> executeResult

            is Either.Right -> {
                val response = executeResult.value
                runCatching {
                    json.decodeFromString(VideoInformationSerializer, response.out)
                }.fold(
                    onSuccess = { it.right() },
                    onFailure = { error -> VideoInfoError.ParseError(cause = error).left() }
                )
            }
        }
    }
}