package net.turtton.ytalarm.usecase

import arrow.core.Either
import net.turtton.ytalarm.kernel.di.DependsOnDataSource
import net.turtton.ytalarm.kernel.di.DependsOnVideoDownloadRepository
import net.turtton.ytalarm.kernel.di.DependsOnVideoRepository
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.kernel.error.DownloadError
import net.turtton.ytalarm.kernel.error.DownloadResult
import net.turtton.ytalarm.kernel.port.FileStoragePort

/**
 * 動画ファイルダウンロードに関するビジネスロジックを定義するUseCaseインターフェース。
 *
 * @param LExec ローカルデータソースのExecutor型
 * @param RExec リモートデータソースのExecutor型
 * @param LDS VideoRepositoryに依存するローカルデータソース型
 * @param RDS VideoDownloadRepositoryに依存するリモートデータソース型
 */
interface DownloadUseCase<LExec, RExec, LDS, RDS>
    where LDS : DependsOnVideoRepository<LExec>,
          LDS : DependsOnDataSource<LExec>,
          RDS : DependsOnVideoDownloadRepository<RExec>,
          RDS : DependsOnDataSource<RExec> {
    val localDataSource: LDS
    val remoteDataSource: RDS
    val fileStorage: FileStoragePort

    companion object {
        const val DEFAULT_FORMAT_SELECTOR = "b[height<=720]/b[height>0]/ba[abr<=320]/ba/b"
    }

    /**
     * 動画をダウンロードしてローカルに保存する。
     * DB状態遷移: Information → Downloading → Downloaded / Failed
     *
     * @param videoId 対象動画のDB ID
     * @param onProgress ダウンロード進捗コールバック（0.0〜100.0）
     * @return ダウンロード結果。動画が見つからない場合やDL不要な場合はnull
     */
    suspend fun downloadVideo(
        videoId: Long,
        onProgress: (Float) -> Unit = {}
    ): Either<DownloadError, DownloadResult>? {
        val lExecutor = localDataSource.dataSource.createExecutor()
        val rExecutor = remoteDataSource.dataSource.createExecutor()

        val video = localDataSource.videoRepository.getFromIdSync(lExecutor, videoId)
            ?: return null

        val isStreamable = when (val state = video.state) {
            is Video.State.Information -> state.isStreamable
            is Video.State.Downloaded -> return null
            is Video.State.Downloading -> return null
            is Video.State.Importing -> return null
            is Video.State.Failed -> true
        }

        localDataSource.videoRepository.update(
            lExecutor,
            video.copy(state = Video.State.Downloading)
        )

        val downloadDir = fileStorage.getDownloadDir()
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        val outputPath = "${downloadDir.absolutePath}/${video.id}.%(ext)s"

        val result = remoteDataSource.videoDownloadRepository.downloadVideo(
            executor = rExecutor,
            videoUrl = video.videoUrl,
            outputPath = outputPath,
            formatSelector = DEFAULT_FORMAT_SELECTOR,
            onProgress = onProgress
        )

        when (result) {
            is Either.Right -> {
                val dlResult = result.value
                val relativePath = "downloads/${dlResult.filePath.substringAfterLast('/')}"
                localDataSource.videoRepository.update(
                    lExecutor,
                    video.copy(
                        state = Video.State.Downloaded(
                            internalLink = relativePath,
                            fileSize = dlResult.fileSize,
                            isStreamable = isStreamable
                        )
                    )
                )
            }

            is Either.Left -> {
                localDataSource.videoRepository.update(
                    lExecutor,
                    video.copy(state = Video.State.Information(isStreamable = isStreamable))
                )
            }
        }

        return result
    }

    /**
     * 追加のバイト数をダウンロードできるか上限チェックする。
     *
     * @param storageLimitBytes ストレージ上限（バイト）
     * @return ダウンロード可能ならtrue
     */
    fun canDownload(storageLimitBytes: Long): Boolean {
        val currentSize = fileStorage.getTotalDownloadSize()
        return currentSize < storageLimitBytes
    }

    /**
     * ダウンロード済みファイルを全て削除し、DB状態をInformationに戻す。
     *
     * @return 削除されたファイル数
     */
    suspend fun deleteAllDownloads(): Long {
        val lExecutor = localDataSource.dataSource.createExecutor()
        val allVideos = localDataSource.videoRepository.getExceptIdsSync(lExecutor, emptyList())
        val downloadedVideos = allVideos.filter { it.state is Video.State.Downloaded }

        for (video in downloadedVideos) {
            val state = video.state as Video.State.Downloaded
            localDataSource.videoRepository.update(
                lExecutor,
                video.copy(
                    state = Video.State.Information(isStreamable = state.isStreamable)
                )
            )
        }

        return fileStorage.deleteAllDownloads()
    }

    /**
     * ダウンロード済みファイルの合計サイズをバイト単位で返す。
     */
    fun getTotalDownloadSize(): Long = fileStorage.getTotalDownloadSize()
}