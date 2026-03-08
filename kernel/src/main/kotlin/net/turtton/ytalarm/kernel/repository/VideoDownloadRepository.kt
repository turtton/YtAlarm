package net.turtton.ytalarm.kernel.repository

import arrow.core.Either
import net.turtton.ytalarm.kernel.error.DownloadError
import net.turtton.ytalarm.kernel.error.DownloadResult

interface VideoDownloadRepository<Executor> {
    suspend fun downloadVideo(
        executor: Executor,
        videoUrl: String,
        outputPath: String,
        formatSelector: String,
        onProgress: (Float) -> Unit = {}
    ): Either<DownloadError, DownloadResult>
}