package net.turtton.ytalarm.kernel.error

/**
 * 動画ファイルダウンロード（downloadVideo）のエラー型。
 */
sealed interface DownloadError {
    /**
     * ネットワーク接続エラーまたはyt-dlp実行エラー。
     */
    data class NetworkError(override val cause: Throwable) :
        DownloadError,
        NetworkFailure

    /**
     * 指定されたフォーマットが存在しない。
     */
    data class FormatNotAvailable(val videoUrl: String, val formatSelector: String) : DownloadError
}

/**
 * ダウンロード成功時の結果。
 */
data class DownloadResult(val filePath: String, val fileSize: Long)