package net.turtton.ytalarm.kernel.fake

import arrow.core.Either
import arrow.core.left
import net.turtton.ytalarm.kernel.error.DownloadError
import net.turtton.ytalarm.kernel.error.DownloadResult
import net.turtton.ytalarm.kernel.repository.VideoDownloadRepository

class FakeVideoDownloadRepository : VideoDownloadRepository<Unit> {
    var downloadResponses: MutableMap<String, Either<DownloadError, DownloadResult>> =
        mutableMapOf()
    val downloadRequests: MutableList<DownloadRequest> = mutableListOf()

    data class DownloadRequest(
        val videoUrl: String,
        val outputPath: String,
        val formatSelector: String
    )

    override suspend fun downloadVideo(
        executor: Unit,
        videoUrl: String,
        outputPath: String,
        formatSelector: String,
        onProgress: (Float) -> Unit
    ): Either<DownloadError, DownloadResult> {
        downloadRequests.add(DownloadRequest(videoUrl, outputPath, formatSelector))
        onProgress(1.0f)
        return downloadResponses[videoUrl]
            ?: DownloadError.FormatNotAvailable(videoUrl, formatSelector).left()
    }
}