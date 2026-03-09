package net.turtton.ytalarm.datasource.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.datasource.remote.YtDlpExecutor
import net.turtton.ytalarm.kernel.error.DownloadError
import net.turtton.ytalarm.kernel.error.DownloadResult
import net.turtton.ytalarm.kernel.repository.VideoDownloadRepository
import java.io.File

/**
 * [VideoDownloadRepository] の yt-dlp (youtubedl-android) 実装。
 *
 * yt-dlp の execute() を使い、動画ファイルをローカルにダウンロードする。
 */
class YtDlpVideoDownloadRepository : VideoDownloadRepository<YtDlpExecutor> {
    override suspend fun downloadVideo(
        executor: YtDlpExecutor,
        videoUrl: String,
        outputPath: String,
        formatSelector: String,
        onProgress: (Float) -> Unit
    ): Either<DownloadError, DownloadResult> = withContext(Dispatchers.IO) {
        runCatching {
            val request = YoutubeDLRequest(videoUrl)
                .addOption("-f", formatSelector)
                .addOption("-o", outputPath)
                .addOption("--no-playlist")
            executor.instance.execute(request) { progress, _, _ ->
                onProgress(progress)
            }
        }.fold(
            onSuccess = {
                // outputPathはyt-dlpテンプレート（例: /path/1.%(ext)s）なので、
                // DL後に実ファイルを親ディレクトリから探す
                val templateFile = File(outputPath)
                val parentDir = templateFile.parentFile
                val baseName = templateFile.name.substringBefore(".%(ext)s")
                val actualFile = parentDir?.listFiles()
                    ?.firstOrNull { it.name.startsWith("$baseName.") && it.isFile }

                if (actualFile != null && actualFile.exists()) {
                    DownloadResult(
                        filePath = actualFile.absolutePath,
                        fileSize = actualFile.length()
                    ).right()
                } else {
                    DownloadError.FormatNotAvailable(
                        videoUrl = videoUrl,
                        formatSelector = formatSelector
                    ).left()
                }
            },
            onFailure = { error ->
                DownloadError.NetworkError(cause = error).left()
            }
        )
    }
}