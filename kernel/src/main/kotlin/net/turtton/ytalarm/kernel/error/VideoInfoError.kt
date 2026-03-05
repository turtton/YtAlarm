package net.turtton.ytalarm.kernel.error

/**
 * 動画情報取得（fetchVideoInfo / fetchPlaylistInfo）のエラー型。
 */
sealed interface VideoInfoError {
    /**
     * ネットワーク接続エラーまたはyt-dlp実行エラー。
     */
    data class NetworkError(override val cause: Throwable) :
        VideoInfoError,
        NetworkFailure

    /**
     * レスポンスJSONのパースエラー。
     */
    data class ParseError(val cause: Throwable) : VideoInfoError

    /**
     * 対象URLがサポートされていない。
     */
    data class UnsupportedUrl(val url: String) : VideoInfoError

    /**
     * 動画が非公開・削除済みなどでアクセスできない。
     */
    data class Unavailable(val url: String, val reason: String? = null) : VideoInfoError
}