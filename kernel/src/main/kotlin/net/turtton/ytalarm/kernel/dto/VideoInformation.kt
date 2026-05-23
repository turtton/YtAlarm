package net.turtton.ytalarm.kernel.dto

/**
 * yt-dlpから取得した動画/プレイリスト情報を表すDTO。
 * 純粋Kotlinで定義し、Android依存を持たない。
 */
data class VideoInformation(
    val id: String,
    val title: String? = null,
    /** yt-dlp の webpage_url 由来の動画ページ永続URL。ストリームURLではなく、保存・再解決に使う正規URL。 */
    val pageUrl: String,
    val domain: String,
    val typeData: Type
) {
    sealed interface Type {
        data class Video(
            val fullTitle: String,
            val thumbnailUrl: String,
            /**
             * yt-dlp の "url" フィールド由来のストリーミング直リンク。
             * 多くのサービス（SoundCloud等）で署名付き時限URLとなり期限切れする。
             * **永続的に保存してはならない**。再生時は親 [VideoInformation.pageUrl]
             * （webpage_url）を yt-dlp に渡して都度解決すること。
             * isStreamable 判定にのみ使用する。
             */
            val streamUrl: String
        ) : Type

        data class Playlist(val entries: List<VideoInformation>) : Type
    }
}