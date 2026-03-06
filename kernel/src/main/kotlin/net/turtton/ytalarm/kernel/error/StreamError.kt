package net.turtton.ytalarm.kernel.error

/**
 * ストリームURL取得（getStreamUrl）のエラー型。
 */
sealed interface StreamError {
    /**
     * ネットワーク接続エラーまたはyt-dlp実行エラー。
     */
    data class NetworkError(override val cause: Throwable) :
        StreamError,
        NetworkFailure

    /**
     * 指定されたフォーマットが存在しない。
     */
    data class FormatNotAvailable(val videoUrl: String, val formatSelector: String) : StreamError

    /**
     * 動画がDRM保護されているためストリームURLを取得できない。
     */
    data class DrmProtected(val videoUrl: String) : StreamError
}